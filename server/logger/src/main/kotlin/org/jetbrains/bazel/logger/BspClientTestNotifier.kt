package org.jetbrains.bazel.logger

import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.testing.BazelTestLocationHintProvider
import org.jetbrains.bsp.protocol.BazelTaskEventsHandler
import org.jetbrains.bsp.protocol.JUnitStyleTestCaseData
import org.jetbrains.bsp.protocol.TaskFinishParams
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.bsp.protocol.TaskStartParams
import org.jetbrains.bsp.protocol.TestFinish
import org.jetbrains.bsp.protocol.TestFinishData
import org.jetbrains.bsp.protocol.TestStart
import org.jetbrains.bsp.protocol.TestStatus
import org.jetbrains.bsp.protocol.TestTask

class BspClientTestNotifier(private val taskEventsHandler: BazelTaskEventsHandler) {
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
   * @param isSuite     whether the started test is a test suite or a test case
   * @param parentSuites list of ancestor suites' names, starting from the top level
   * @param classname `classname` value from test XML, if present
   */
  fun startTest(
    displayName: String,
    taskId: TaskId,
    isSuite: Boolean,
    parentSuites: List<String> = emptyList(),
    classname: String? = null,
  ) {
    val locationHint = BazelTestLocationHintProvider.testLocationHint(displayName, classname, parentSuites, isSuite = isSuite)
    val testStart = TestStart(displayName, isSuite, locationHint)
    val taskStartParams =
      TaskStartParams(
        taskId,
        data = testStart,
        message = "Test $displayName started",
      )
    taskEventsHandler.onBuildTaskStart(taskStartParams)
  }

  /**
   * Notifies the client about finishing a single test or a test suite.
   * The presence or lack of parent's taskId indicates whether it's a test case or a test suite.
   *
   * @param displayName display name of the finished test / test suite
   * @param taskId      TaskId of the finished test / test suite
   * @param status      status of the performed test (does not matter for test suites)
   * @param message     additional message concerning the test execution
   * @param data
   */
  fun finishTest(
    displayName: String,
    taskId: TaskId,
    status: TestStatus,
    message: String?,
    data: TestFinishData? = null,
  ) {
    val testFinish = TestFinish(displayName, message = message, status = status, data = data)

    // For leaf tests, update reported counters
    if (data is JUnitStyleTestCaseData) {
      when (status) {
        TestStatus.PASSED -> passedTests++
        TestStatus.FAILED -> failedTests++
        TestStatus.IGNORED -> ignoredTests++
        TestStatus.CANCELLED -> cancelledTests++
        TestStatus.SKIPPED -> skippedTests++
      }
    }

    val taskFinishParams =
      TaskFinishParams(
        taskId,
        status = BazelStatus.SUCCESS,
        data = testFinish,
        message = "Test $displayName finished",
      )
    taskEventsHandler.onBuildTaskFinish(taskFinishParams)
  }

  /**
   * Notifies the client about beginning the testing procedure
   *
   * @param targetIdentifier identifier of the testing target being executed
   * @param taskId           TaskId of the testing target execution
   */
  fun beginTestTarget(targetIdentifier: Label, taskId: TaskId) {
    val testingBegin = TestTask(targetIdentifier)
    val taskStartParams =
      TaskStartParams(
        taskId,
        data = testingBegin,
        message = "Test started for target $targetIdentifier",
      )
    taskEventsHandler.onBuildTaskStart(taskStartParams)
  }
}
