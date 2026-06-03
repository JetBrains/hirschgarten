package org.jetbrains.bazel.server.bep

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos
import com.google.devtools.build.v1.BuildEvent
import com.google.protobuf.Any
import io.kotest.matchers.nulls.shouldNotBeNull
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
import org.jetbrains.bsp.protocol.TestFinish
import org.jetbrains.bsp.protocol.TestStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Files
import kotlin.io.path.Path

class BepDiagnosticsTest : MockProjectBaseTest() {
  private class MockBuildTaskEventsHandler : BazelTaskEventsHandler {
    val buildPublishDiagnostics: MutableList<PublishDiagnosticsParams> = mutableListOf()
    val taskFinishCalls: MutableList<TaskFinishParams> = mutableListOf()

    override fun onBuildLogMessage(p0: LogMessageParams) {}

    override fun onBuildPublishDiagnostics(p0: PublishDiagnosticsParams) {
      buildPublishDiagnostics.add(p0)
    }

    override fun onBuildTaskStart(p0: TaskStartParams) {}

    override fun onBuildTaskFinish(p0: TaskFinishParams) {
      taskFinishCalls.add(p0)
    }

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
      project = project,
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

    val buildEventProto = BuildEventStreamProtos.BuildEvent.newBuilder().build()
    val buildEvent = BuildEvent.newBuilder()
      .setBazelEvent(Any.pack(buildEventProto))
      .build()

    server.handleEvent(buildEvent)
    server.handleEvent(buildEvent)

    handlerCalledTimes shouldBe 2
    providerCalledTimes shouldBe 1

    server.handleBuildEventStreamProtosEvent(buildEventProto)
    server.handleBuildEventStreamProtosEvent(buildEventProto)

    handlerCalledTimes shouldBe 4
    providerCalledTimes shouldBe 1
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `empty test xml reports a target-level node with the target label and status`(passed: Boolean) {
    val client = MockBuildTaskEventsHandler()
    val server = newBepServer(client)
    val label = "//src/go/demo:demo_test"

    val startedEvent =
      BuildEventStreamProtos.BuildEvent
        .newBuilder()
        .apply {
          idBuilder.testResultBuilder.label = label
          started =
            startedBuilder
              .apply {
                uuid = "uuid"
                command = Constants.BAZEL_TEST_COMMAND
              }.build()
        }.build()
    server.handleBuildEventStreamProtosEvent(startedEvent)

    // rules_go without verbose output writes an empty <testsuites> for a passing target.
    val testXml = Files.createTempFile("test", ".xml")
    Files.write(testXml, "<testsuites></testsuites>".toByteArray())

    val testResultEvent =
      BuildEventStreamProtos.BuildEvent
        .newBuilder()
        .apply {
          idBuilder.testResultBuilder.label = label
          testResult =
            testResultBuilder
              .apply {
                status = if (passed) BuildEventStreamProtos.TestStatus.PASSED else BuildEventStreamProtos.TestStatus.FAILED
                addTestActionOutput(
                  BuildEventStreamProtos.File
                    .newBuilder()
                    .apply {
                      name = "test.xml"
                      uri = testXml.toUri().toString()
                    }.build(),
                )
              }.build()
        }.build()
    server.handleBuildEventStreamProtosEvent(testResultEvent)

    val finish =
      client.taskFinishCalls
        .mapNotNull { it.data as? TestFinish }
        .find { it.displayName == label }
    finish.shouldNotBeNull()
    finish.status shouldBe if (passed) TestStatus.PASSED else TestStatus.FAILED
  }
}
