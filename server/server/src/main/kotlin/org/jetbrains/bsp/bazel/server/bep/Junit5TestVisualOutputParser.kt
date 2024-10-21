package org.jetbrains.bsp.bazel.server.bep

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.TaskId
import ch.epfl.scala.bsp4j.TestReport
import ch.epfl.scala.bsp4j.TestStatus
import org.jetbrains.bsp.bazel.logger.BspClientTestNotifier
import org.jetbrains.bsp.protocol.JUnitStyleTestCaseData
import java.util.Stack
import java.util.UUID
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Parses the nice-looking test execution tree Junit5 produces
 */
class Junit5TestVisualOutputParser(private val bspClientTestNotifier: BspClientTestNotifier) {
  fun processTestOutput(output: String) {
    val startedSuites: Stack<TestOutputLine> = Stack()
    var startedBuildTarget: StartedBuildTarget? = null
    var previousOutputLine: TestOutputLine? = null

    output.lines().forEach {
      if (startedBuildTarget == null) {
        startedBuildTarget = checkLineForTestingBeginning(it)
      } else {
        val testingEndedMatcher = testingEndedPattern.matcher(it)
        val currentOutputLine = getCurrentOutputLine(it)
        if (currentOutputLine != null) {
          processPreviousOutputLine(previousOutputLine, currentOutputLine, startedSuites)
          previousOutputLine = currentOutputLine
        } else if (testingEndedMatcher.find()) {
          processTestingEndingLine(startedSuites, previousOutputLine, testingEndedMatcher, startedBuildTarget)
          startedBuildTarget = null
        }
      }
    }
  }

  private fun processTestingEndingLine(
    startedSuites: Stack<TestOutputLine>,
    previousOutputLine: TestOutputLine?,
    testingEndedMatcher: Matcher,
    startedBuildTarget: StartedBuildTarget?,
  ) {
    startAndFinishTest(startedSuites, previousOutputLine!!)
    while (startedSuites.isNotEmpty()) {
      finishTopmostSuite(startedSuites)
    }
    val time = testingEndedMatcher.group("time").toLongOrNull() ?: 0
    endTesting(startedBuildTarget!!, time)
  }

  private fun processPreviousOutputLine(
    previousOutputLine: TestOutputLine?,
    currentOutputLine: TestOutputLine,
    startedSuites: Stack<TestOutputLine>,
  ) {
    if (previousOutputLine != null) {
      if (currentOutputLine.indent > previousOutputLine.indent) {
        startSuite(startedSuites, previousOutputLine)
      } else {
        startAndFinishTest(startedSuites, previousOutputLine)
        removeAllFinishedSuites(startedSuites, currentOutputLine)
      }
    }
  }

  private fun removeAllFinishedSuites(startedSuites: Stack<TestOutputLine>, currentOutputLine: TestOutputLine) {
    while (startedSuites.isNotEmpty() && startedSuites.peek().indent >= currentOutputLine.indent) {
      finishTopmostSuite(startedSuites)
    }
  }

  private fun getCurrentOutputLine(line: String): TestOutputLine? {
    val cleanLine = line.removeFormat()

    val currentLineMatcher = testLinePattern.matcher(cleanLine)
    return if (currentLineMatcher.find()) {
      TestOutputLine(
        name = currentLineMatcher.group("name"),
        status = currentLineMatcher.group("result").toTestStatus(),
        message = currentLineMatcher.group("message"),
        indent = currentLineMatcher.start("name"),
        taskId = TaskId(UUID.randomUUID().toString()),
      )
    } else {
      null
    }
  }

  private fun checkLineForTestingBeginning(line: String): StartedBuildTarget? {
    val testingStartMatcher = testingStartPattern.matcher(line)
    return if (testingStartMatcher.find()) {
      StartedBuildTarget(testingStartMatcher.group("target"), TaskId(testUUID())).also {
        beginTesting(it)
      }
    } else {
      null
    }
  }

  private fun beginTesting(startedBuildTarget: StartedBuildTarget) {
    bspClientTestNotifier.beginTestTarget(BuildTargetIdentifier(startedBuildTarget.uri), startedBuildTarget.taskId)
  }

  private fun endTesting(testTarget: StartedBuildTarget, millis: Long) {
    val report = TestReport(BuildTargetIdentifier(testTarget.uri), 0, 0, 0, 0, 0)
    report.time = millis
    bspClientTestNotifier.endTestTarget(report, testTarget.taskId)
  }

  private fun startSuite(startedSuites: Stack<TestOutputLine>, suite: TestOutputLine) {
    bspClientTestNotifier.startTest(suite.name, suite.taskId)
    startedSuites.push(suite)
  }

  private fun finishTopmostSuite(startedSuites: Stack<TestOutputLine>) {
    with(startedSuites.pop()) {
      bspClientTestNotifier.finishTest(
        displayName = name,
        taskId = taskId,
        status = status,
        message = message,
      )
    }
  }

  private fun startAndFinishTest(startedSuites: Stack<TestOutputLine>, test: TestOutputLine) {
    test.taskId.parents = generateParentList(startedSuites)
    bspClientTestNotifier.startTest(test.name, test.taskId)
    bspClientTestNotifier.finishTest(
      displayName = test.name,
      taskId = test.taskId,
      status = test.status,
      message = test.message,
      dataKind = JUnitStyleTestCaseData.DATA_KIND,
      data = createTestCaseData(test.message),
    )
  }

  private fun generateParentList(parents: Stack<TestOutputLine>): List<String> = parents.toList().reversed().mapNotNull { it.taskId.id }

  private fun testUUID(): String = "test-" + UUID.randomUUID().toString()

  private fun String.toTestStatus(): TestStatus =
    when (this) {
      "✔" -> TestStatus.PASSED
      "✘" -> TestStatus.FAILED
      else -> TestStatus.SKIPPED
    }

  companion object {
    fun textContainsJunit5VisualOutput(text: String): Boolean =
      text.contains(Char(0x2577)) // every junit5 visual output starts with that character ("╷")
  }
}

private val testingStartPattern = Pattern.compile("^Executing\\htests\\hfrom\\h(?<target>[^:]*:[^:]+)")
private val testLinePattern =
  Pattern.compile("^(?:[\\h└├│]{3})+[└├│]─\\h(?<name>.+)\\h(?<result>[✔✘↷])\\h?(?<message>.*)\$")
private val testingEndedPattern = Pattern.compile("^Test\\hrun\\hfinished\\hafter\\h(?<time>\\d+)\\hms")

private fun String.removeFormat(): String =
  this.replace(Regex("[?\\u001b]\\[[;\\d]*m"), "") // '1B' symbol appears in console output and test logs, while '?' appears in test XMLs

private fun createTestCaseData(message: String): JUnitStyleTestCaseData =
  JUnitStyleTestCaseData(
    time = null,
    className = null,
    errorMessage = message,
    errorContent = null,
    errorType = null,
  )

private data class TestOutputLine(
  val name: String,
  val status: TestStatus,
  val message: String,
  val indent: Int,
  val taskId: TaskId,
)

private data class StartedBuildTarget(val uri: String, val taskId: TaskId)
