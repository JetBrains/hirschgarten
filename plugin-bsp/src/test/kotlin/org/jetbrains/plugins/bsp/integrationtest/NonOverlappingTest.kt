package org.jetbrains.plugins.bsp.integrationtest

import ch.epfl.scala.bsp4j.*
import com.google.gson.Gson
import io.kotest.matchers.shouldBe
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.jetbrains.magicmetamodel.impl.NonOverlappingTargets
import org.jetbrains.magicmetamodel.impl.OverlappingTargetsGraph
import org.jetbrains.magicmetamodel.impl.TargetsDetailsForDocumentProvider
import org.jetbrains.plugins.bsp.server.connection.BspServer
import org.jetbrains.plugins.bsp.server.tasks.calculateProjectDetailsWithCapabilities
import org.jetbrains.plugins.bsp.utils.withRealEnvs
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

// TODO make sure these values are updated by dependabot or similar tool
private const val bazelRepositoryTag = "6.0.0"
private const val bazelExecutableVersion = "5.4.0"
private const val bazelBspVersion = "2.3.1"

@OptIn(ExperimentalTime::class)
class NonOverlappingTest {
  @Test
  fun `Compute non overlapping targets for bazelbuild_bazel project`() {
    val bazelDir =  createTempDirectory("bazel-bsp-")
    cloneRepository(bazelDir, bazelRepositoryTag)
    setBazelVersion(bazelDir, bazelExecutableVersion)
    installBsp(bazelDir, "//...")
    val connectionDetails = Gson().fromJson(bazelDir.resolve(".bsp/bazelbsp.json").readText(), BspConnectionDetails::class.java)
    val bspServerProcess = bspProcess(connectionDetails, bazelDir)
    try {
      val launcher = startBsp(bspServerProcess)
      val params = initializeParams(bazelDir)
      launcher.startListening()
      val server = launcher.remoteProxy
      val initializationResult = server.buildInitialize(params).get()
      server.onBuildInitialized()
      val projectDetails = calculateProjectDetailsWithCapabilities(server, initializationResult.capabilities) { println(it) }
      val targetsDetailsForDocumentProvider = TargetsDetailsForDocumentProvider(projectDetails.sources)
      val overlappingTargetsGraph = OverlappingTargetsGraph(targetsDetailsForDocumentProvider)
      val nonOverlapping = measureTimedValue { NonOverlappingTargets(projectDetails.targets, overlappingTargetsGraph) }
      nonOverlapping.value.size shouldBe 680
      println("Computing non-overlapping targets took ${nonOverlapping.duration}")
    } finally {
        bspServerProcess.destroyForcibly()
    }
  }

  private fun setBazelVersion(bazelDir: Path, bazelVersion: String) {
    bazelDir.resolve(".bazelversion").writeText(bazelVersion)
  }

  private fun cloneRepository(bazelDir: Path, gitRevision: String) {
    ProcessBuilder("git", "clone",
      "--branch", gitRevision,
      "--depth", "1",
      "https://github.com/bazelbuild/bazel",
      bazelDir.toAbsolutePath().toString())
      .inheritIO()
      .start().run {
        waitFor(3, TimeUnit.MINUTES)
        if (exitValue() != 0) throw RuntimeException("Could not clone")
      }
  }

  private fun installBsp(bazelDir: Path, target: String) {
    ProcessBuilder(
      "cs", "launch", "org.jetbrains.bsp:bazel-bsp:$bazelBspVersion",
      "-M", "org.jetbrains.bsp.bazel.install.Install",
      "--",
      "-t", target
    ).run {
      inheritIO()
      directory(bazelDir.toFile())
      start()
    }.run {
      waitFor(3, TimeUnit.MINUTES)
      if (exitValue() != 0) throw RuntimeException("Could not setup BSP")
    }
  }

  private fun initializeParams(bazelDir: Path) = InitializeBuildParams(
    "IntelliJ-BSP",
    "0.0.1",
    "2.0.0",
    bazelDir.toUri().toString(),
    BuildClientCapabilities(listOf("java"))
  )

  private fun startBsp(bspServerProcess: Process): Launcher<BspServer> {
    return Launcher.Builder<BspServer>()
      .setRemoteInterface(BspServer::class.java)
      .setExecutorService(Executors.newFixedThreadPool(4))
      .setInput(bspServerProcess.inputStream)
      .setOutput(bspServerProcess.outputStream)
      .setLocalService(DummyClient())
      .create()
  }

  private fun bspProcess(connectionDetails: BspConnectionDetails, bazelDir: Path): Process {
    return ProcessBuilder(connectionDetails.argv)
      .directory(bazelDir.toFile())
      .withRealEnvs()
      .start()
  }
}



class DummyClient : BuildClient {
  override fun onBuildShowMessage(params: ShowMessageParams?) {
    println(params)
  }

  override fun onBuildLogMessage(params: LogMessageParams?) {
    println(params)
  }

  override fun onBuildTaskStart(params: TaskStartParams?) {
    println(params)
  }

  override fun onBuildTaskProgress(params: TaskProgressParams?) {
    println(params)
  }

  override fun onBuildTaskFinish(params: TaskFinishParams?) {
    println(params)
  }

  override fun onBuildPublishDiagnostics(params: PublishDiagnosticsParams?) {
    println(params)
  }

  override fun onBuildTargetDidChange(params: DidChangeBuildTarget?) {
    println(params)
  }
}