package org.jetbrains.bazel.server.bep

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos
import com.google.devtools.build.v1.BuildEvent
import com.google.protobuf.Any
import com.google.protobuf.ByteString
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import it.unimi.dsi.fastutil.chars.CharSet
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.diagnostics.DiagnosticsService
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.testproject.bazelInfo
import org.jetbrains.bazel.test.framework.testproject.testBazelProjectFixture
import org.jetbrains.bazel.test.framework.testproject.workspaceRoot
import org.jetbrains.bsp.protocol.BazelInvocationContext
import org.jetbrains.bsp.protocol.BazelTaskEventsHandler
import org.jetbrains.bsp.protocol.CachedTestLog
import org.jetbrains.bsp.protocol.CoverageReport
import org.jetbrains.bsp.protocol.DiagnosticSeverity
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
import kotlin.test.assertTrue

@BazelTestApplication
class BepDiagnosticsTest {
  private val testBazel by testBazelProjectFixture()

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

  fun newBepServer(taskHandler: BazelTaskEventsHandler, context : BazelInvocationContext = BazelInvocationContext.BUILD): BepServer =
    BepServer(
      project = testBazel.project,
      taskEventsHandler = taskHandler,
      diagnosticsService = DiagnosticsService(testBazel.workspaceRoot),
      parentId = TaskGroupId("originId").task(""),
      bazelPathsResolver = BazelPathsResolver(testBazel.bazelInfo),
      context = context,
    )

  fun newBuildStartedEvent() = BuildEventStreamProtos.BuildEvent
      .newBuilder()
      .apply {
        started =
          startedBuilder
            .apply {
              uuid = "uuid"
              command = Constants.BAZEL_BUILD_COMMAND
            }.build()
      }.build()

  val CONSIDERED_LABEL = "//the:action"

  fun newActionEvent(stderrContents: String, success: Boolean = false) = BuildEventStreamProtos.BuildEvent
      .newBuilder()
      .apply {
        id = BuildEventStreamProtos.BuildEventId.newBuilder().apply {
          actionCompleted = actionCompletedBuilder.apply {
            label = CONSIDERED_LABEL
          }.build()
        }.build()
        action =
          actionBuilder
            .apply {
              this.success = success
              stderr =
                BuildEventStreamProtos.File
                  .newBuilder()
                  .apply {
                    contents = ByteString.copyFrom(stderrContents, Charsets.UTF_8)
                  }.build()
            }.build()
      }.build()

  fun newAbortedConfiguredTarget(descriptionString: String, abortReason: BuildEventStreamProtos.Aborted.AbortReason = BuildEventStreamProtos.Aborted.AbortReason.ANALYSIS_FAILURE) =
    BuildEventStreamProtos.BuildEvent.newBuilder().apply {
    id = BuildEventStreamProtos.BuildEventId.newBuilder().apply {
     targetConfigured = targetConfiguredBuilder.apply {
       label = CONSIDERED_LABEL
     }.build()
    }.build()
    aborted = abortedBuilder.apply {
      reason = abortReason
      description = descriptionString
    }.build()
  }.build()

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

    val buildStartedEvent = newBuildStartedEvent()
    server.handleBuildEventStreamProtosEvent(buildStartedEvent)

    val actionEvent = newActionEvent(stderrContents, success)
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
    BepEventHandlerProvider.EP_NAME.point.registerExtension(testBepEventHandlerProvider, testBazel.disposable)

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

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `action errors are demoted on sync`(sync: Boolean) {
    val client = MockBuildTaskEventsHandler()
    val server = newBepServer(client, if (sync) BazelInvocationContext.SYNC else BazelInvocationContext.BUILD)
    val buildStartedEvent = newBuildStartedEvent()
    server.handleBuildEventStreamProtosEvent(buildStartedEvent)

    val stderrContents = """
src/build/NotCompiling.java:4: error: cannot find symbol
    public static ThisTypeDoesntExist getMessage() {
                  ^
  symbol:   class ThisTypeDoesntExist
  location: class NotCompiling
"""

    val actionEvent = newActionEvent(stderrContents)
    server.handleBuildEventStreamProtosEvent(actionEvent)
    assertEquals(1, client.buildPublishDiagnostics.size)
    val publishedDiagnostic = client.buildPublishDiagnostics[0]
    assertEquals(1, publishedDiagnostic.diagnostics.size)
    val diagnostics = publishedDiagnostic.diagnostics[0]
    if (sync) {
     assertEquals(DiagnosticSeverity.WARNING, diagnostics.level)
    } else {
      assertEquals(DiagnosticSeverity.ERROR, diagnostics.level)
    }
    assertEquals(Label.parse(CONSIDERED_LABEL), diagnostics.targetLabel)
    assertTrue(diagnostics.fileLocation?.endsWith("NotCompiling.java") ?: false)
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `unkonwn build failures are remorted`(sync: Boolean) {
    val client = MockBuildTaskEventsHandler()
    val server = newBepServer(client, if (sync) BazelInvocationContext.SYNC else BazelInvocationContext.BUILD)
    val buildStartedEvent = newBuildStartedEvent()
    server.handleBuildEventStreamProtosEvent(buildStartedEvent)
    val actionEvent = newActionEvent("/bin/bash: line 1: nonexistenttool: command not found")
    server.handleBuildEventStreamProtosEvent(actionEvent)
    assertEquals(1, client.buildPublishDiagnostics.size)
    val publishedDiagnostic = client.buildPublishDiagnostics[0]
    assertEquals(1, publishedDiagnostic.diagnostics.size)
    val diagnostics = publishedDiagnostic.diagnostics[0]
    if (sync) {
      assertEquals(DiagnosticSeverity.WARNING, diagnostics.level)
    } else {
      assertEquals(DiagnosticSeverity.ERROR, diagnostics.level)
    }
    assertEquals(Label.parse(CONSIDERED_LABEL), diagnostics.targetLabel)
    assertTrue(diagnostics.message.contains("nonexistenttool"))
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `silent build failures are remorted`(sync: Boolean) {
    val client = MockBuildTaskEventsHandler()
    val server = newBepServer(client, if (sync) BazelInvocationContext.SYNC else BazelInvocationContext.BUILD)
    val buildStartedEvent = newBuildStartedEvent()
    server.handleBuildEventStreamProtosEvent(buildStartedEvent)
    val actionEvent = newActionEvent("")
    server.handleBuildEventStreamProtosEvent(actionEvent)
    assertEquals(1, client.buildPublishDiagnostics.size)
    val publishedDiagnostic = client.buildPublishDiagnostics[0]
    assertEquals(1, publishedDiagnostic.diagnostics.size)
    val diagnostics = publishedDiagnostic.diagnostics[0]
    if (sync) {
      assertEquals(DiagnosticSeverity.WARNING, diagnostics.level)
    } else {
      assertEquals(DiagnosticSeverity.ERROR, diagnostics.level)
    }
    assertEquals(Label.parse(CONSIDERED_LABEL), diagnostics.targetLabel)
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `analysis errors are reported and not demoted`(sync: Boolean) {
    val client = MockBuildTaskEventsHandler()
    val server = newBepServer(client, if (sync) BazelInvocationContext.SYNC else BazelInvocationContext.BUILD)
    val buildStartedEvent = newBuildStartedEvent()
    server.handleBuildEventStreamProtosEvent(buildStartedEvent)
    val errorContent = """
        in java_binary rule //src:use: Visibility error:
        target '//src/lib:data' is not visible from
        target '//src:use'
        Recommendation: modify the visibility declaration if you think the dependency is legitimate. For more info see https://bazel.build/concepts/visibility
      """.trimIndent()
    val abortedEvent = newAbortedConfiguredTarget(errorContent)
    server.handleBuildEventStreamProtosEvent(abortedEvent)
    assertEquals(1, client.buildPublishDiagnostics.size)
    val publishedDiagnostic = client.buildPublishDiagnostics[0]
    assertEquals(1, publishedDiagnostic.diagnostics.size)
    val diagnostics = publishedDiagnostic.diagnostics[0]
    assertEquals(DiagnosticSeverity.ERROR, diagnostics.level) // Analysis errors are always reported as errors
    assertTrue(diagnostics.message.contains("analysis_failure", ignoreCase = true))
    assertTrue(diagnostics.message.contains("target '//src/lib:data' is not visible"))
  }


  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `loading errors are reported and not demoted`(sync: Boolean) {
    val client = MockBuildTaskEventsHandler()
    val server = newBepServer(client, if (sync) BazelInvocationContext.SYNC else BazelInvocationContext.BUILD)
    val buildStartedEvent = newBuildStartedEvent()
    server.handleBuildEventStreamProtosEvent(buildStartedEvent)

    val labelString = "//missing:package"
    val abortedEvent = BuildEventStreamProtos.BuildEvent.newBuilder().apply {
      id = BuildEventStreamProtos.BuildEventId.newBuilder().apply {
        pattern = patternBuilder.addPattern(labelString).build()
      }.build()
      aborted = abortedBuilder.apply {
        reason = BuildEventStreamProtos.Aborted.AbortReason.LOADING_FAILURE
        description = "no such package 'missing': BUILD file not found ..."
      }.build()
    }.build()
    server.handleBuildEventStreamProtosEvent(abortedEvent)
    assertEquals(1, client.buildPublishDiagnostics.size)
    val publishedDiagnostic = client.buildPublishDiagnostics[0]
    assertEquals(1, publishedDiagnostic.diagnostics.size)
    val diagnostics = publishedDiagnostic.diagnostics[0]
    assertEquals(DiagnosticSeverity.ERROR, diagnostics.level) // Analysis errors are always reported as errors
    assertTrue(diagnostics.message.contains("loading_failure",  ignoreCase = true))
    assertTrue(diagnostics.message.contains("BUILD file not found"))
  }
}
