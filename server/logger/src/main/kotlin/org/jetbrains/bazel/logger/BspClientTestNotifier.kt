package org.jetbrains.bazel.logger

import ch.epfl.scala.bsp4j.BuildClient
import ch.epfl.scala.bsp4j.StatusCode
import ch.epfl.scala.bsp4j.TaskFinishDataKind
import ch.epfl.scala.bsp4j.TaskFinishParams
import ch.epfl.scala.bsp4j.TaskId
import ch.epfl.scala.bsp4j.TaskStartDataKind
import ch.epfl.scala.bsp4j.TaskStartParams
import ch.epfl.scala.bsp4j.TestFinish
import ch.epfl.scala.bsp4j.TestReport
import ch.epfl.scala.bsp4j.TestStart
import ch.epfl.scala.bsp4j.TestStatus
import ch.epfl.scala.bsp4j.TestTask
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.toBspIdentifier
import org.jetbrains.bsp.protocol.JUnitStyleTestCaseData

class BspClientTestNotifier(private val bspClient: BuildClient, private val originId: String) {
  private var passedTests: Int = 0
  private var failedTests: Int = 0
  private var ignoredTests: Int = 0
  private var cancelledTests: Int = 0
  private var skippedTests: Int = 0

  /**
   * Notifies the client about starting a single test or a test suite
   * The presence or lack of parent's taskId indicates whether it's a test case or a test suite.
   *
   * @param displayName display name of the started test / test suite
   * @param taskId      TaskId of the started test - when parentsId is not empty / test suite - otherwise
   */
  fun startTest(displayName: String?, taskId: TaskId) {
    val testStart = TestStart(displayName)
    val taskStartParams = TaskStartParams(taskId)
    taskStartParams.originId = originId
    taskStartParams.dataKind = TaskStartDataKind.TEST_START
    taskStartParams.data = testStart
    bspClient.onBuildTaskStart(taskStartParams)
  }

  /**
   * Notifies the client about finishing a single test or a test suite.
   * The presence or lack of parent's taskId indicates whether it's a test case or a test suite.
   *
   * @param displayName display name of the finished test / test suite
   * @param taskId      TaskId of the finished test / test suite
   * @param status      status of the performed test (does not matter for test suites)
   * @param message     additional message concerning the test execution
   */
  fun finishTest(
    displayName: String?,
    taskId: TaskId,
    status: TestStatus?,
    message: String?,
    dataKind: String? = null,
    data: Any? = null,
  ) {
    val testFinish = TestFinish(displayName, status)
    if (message != null) {
      testFinish.message = message
    }

    if (dataKind != null && data != null) {
      testFinish.dataKind = dataKind
      testFinish.data = data
    }

    // For leaf tests, update reported counters
    if (data is JUnitStyleTestCaseData) {
      when (status) {
        TestStatus.PASSED -> passedTests++
        TestStatus.FAILED -> failedTests++
        TestStatus.IGNORED -> ignoredTests++
        TestStatus.CANCELLED -> cancelledTests++
        TestStatus.SKIPPED -> skippedTests++
        null -> {}
      }
    }

    val taskFinishParams = TaskFinishParams(taskId, StatusCode.OK)
    taskFinishParams.originId = originId
    taskFinishParams.dataKind = TaskFinishDataKind.TEST_FINISH
    taskFinishParams.data = testFinish
    bspClient.onBuildTaskFinish(taskFinishParams)
  }

  /**
   * Notifies the client about beginning the testing procedure
   *
   * @param targetIdentifier identifier of the testing target being executed
   * @param taskId           TaskId of the testing target execution
   */
  fun beginTestTarget(targetIdentifier: Label?, taskId: TaskId) {
    val testingBegin = TestTask(targetIdentifier?.toBspIdentifier())
    val taskStartParams = TaskStartParams(taskId)
    taskStartParams.originId = originId
    taskStartParams.dataKind = TaskStartDataKind.TEST_TASK
    taskStartParams.data = testingBegin
    bspClient.onBuildTaskStart(taskStartParams)
  }

  /**
   * Notifies the client about ending the testing procedure
   *
   * @param targetIdentifier identifier of the testing target being finished
   * @param taskId     TaskId of the testing target execution
   */
  fun endTestTarget(
    targetIdentifier: Label?,
    taskId: TaskId,
    time: Long? = null,
  ) {
    val testReport =
      TestReport(
        targetIdentifier?.toBspIdentifier(),
        passedTests,
        failedTests,
        ignoredTests,
        cancelledTests,
        skippedTests,
      )
    time?.let { testReport.time = it }

    val taskFinishParams = TaskFinishParams(taskId, StatusCode.OK)
    taskFinishParams.originId = originId
    taskFinishParams.dataKind = TaskFinishDataKind.TEST_REPORT
    taskFinishParams.data = testReport
    bspClient.onBuildTaskFinish(taskFinishParams)
  }
}
