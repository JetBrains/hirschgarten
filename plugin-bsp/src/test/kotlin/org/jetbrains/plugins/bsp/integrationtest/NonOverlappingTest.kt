package org.jetbrains.plugins.bsp.integrationtest

import ch.epfl.scala.bsp4j.BspConnectionDetails
import ch.epfl.scala.bsp4j.BuildClient
import ch.epfl.scala.bsp4j.BuildClientCapabilities
import ch.epfl.scala.bsp4j.DidChangeBuildTarget
import ch.epfl.scala.bsp4j.InitializeBuildParams
import ch.epfl.scala.bsp4j.LogMessageParams
import ch.epfl.scala.bsp4j.PublishDiagnosticsParams
import ch.epfl.scala.bsp4j.ShowMessageParams
import ch.epfl.scala.bsp4j.TaskFinishParams
import ch.epfl.scala.bsp4j.TaskProgressParams
import ch.epfl.scala.bsp4j.TaskStartParams
import com.google.gson.Gson
import io.kotest.matchers.shouldBe
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.jetbrains.magicmetamodel.impl.NonOverlappingTargets
import org.jetbrains.magicmetamodel.impl.OverlappingTargetsGraph
import org.jetbrains.magicmetamodel.impl.TargetsDetailsForDocumentProvider
import org.jetbrains.magicmetamodel.impl.workspacemodel.toBuildTargetInfo
import org.jetbrains.plugins.bsp.server.connection.BspServer
import org.jetbrains.plugins.bsp.server.tasks.calculateProjectDetailsWithCapabilities
import org.jetbrains.plugins.bsp.utils.withRealEnvs
import org.jetbrains.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.time.measureTimedValue

// TODO https://youtrack.jetbrains.com/issue/BAZEL-629
private const val BAZEL_REPOSITORY_TAG = "6.0.0"
private const val BAZEL_EXECUTABLE_VERSION = "5.4.0"
private const val BAZEL_BSP_VERSION = "3.1.0-20231004-838ae95-NIGHTLY"

class NonOverlappingTest : MockProjectBaseTest() {
  @Test
  fun `Compute non overlapping targets for bazelbuild_bazel project`() {
    val bazelDir = createTempDirectory("bazel-bsp-")
    cloneRepository(bazelDir, BAZEL_REPOSITORY_TAG)
    setBazelVersion(bazelDir, BAZEL_EXECUTABLE_VERSION)
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
      val projectDetails = calculateProjectDetailsWithCapabilities(
        server = server,
        buildServerCapabilities = initializationResult.capabilities,
        projectRootDir = bazelDir.toUri().toString(),
        errorCallback = { println(it) },)!!
      val targetsDetailsForDocumentProvider = TargetsDetailsForDocumentProvider(projectDetails.sources)
      val overlappingTargetsGraph = OverlappingTargetsGraph(targetsDetailsForDocumentProvider)
      val targets = projectDetails.targets.map { it.toBuildTargetInfo() }.toSet()
      val nonOverlapping = measureTimedValue { NonOverlappingTargets(targets, overlappingTargetsGraph) }
      nonOverlapping.value.size shouldBe 1684
      println("Computing non-overlapping targets took ${nonOverlapping.duration}")
    } finally {
      bspServerProcess.destroyForcibly()
    }
  }

  private fun setBazelVersion(bazelDir: Path, bazelVersion: String) {
    bazelDir.resolve(".bazelversion").writeText(bazelVersion)
  }

  private fun cloneRepository(bazelDir: Path, gitRevision: String) {
    ProcessBuilder(
      "git", "clone",
      "--branch", gitRevision,
      "--depth", "1",
      "https://github.com/bazelbuild/bazel",
      bazelDir.toAbsolutePath().toString(),
    )
      .inheritIO()
      .start()
      .run {
        waitFor(3, TimeUnit.MINUTES)
        if (exitValue() != 0) error("Could not clone")
      }
  }

  private fun installBsp(bazelDir: Path, target: String) {
    ProcessBuilder(
      "cs", "launch", "org.jetbrains.bsp:bazel-bsp:$BAZEL_BSP_VERSION",
      "-M", "org.jetbrains.bsp.bazel.install.Install",
      "--",
      "-t", target,
    ).run {
      inheritIO()
      directory(bazelDir.toFile())
      start()
    }.run {
      waitFor(3, TimeUnit.MINUTES)
      if (exitValue() != 0) error("Could not setup BSP")
    }
  }

  private fun initializeParams(bazelDir: Path) = InitializeBuildParams(
    "IntelliJ-BSP",
    "0.0.1",
    "2.0.0",
    bazelDir.toUri().toString(),
    BuildClientCapabilities(listOf("java")),
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
