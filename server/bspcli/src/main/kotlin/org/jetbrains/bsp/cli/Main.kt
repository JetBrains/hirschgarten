package org.jetbrains.bsp.cli

import ch.epfl.scala.bsp4j.BuildClientCapabilities
import ch.epfl.scala.bsp4j.DidChangeBuildTarget
import ch.epfl.scala.bsp4j.InitializeBuildParams
import ch.epfl.scala.bsp4j.LogMessageParams
import ch.epfl.scala.bsp4j.PrintParams
import ch.epfl.scala.bsp4j.PublishDiagnosticsParams
import ch.epfl.scala.bsp4j.ShowMessageParams
import ch.epfl.scala.bsp4j.TaskFinishParams
import ch.epfl.scala.bsp4j.TaskProgressParams
import ch.epfl.scala.bsp4j.TaskStartParams
import org.jetbrains.bsp.bazel.install.Install
import org.jetbrains.bsp.bazel.server.benchmark.shutdownTelemetry
import org.jetbrains.bsp.inmem.Connection
import org.jetbrains.bsp.protocol.JoinedBuildClient
import org.jetbrains.bsp.protocol.PublishOutputParams
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions
import kotlin.system.exitProcess

data class Args(
  val workspace: Path,
  val metricsFile: Path?,
  val target: String,
)

fun parseArgs(args: Array<String>): Args {
  if (args.size == 1) {
    return Args(workspace = Paths.get(args[0]), metricsFile = null, target = "//...")
  }
  if (args.size == 2) {
    return Args(workspace = Paths.get(args[0]), metricsFile = Paths.get(args[1]), target = "//...")
  }
  if (args.size == 3) {
    return Args(workspace = Paths.get(args[0]), metricsFile = Paths.get(args[1]), target = args[2])
  }

  println("Invalid number of arguments. Just pass path to your workspace as a CLI argument to this app")
  exitProcess(1)
}

/**
 * The application expects just a single argument - path to your bazel project
 */
fun main(args0: Array<String>) {
  val args = parseArgs(args0)
  val attrs =
    PosixFilePermissions.asFileAttribute(
      setOf(
        PosixFilePermission.OWNER_WRITE,
        PosixFilePermission.OWNER_READ,
        PosixFilePermission.OWNER_EXECUTE,
      ),
    )
  val installationDirectory = Files.createTempDirectory("bazelbsp-dir-", attrs)
  Install.main(
    arrayOf(
      "--bazel-workspace",
      args.workspace.toString(),
      "--directory",
      installationDirectory.toString(),
      "--targets",
      args.target,
    ),
  )

  val connection =
    Connection(
      installationDirectory,
      args.metricsFile,
      args.workspace,
      BuildClient(),
      propagateTelemetryContext = false,
    )

  val proxy = connection.clientLauncher.remoteProxy
  val buildInitializeResponse =
    proxy
      .buildInitialize(
        InitializeBuildParams(
          "IntelliJ-BSP",
          "0.0.1",
          "2.0.0",
          args.workspace.toUri().toString(),
          BuildClientCapabilities(listOf("java")),
        ),
      ).get()
  println(buildInitializeResponse)
  proxy.onBuildInitialized()
  proxy.workspaceBuildTargets().get().let { println(it) }

  connection.stop()
  shutdownTelemetry()
}

class BuildClient : JoinedBuildClient {
  override fun onBuildShowMessage(params: ShowMessageParams?) {}

  override fun onBuildLogMessage(params: LogMessageParams?) {}

  override fun onBuildTaskStart(params: TaskStartParams?) {}

  override fun onBuildTaskProgress(params: TaskProgressParams?) {}

  override fun onBuildTaskFinish(params: TaskFinishParams?) {}

  override fun onRunPrintStdout(p0: PrintParams?) {}

  override fun onRunPrintStderr(p0: PrintParams?) {}

  override fun onBuildPublishDiagnostics(params: PublishDiagnosticsParams?) {}

  override fun onBuildTargetDidChange(params: DidChangeBuildTarget?) {}

  override fun onBuildPublishOutput(params: PublishOutputParams) {}
}
