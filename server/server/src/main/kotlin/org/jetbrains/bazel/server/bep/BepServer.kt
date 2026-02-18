package org.jetbrains.bazel.server.bep

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos
import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.NamedSetOfFiles
import com.google.devtools.build.v1.BuildEvent
import com.google.devtools.build.v1.PublishBuildEventGrpc
import com.google.devtools.build.v1.PublishBuildToolEventStreamRequest
import com.google.devtools.build.v1.PublishBuildToolEventStreamResponse
import com.google.devtools.build.v1.PublishLifecycleEventRequest
import com.google.protobuf.Empty
import com.intellij.platform.util.progress.RawProgressReporter
import io.grpc.stub.StreamObserver
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.label.AllRuleTargets
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.SyntheticLabel
import org.jetbrains.bazel.logger.BspClientTestNotifier
import org.jetbrains.bazel.server.diagnostics.DiagnosticsService
import org.jetbrains.bsp.protocol.BazelTaskEventsHandler
import org.jetbrains.bsp.protocol.CachedTestLog
import org.jetbrains.bsp.protocol.CompileReport
import org.jetbrains.bsp.protocol.CompileTask
import org.jetbrains.bsp.protocol.CoverageReport
import org.jetbrains.bsp.protocol.TaskFinishParams
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.bsp.protocol.TaskStartParams
import org.jetbrains.bsp.protocol.TestStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.FileSystemNotFoundException
import java.nio.file.Files
import java.util.UUID
import kotlin.random.Random

class BepServer(
  private val taskEventsHandler: BazelTaskEventsHandler,
  private val diagnosticsService: DiagnosticsService,
  private val parentId: TaskId,
  private val bazelPathsResolver: BazelPathsResolver,
  private val rawProgressReporter: RawProgressReporter? = null,
) : PublishBuildEventGrpc.PublishBuildEventImplBase() {

  private var startedEvent: TaskId? = null
  private var bspClientTestNotifier: BspClientTestNotifier? = null // Present for test commands
  private val bepOutputBuilder = BepOutputBuilder(bazelPathsResolver)
  private val buildProgressParser = BuildProgressParser()

  override fun publishLifecycleEvent(request: PublishLifecycleEventRequest, responseObserver: StreamObserver<Empty>) {
    responseObserver.onNext(Empty.getDefaultInstance())
    responseObserver.onCompleted()
  }

  override fun publishBuildToolEventStream(
    responseObserver: StreamObserver<PublishBuildToolEventStreamResponse>,
  ): StreamObserver<PublishBuildToolEventStreamRequest> = BepStreamObserver(this, responseObserver)

  fun handleEvent(buildEvent: BuildEvent) {
    try {
      val event = BuildEventStreamProtos.BuildEvent.parseFrom(buildEvent.bazelEvent.value)

      LOGGER.trace("Got event {}", event)

      handleBuildEventStreamProtosEvent(event)
    } catch (e: IOException) {
      LOGGER.error("Error deserializing BEP proto: {}", e.toString())
    }
  }

  fun handleBuildEventStreamProtosEvent(event: BuildEventStreamProtos.BuildEvent) {
    processBuildStartedEvent(event)
    processProgressEvent(event)
    processBuildMetrics(event)
    processFinishedEvent(event)
    processActionCompletedEvent(event)
    fetchNamedSet(event)
    processCompletedEvent(event)
    processAbortedEvent(event)
    processTestResult(event)
    processTestSummary(event)
  }

  private fun processTestResult(event: BuildEventStreamProtos.BuildEvent) {
    if (event.hasTestResult()) {
      val taskId = startedEvent
      val bspClientTestNotifier = this.bspClientTestNotifier
      if (taskId == null || bspClientTestNotifier == null) {
        return
      }

      val testResult = event.testResult

      // TODO: this is the place where we could parse the test result and produce individual test events
      // TODO: there's some other interesting data
      //  If testing is requested, a TestResult event is sent for each test attempt,
      //  shard, and run per test. This allows BEP consumers to identify precisely
      //  which test actions failed their tests and identify the test outputs
      //  (such as logs, test.xml files) for each test action.

      val testStatus =
        when (testResult.status) {
          BuildEventStreamProtos.TestStatus.NO_STATUS -> TestStatus.SKIPPED
          BuildEventStreamProtos.TestStatus.PASSED -> TestStatus.PASSED
          BuildEventStreamProtos.TestStatus.FLAKY -> TestStatus.FAILED
          BuildEventStreamProtos.TestStatus.TIMEOUT -> TestStatus.FAILED
          BuildEventStreamProtos.TestStatus.FAILED -> TestStatus.FAILED
          BuildEventStreamProtos.TestStatus.INCOMPLETE -> TestStatus.SKIPPED
          BuildEventStreamProtos.TestStatus.REMOTE_FAILURE -> TestStatus.IGNORED
          BuildEventStreamProtos.TestStatus.FAILED_TO_BUILD -> TestStatus.CANCELLED
          BuildEventStreamProtos.TestStatus.TOOL_HALTED_BEFORE_TESTING -> TestStatus.SKIPPED
          else -> TestStatus.FAILED
        }

      val coverageReport =
        testResult.testActionOutputList
          .find { it.name == "test.lcov" }
          ?.let { bazelPathsResolver.resolve(it) }
      if (coverageReport != null) {
        taskEventsHandler.onPublishCoverageReport(
          CoverageReport(
            taskId,
            coverageReport,
          ),
        )
      }

      if (testResult.cachedLocally) {
        val testLog =
          testResult.testActionOutputList
            .find { it.name == "test.log" }
            ?.let { bazelPathsResolver.resolve(it) }

        if (testLog != null) {
          taskEventsHandler.onCachedTestLog(
            CachedTestLog(
              taskId,
              testLog,
            ),
          )
        }
      }

      val testXml = testResult.testActionOutputList.find { it.name == "test.xml" }?.let { bazelPathsResolver.resolve(it) }
      if (testXml != null) {
        // Test cases identified and sent to the client by TestXmlParser.
        TestXmlParser(bspClientTestNotifier).parseAndReport(taskId, testXml)
      } else {
        // Send a generic notification if individual tests cannot be processed.
        val childId = taskId.subTask("test-" + Random.nextBytes(8).toHexString())
        bspClientTestNotifier.startTest("Test", childId, isSuite = true)
        bspClientTestNotifier.finishTest("Test", childId, testStatus, "Test finished")
      }
    }
  }

  private fun processTestSummary(event: BuildEventStreamProtos.BuildEvent) {
    if (event.hasTestSummary()) {
      // TODO: this is probably only relevant in remote scenarios
    }
  }

  private fun fetchNamedSet(event: BuildEventStreamProtos.BuildEvent) {
    if (event.id.hasNamedSet()) {
      val internedNamedSetOfFiles = event.namedSetOfFiles.intern()
      bepOutputBuilder.storeNamedSet(
        event.id.namedSet.id,
        internedNamedSetOfFiles,
      )
    }
  }

  /**
   *  Returns a copy of a [NamedSetOfFiles] with interned string references.
   *  String references in [NamedSetOfFiles] are interned to conserve memory.
   *
   *  BEP protos often contain many duplicate strings both within a single stream and across
   *  shards running in parallel, so string interner is used to share references.
   */

  private fun NamedSetOfFiles.intern(): NamedSetOfFiles =
    toBuilder()
      .clearFiles()
      .addAllFiles(
        filesList.map { file ->
          file
            .toBuilder()
            .setUri(file.uri.intern())
            .setName(file.name.intern())
            .clearPathPrefix()
            .addAllPathPrefix(
              file.pathPrefixList.map { it.intern() },
            ).build()
        },
      ).build()

  private fun processBuildStartedEvent(event: BuildEventStreamProtos.BuildEvent) {
    if (event.hasStarted()) {
      consumeBuildStartedEvent(event)
    }
  }

  private fun processProgressEvent(event: BuildEventStreamProtos.BuildEvent) {
    if (event.hasProgress()) {
      val progress = event.progress
      if (progress.stderr.isNotEmpty()) {
        val stderrLines = progress.stderr.lines().map { line ->
          line.replace(ansiEscapeCode, "")
        }

        val events =
          diagnosticsService.extractDiagnostics(
            stderrLines,
            SyntheticLabel(AllRuleTargets),
            parentId,
            isCommandLineFormattedOutput = true,
            onlyFromParsedOutput = true,
          )
        events.forEach {
          taskEventsHandler.onBuildPublishDiagnostics(
            it,
          )
        }

        buildProgressParser.parse(stderrLines)?.let { progress ->
          rawProgressReporter?.details(progress.details)
          rawProgressReporter?.fraction(progress.fraction)
        }
      }
    }
  }

  private fun processBuildMetrics(event: BuildEventStreamProtos.BuildEvent) {
    if (event.hasBuildMetrics()) {
      bepMetrics = event.buildMetrics
    }
  }

  private fun consumeBuildStartedEvent(event: BuildEventStreamProtos.BuildEvent) {
    bepOutputBuilder.clear()

    val taskId = parentId.subTask("started-" + event.started.uuid)
    val startParams = TaskStartParams(taskId, eventTime = event.started.startTimeMillis)
    val target =
      event.id.testResult.label
        ?.let { Label.parse(it) }

    if (event.started.command == Constants.BAZEL_BUILD_COMMAND) { // todo: why only build?
      if (target != null) {
        val task = CompileTask(target)
        startParams.data = task
      }
      taskEventsHandler.onBuildTaskStart(startParams)
    } else if (event.started.command == Constants.BAZEL_TEST_COMMAND || event.started.command == Constants.BAZEL_COVERAGE_COMMAND) {
      if (target == null) {
        return
      }

      val bspClientTestNotifier = BspClientTestNotifier(taskEventsHandler)
      this.bspClientTestNotifier = bspClientTestNotifier
      bspClientTestNotifier.beginTestTarget(target, taskId)
    }
    startedEvent = taskId
  }

  private fun processFinishedEvent(event: BuildEventStreamProtos.BuildEvent) {
    if (event.hasFinished()) {
      consumeFinishedEvent(event)
    }
  }

  private fun consumeFinishedEvent(event: BuildEventStreamProtos.BuildEvent) {
    val taskId = startedEvent
    val target =
      event.id.testResult.label
        ?.let { Label.parse(it) }

    if (target == null) {
      LOGGER.warn("No target label was found. Task id: {}", taskId)
      return
    }

    if (taskId == null) {
      LOGGER.warn("No start event id was found. Origin id: {}", parentId)
      return
    }

    if (bspClientTestNotifier != null) {
      bspClientTestNotifier = null
      return
    }

    val statusCode = BazelStatus.fromExitCode(event.finished.exitCode.code)
    val isSuccess = statusCode == BazelStatus.SUCCESS
    val errors = if (isSuccess) 0 else 1
    val report = CompileReport(target, errors, 0)
    val finishParams =
      TaskFinishParams(taskId, status = statusCode, eventTime = event.finished.finishTimeMillis, data = report)
    taskEventsHandler.onBuildTaskFinish(finishParams)
  }

  private fun processCompletedEvent(event: BuildEventStreamProtos.BuildEvent) {
    if (event.hasCompleted()) {
      consumeCompletedEvent(event)
    }
  }

  private fun processActionCompletedEvent(event: BuildEventStreamProtos.BuildEvent) {
    if (event.hasAction()) {
      consumeActionCompletedEvent(event.action)
    }
  }

  private fun consumeActionCompletedEvent(event: BuildEventStreamProtos.ActionExecuted) {
    val label = Label.parse(event.label)
    when (event.stderr.fileCase) {
      BuildEventStreamProtos.File.FileCase.URI -> {
        try {
          val path = bazelPathsResolver.resolve(event.stderr)
          val stdErrText = Files.readString(path)
          processDiagnosticText(stdErrText, label, parentId)
        } catch (e: FileSystemNotFoundException) {
          LOGGER.warn("Failed to process diagnostic text", e)
        } catch (e: IOException) {
          LOGGER.warn("Failed to process diagnostic text", e)
        }
      }

      BuildEventStreamProtos.File.FileCase.CONTENTS -> {
        processDiagnosticText(event.stderr.contents.toStringUtf8(), label, parentId)
      }

      else -> {}
    }
  }

  private fun processDiagnosticText(
    stdErrText: String,
    targetLabel: Label,
    taskId: TaskId,
  ) {
    if (stdErrText.isNotEmpty()) {
      val events =
        diagnosticsService.extractDiagnostics(
          stdErrText.lines(),
          targetLabel,
          taskId,
        )
      events.forEach {
        taskEventsHandler.onBuildPublishDiagnostics(
          it,
        )
      }
    }
  }

  private fun consumeCompletedEvent(event: BuildEventStreamProtos.BuildEvent) {
    val eventLabel = Label.parseOrNull(event.id.targetCompleted.label)
    val label =
      eventLabel ?: run {
        LOGGER.warn("No target label found in event {}", event)
        return
      }
    val targetComplete = event.completed
    val outputGroups = targetComplete.outputGroupList
    LOGGER.trace("Consuming target completed event {}", targetComplete)
    bepOutputBuilder.storeTargetOutputGroups(label, outputGroups)
  }

  private fun processAbortedEvent(event: BuildEventStreamProtos.BuildEvent) {
    if (event.hasAborted()) {
      consumeAbortedEvent(event.aborted)
    }
  }

  private fun consumeAbortedEvent(aborted: BuildEventStreamProtos.Aborted) {
    if (aborted.reason != BuildEventStreamProtos.Aborted.AbortReason.NO_BUILD) {
      LOGGER.warn(
        "Command aborted with reason {}: {}",
        aborted.reason,
        aborted.description,
      )
    }
  }

  val bepOutput: BepOutput = bepOutputBuilder.build()
  var bepMetrics: BuildEventStreamProtos.BuildMetrics? = null
    private set

  companion object {
    private val ansiEscapeCode = "\\u001B\\[[\\d;]*[^\\d;]".toRegex()

    private val LOGGER: Logger = LoggerFactory.getLogger(BepServer::class.java)
  }
}
