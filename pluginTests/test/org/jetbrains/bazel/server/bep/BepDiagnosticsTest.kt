package org.jetbrains.bazel.server.bep

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos
import com.google.devtools.build.v1.BuildEvent
import com.google.protobuf.Any
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.server.diagnostics.DiagnosticsService
import org.jetbrains.bazel.workspace.model.test.framework.MockProjectBaseTest
import org.jetbrains.bsp.protocol.BazelTaskEventsHandler
import org.jetbrains.bsp.protocol.CachedTestLog
import org.jetbrains.bsp.protocol.CoverageReport
import org.jetbrains.bsp.protocol.LogMessageParams
import org.jetbrains.bsp.protocol.PublishDiagnosticsParams
import org.jetbrains.bsp.protocol.TaskFinishParams
import org.jetbrains.bsp.protocol.TaskGroupId
import org.jetbrains.bsp.protocol.TaskStartParams
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Files
import kotlin.io.path.Path

class BepDiagnosticsTest : MockProjectBaseTest() {
  private class MockBuildTaskEventsHandler : BazelTaskEventsHandler {
    val buildPublishDiagnostics: MutableList<PublishDiagnosticsParams> = mutableListOf()

    override fun onBuildLogMessage(p0: LogMessageParams) {}

    override fun onBuildPublishDiagnostics(p0: PublishDiagnosticsParams) {
      buildPublishDiagnostics.add(p0)
    }

    override fun onBuildTaskStart(p0: TaskStartParams) {}

    override fun onBuildTaskFinish(p0: TaskFinishParams) {}

    override fun onPublishCoverageReport(report: CoverageReport) {}

    override fun onCachedTestLog(testLog: CachedTestLog) {}
  }

  fun newBepServer(taskHandler: BazelTaskEventsHandler): BepServer {
    val workspaceRoot = Path("workspaceRoot")
    val bazelInfo =
      BazelInfo(
        execRoot = Path("execRoot"),
        outputBase = Path("outputBase"),
        workspaceRoot = workspaceRoot,
        bazelBin = Path("bazel-bin"),
        release = BazelRelease(7),
        isBzlModEnabled = true,
        isWorkspaceEnabled = false,
        externalAutoloads = emptyList(),
      )
    return BepServer(
      taskEventsHandler = taskHandler,
      diagnosticsService = DiagnosticsService(workspaceRoot),
      parentId = TaskGroupId("originId").task(""),
      bazelPathsResolver = BazelPathsResolver(bazelInfo),
    )
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `compilation gives warnings`(success: Boolean) {
    val client = MockBuildTaskEventsHandler()
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
    val client = MockBuildTaskEventsHandler()
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

  @Test
  fun `BepEventHandler is called`() {
    val client = MockBuildTaskEventsHandler()

    var handlerCalledTimes = 0
    val bepEventHandler = object : BepEventHandler {
      override fun handleEvent(event: BuildEventStreamProtos.BuildEvent): Boolean {
        handlerCalledTimes++
        return true
      }
    }

    var providerCalledTimes = 0
    val testBepEventHandlerProvider = object : BepEventHandlerProvider {
      override fun create(context: BepEventHandlerContext): BepEventHandler {
        context.taskEventsHandler shouldBe client
        providerCalledTimes++
        return bepEventHandler
      }
    }
    BepEventHandlerProvider.EP_NAME.registerExtension(testBepEventHandlerProvider)

    val server = newBepServer(client)

    val buildStartedEvent = BuildEvent.newBuilder()
      .setBazelEvent(Any.pack(BuildEventStreamProtos.BuildEvent.newBuilder().build()))
      .build()

    server.handleEvent(buildStartedEvent)
    server.handleEvent(buildStartedEvent)

    handlerCalledTimes shouldBe 2
    providerCalledTimes shouldBe 1
  }
}
