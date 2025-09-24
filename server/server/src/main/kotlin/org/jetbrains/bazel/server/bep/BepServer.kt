package org.jetbrains.bazel.server.bep

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos
import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.NamedSetOfFiles
import com.google.devtools.build.v1.BuildEvent
import com.google.devtools.build.v1.PublishBuildEventGrpc
import com.google.devtools.build.v1.PublishBuildToolEventStreamRequest
import com.google.devtools.build.v1.PublishBuildToolEventStreamResponse
import com.google.devtools.build.v1.PublishLifecycleEventRequest
import com.google.protobuf.Empty
import io.grpc.stub.StreamObserver
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.logger.BspClientLogger
import org.jetbrains.bazel.logger.BspClientTestNotifier
import org.jetbrains.bazel.server.diagnostics.DiagnosticsService
import org.jetbrains.bsp.protocol.CompileReport
import org.jetbrains.bsp.protocol.CompileTask
import org.jetbrains.bsp.protocol.CoverageReport
import org.jetbrains.bsp.protocol.JoinedBuildClient
import org.jetbrains.bsp.protocol.TaskFinishParams
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.bsp.protocol.TaskStartParams
import org.jetbrains.bsp.protocol.TestStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.nio.file.FileSystemNotFoundException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID
import kotlin.io.path.toPath

class BepServer(
  private val bspClient: JoinedBuildClient,
  private val diagnosticsService: DiagnosticsService,
  private val originId: String?,
  bazelPathsResolver: BazelPathsResolver,
  private val enableLegacyDiagnostics: Boolean = false,
) : PublishBuildEventGrpc.PublishBuildEventImplBase() {
  private val bspClientLogger = BspClientLogger(bspClient)
  private val bepLogger = BepLogger(bspClientLogger)

  private var startedEvent: TaskId? = null
  private var bspClientTestNotifier: BspClientTestNotifier? = null // Present for test commands
  private val bepOutputBuilder = BepOutputBuilder(bazelPathsResolver)

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
      if (originId == null || taskId == null || bspClientTestNotifier == null) {
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
          ?.uri
          ?.let { URI(it).toPath() }
      if (coverageReport != null) {
        bspClient.onPublishCoverageReport(
          CoverageReport(
            originId,
            coverageReport,
          ),
        )
      }

      val testXmlUri = testResult.testActionOutputList.find { it.name == "test.xml" }?.uri
      if (testXmlUri != null) {
        // Test cases identified and sent to the client by TestXmlParser.
        TestXmlParser(bspClientTestNotifier).parseAndReport(testXmlUri)
      } else {
        // Send a generic notification if individual tests cannot be processed.
        val childId = TaskId(UUID.randomUUID().toString(), parents = listOf(taskId.id))
        bspClientTestNotifier.startTest("Test", childId)
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
      // TODO https://youtrack.jetbrains.com/issue/BAZEL-622
      // bepLogger.onProgress(event.getProgress());
    }
  }

  private fun processBuildMetrics(event: BuildEventStreamProtos.BuildEvent) {
    if (event.hasBuildMetrics()) {
      bepLogger.onBuildMetrics(event.buildMetrics)
    }
  }

  private fun consumeBuildStartedEvent(event: BuildEventStreamProtos.BuildEvent) {
    bepOutputBuilder.clear()

    if (originId == null) {
      LOGGER.warn("No origin id was found. Event: {}", event)
      return
    }

    val taskId = TaskId(event.started.uuid)
    val startParams = TaskStartParams(taskId, eventTime = event.started.startTimeMillis, originId = originId)
    val target =
      event.id.testResult.label
        ?.let { Label.parse(it) }

    if (event.started.command == Constants.BAZEL_BUILD_COMMAND) { // todo: why only build?
      if (target != null) {
        val task = CompileTask(target)
        startParams.data = task
      }
      bspClient.onBuildTaskStart(startParams)
    } else if (event.started.command == Constants.BAZEL_TEST_COMMAND || event.started.command == Constants.BAZEL_COVERAGE_COMMAND) {
      if (target == null) {
        return
      }

      val bspClientTestNotifier = BspClientTestNotifier(bspClient, originId)
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
      LOGGER.warn("No start event id was found. Origin id: {}", originId)
      return
    }

    if (originId == null) {
      LOGGER.warn("No origin id was found. Task id: {}", taskId)
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
      TaskFinishParams(taskId, status = statusCode, eventTime = event.finished.finishTimeMillis, originId = originId, data = report)
    bspClient.onBuildTaskFinish(finishParams)
  }

  private fun processCompletedEvent(event: BuildEventStreamProtos.BuildEvent) {
    if (event.hasCompleted()) {
      consumeCompletedEvent(event)
    }
  }

  private fun processActionCompletedEvent(event: BuildEventStreamProtos.BuildEvent) {
    val originId: String = originId ?: return
    if (event.hasAction()) {
      consumeActionCompletedEvent(event.action, originId)
    }
  }

  private fun consumeActionCompletedEvent(event: BuildEventStreamProtos.ActionExecuted, originId: String) {
    val label = Label.parse(event.label)
    when (event.stderr.fileCase) {
      BuildEventStreamProtos.File.FileCase.URI -> {
        try {
          val path = Paths.get(URI.create(event.stderr.uri))
          val stdErrText = Files.readString(path)
          processDiagnosticText(stdErrText, label, originId)
        } catch (e: FileSystemNotFoundException) {
          LOGGER.warn("Failed to process diagnostic text", e)
        } catch (e: IOException) {
          LOGGER.warn("Failed to process diagnostic text", e)
        }
      }

      BuildEventStreamProtos.File.FileCase.CONTENTS -> {
        processDiagnosticText(event.stderr.contents.toStringUtf8(), label, originId)
      }

      else -> {}
    }
  }

  private fun processDiagnosticText(
    stdErrText: String,
    targetLabel: Label,
    originId: String,
  ) {
    // Legacy BSP diagnostics are opt-in and disabled by default to avoid double-reporting
    if (!enableLegacyDiagnostics) return

    if (stdErrText.isNotEmpty()) {
      val events = diagnosticsService.extractDiagnostics(stdErrText, targetLabel, originId)
      events.forEach { params ->
        bspClient.onBuildPublishDiagnostics(params)
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

  companion object {
    private val LOGGER: Logger = LoggerFactory.getLogger(BepServer::class.java)
  }
}
