package org.jetbrains.bsp.bazel.server.bep

import ch.epfl.scala.bsp4j.DidChangeBuildTarget
import ch.epfl.scala.bsp4j.LogMessageParams
import ch.epfl.scala.bsp4j.PrintParams
import ch.epfl.scala.bsp4j.PublishDiagnosticsParams
import ch.epfl.scala.bsp4j.ShowMessageParams
import ch.epfl.scala.bsp4j.TaskFinishParams
import ch.epfl.scala.bsp4j.TaskProgressParams
import ch.epfl.scala.bsp4j.TaskStartParams
import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos
import org.jetbrains.bsp.bazel.bazelrunner.utils.BazelInfo
import org.jetbrains.bsp.bazel.bazelrunner.utils.BazelRelease
import org.jetbrains.bsp.bazel.commons.Constants
import org.jetbrains.bsp.bazel.server.diagnostics.DiagnosticsService
import org.jetbrains.bsp.bazel.server.model.Label
import org.jetbrains.bsp.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bsp.protocol.JoinedBuildClient
import org.jetbrains.bsp.protocol.PublishOutputParams
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Files
import kotlin.io.path.Path

class BepDiagnosticsTest {
  private class MockBuildClient : JoinedBuildClient {
    val buildPublishDiagnostics: MutableList<PublishDiagnosticsParams> = mutableListOf()

    override fun onBuildShowMessage(p0: ShowMessageParams?) {}

    override fun onBuildLogMessage(p0: LogMessageParams?) {}

    override fun onBuildPublishDiagnostics(p0: PublishDiagnosticsParams) {
      buildPublishDiagnostics.add(p0)
    }

    override fun onBuildTargetDidChange(p0: DidChangeBuildTarget?) {}

    override fun onBuildTaskStart(p0: TaskStartParams?) {}

    override fun onBuildTaskProgress(p0: TaskProgressParams?) {}

    override fun onBuildTaskFinish(p0: TaskFinishParams?) {}

    override fun onRunPrintStdout(p0: PrintParams?) {}

    override fun onRunPrintStderr(p0: PrintParams?) {}

    override fun onBuildPublishOutput(params: PublishOutputParams) {}
  }

  fun newBepServer(client: JoinedBuildClient): BepServer {
    val workspaceRoot = Path("workspaceRoot")
    val bazelInfo =
      BazelInfo(
        execRoot = "execRoot",
        outputBase = Path("outputBase"),
        workspaceRoot = workspaceRoot,
        release = BazelRelease(7),
        isBzlModEnabled = true,
        isWorkspaceEnabled = false,
      )
    return BepServer(
      bspClient = client,
      diagnosticsService = DiagnosticsService(workspaceRoot),
      originId = "originId",
      target = Label.parse("//target"),
      bazelPathsResolver = BazelPathsResolver(bazelInfo),
    )
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `compilation gives warnings`(success: Boolean) {
    val client = MockBuildClient()
    val server = newBepServer(client)

    val stderrContents = """
src/build/NotCompiling.java:4: error: cannot find symbol
    public static ThisTypeDoesntExist getMessage() {
                  ^
  symbol:   class ThisTypeDoesntExist
  location: class NotCompiling
"""
    val path = Files.createTempFile("stderr", null)
    Files.write(path, stderrContents.toByteArray())

    val buildStartedEvent =
      BuildEventStreamProtos.BuildEvent
        .newBuilder()
        .apply {
          started =
            startedBuilder
              .apply {
                uuid = "uuid"
                command = Constants.BAZEL_BUILD_COMMAND
              }.build()
        }.build()

    server.handleBuildEventStreamProtosEvent(buildStartedEvent)

    val actionEvent =
      BuildEventStreamProtos.BuildEvent
        .newBuilder()
        .apply {
          action =
            actionBuilder
              .apply {
                this.success = success
                stderr =
                  BuildEventStreamProtos.File
                    .newBuilder()
                    .apply {
                      uri = path.toUri().toString()
                    }.build()
              }.build()
        }.build()

    server.handleBuildEventStreamProtosEvent(actionEvent)

    assertEquals(1, client.buildPublishDiagnostics.size)
  }

  @Test
  fun `unsuccessful compilation gives errors`() {
    val client = MockBuildClient()
    val server = newBepServer(client)

    val stderrContents = """
src/build/NotCompiling.java:4: error: cannot find symbol
    public static ThisTypeDoesntExist getMessage() {
                  ^
  symbol:   class ThisTypeDoesntExist
  location: class NotCompiling
"""
    val path = Files.createTempFile("stderr", null)
    Files.write(path, stderrContents.toByteArray())

    val buildStartedEvent =
      BuildEventStreamProtos.BuildEvent
        .newBuilder()
        .apply {
          started =
            startedBuilder
              .apply {
                uuid = "uuid"
                command = Constants.BAZEL_BUILD_COMMAND
              }.build()
        }.build()

    server.handleBuildEventStreamProtosEvent(buildStartedEvent)

    val actionEvent =
      BuildEventStreamProtos.BuildEvent
        .newBuilder()
        .apply {
          action =
            actionBuilder
              .apply {
                success = false
                stderr =
                  BuildEventStreamProtos.File
                    .newBuilder()
                    .apply {
                      uri = path.toUri().toString()
                    }.build()
              }.build()
        }.build()

    server.handleBuildEventStreamProtosEvent(actionEvent)

    assertEquals(1, client.buildPublishDiagnostics.size)
  }
}
