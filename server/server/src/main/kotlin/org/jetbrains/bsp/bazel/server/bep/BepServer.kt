package org.jetbrains.bsp.bazel.server.bep

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.CompileReport
import ch.epfl.scala.bsp4j.CompileTask
import ch.epfl.scala.bsp4j.TaskFinishDataKind
import ch.epfl.scala.bsp4j.TaskFinishParams
import ch.epfl.scala.bsp4j.TaskId
import ch.epfl.scala.bsp4j.TaskStartDataKind
import ch.epfl.scala.bsp4j.TaskStartParams
import ch.epfl.scala.bsp4j.TestStatus
import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos
import com.google.devtools.build.v1.BuildEvent
import com.google.devtools.build.v1.PublishBuildEventGrpc
import com.google.devtools.build.v1.PublishBuildToolEventStreamRequest
import com.google.devtools.build.v1.PublishBuildToolEventStreamResponse
import com.google.devtools.build.v1.PublishLifecycleEventRequest
import com.google.protobuf.Empty
import io.grpc.stub.StreamObserver
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.jetbrains.bsp.bazel.commons.BazelStatus
import org.jetbrains.bsp.bazel.commons.Constants
import org.jetbrains.bsp.bazel.logger.BspClientLogger
import org.jetbrains.bsp.bazel.logger.BspClientTestNotifier
import org.jetbrains.bsp.bazel.server.diagnostics.DiagnosticsService
import org.jetbrains.bsp.bazel.server.model.Label
import org.jetbrains.bsp.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bsp.protocol.JoinedBuildClient
import org.jetbrains.bsp.protocol.PublishOutputParams
import org.jetbrains.bsp.protocol.TestCoverageReport
import java.io.IOException
import java.net.URI
import java.nio.file.FileSystemNotFoundException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

class BepServer(
  private val bspClient: JoinedBuildClient,
  private val diagnosticsService: DiagnosticsService,
  private val originId: String?,
  private val target: BuildTargetIdentifier?,
  bazelPathsResolver: BazelPathsResolver,
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

      val coverageReportUri = testResult.testActionOutputList.find { it.name == "test.lcov" }?.uri
      if (coverageReportUri != null) {
        bspClient.onBuildPublishOutput(
          PublishOutputParams(originId, taskId, target, TestCoverageReport.DATA_KIND, TestCoverageReport(coverageReportUri)),
        )
      }

      val testXmlUri = testResult.testActionOutputList.find { it.name == "test.xml" }?.uri
      if (testXmlUri != null) {
        // Test cases identified and sent to the client by TestXmlParser.
        TestXmlParser(bspClientTestNotifier).parseAndReport(testXmlUri)
      } else {
        // Send a generic notification if individual tests cannot be processed.
        val childId = TaskId(UUID.randomUUID().toString())
        childId.parents = listOf(taskId.id)
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
      bepOutputBuilder.storeNamedSet(
        event.id.namedSet.id,
        event.namedSetOfFiles,
      )
    }
  }

  private fun processBuildStartedEvent(event: BuildEventStreamProtos.BuildEvent) {
    if (event.hasStarted()) {
      consumeBuildStartedEvent(event.started)
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

  private fun consumeBuildStartedEvent(buildStarted: BuildEventStreamProtos.BuildStarted) {
    bepOutputBuilder.clear()
    val taskId = TaskId(buildStarted.uuid)
    val startParams = TaskStartParams(taskId)
    startParams.eventTime = buildStarted.startTimeMillis

    if (buildStarted.command == Constants.BAZEL_BUILD_COMMAND) { // todo: why only build?
      if (target != null) {
        startParams.dataKind = TaskStartDataKind.COMPILE_TASK
        val task = CompileTask(target)
        startParams.data = task
      }
      bspClient.onBuildTaskStart(startParams)
    } else if (buildStarted.command == Constants.BAZEL_TEST_COMMAND) {
      if (originId == null) {
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
      consumeFinishedEvent(event.finished)
    }
  }

  private fun consumeFinishedEvent(buildFinished: BuildEventStreamProtos.BuildFinished) {
    val taskId = startedEvent

    if (taskId == null) {
      LOGGER.warn("No start event id was found. Origin id: {}", originId)
      return
    }

    val bspClientTestNotifier = this.bspClientTestNotifier
    if (bspClientTestNotifier != null) {
      bspClientTestNotifier.endTestTarget(target, taskId)
      this.bspClientTestNotifier = null
      return
    }

    val statusCode = BazelStatus.fromExitCode(buildFinished.exitCode.code).toBspStatusCode()
    val finishParams = TaskFinishParams(taskId, statusCode)
    finishParams.eventTime = buildFinished.finishTimeMillis

    if (target != null) {
      finishParams.dataKind = TaskFinishDataKind.COMPILE_REPORT
      val isSuccess = statusCode.value == 1
      val errors = if (isSuccess) 0 else 1
      val report = CompileReport(target, errors, 0)
      finishParams.data = report
    }
    bspClient.onBuildTaskFinish(finishParams)
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
          val path = Paths.get(URI.create(event.stderr.uri))
          val stdErrText = Files.readString(path)
          processDiagnosticText(stdErrText, label)
        } catch (e: FileSystemNotFoundException) {
          LOGGER.warn(e)
        } catch (e: IOException) {
          LOGGER.warn(e)
        }
      }

      BuildEventStreamProtos.File.FileCase.CONTENTS -> {
        processDiagnosticText(event.stderr.contents.toStringUtf8(), label)
      }

      else -> {}
    }
  }

  private fun processDiagnosticText(stdErrText: String, targetLabel: Label) {
    if (stdErrText.isNotEmpty()) {
      val events =
        diagnosticsService.extractDiagnostics(
          stdErrText,
          targetLabel,
          originId,
        )
      events.forEach {
        bspClient.onBuildPublishDiagnostics(
          it,
        )
      }
    }
  }

  private fun consumeCompletedEvent(event: BuildEventStreamProtos.BuildEvent) {
    val eventLabel = event.id.targetCompleted.label
    /* The events never contain @, which will be different than the actual target id. Here we work around that fact,
     * but since we also set up the BEP server to gather info about build targets within certain path (//... etc.), we can't
     * just use target.uri.
     * */
    val labelText = if (target != null && ("@$eventLabel" == target.uri || "@@$eventLabel" == target.uri)) target.uri else eventLabel
    val label = Label.parse(labelText)
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
    private val LOGGER: Logger = LogManager.getLogger(BepServer::class.java)
  }
}
