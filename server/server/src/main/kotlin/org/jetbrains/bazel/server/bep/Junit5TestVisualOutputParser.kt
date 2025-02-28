package org.jetbrains.bazel.server.bep

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.logger.BspClientTestNotifier
import org.jetbrains.bsp.protocol.JUnitStyleTestCaseData
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.bsp.protocol.TestStatus
import java.util.UUID
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Parses the nice-looking test execution tree Junit5 produces
 */
class Junit5TestVisualOutputParser(private val bspClientTestNotifier: BspClientTestNotifier) {
  private val startedSuites: ArrayDeque<TestOutputLine> = ArrayDeque()

  fun processTestOutput(output: String) {
    var startedBuildTarget: StartedBuildTarget? = null
    var previousOutputLine: TestOutputLine? = null

    output.lines().forEach {
      if (startedBuildTarget == null) {
        startedBuildTarget = parseStartedBuildTarget(it)
      } else {
        val testingEndedMatcher = testingEndedPattern.matcher(it)
        val currentOutputLine = getCurrentOutputLine(it)
        if (currentOutputLine != null) {
          processPreviousOutputLine(previousOutputLine, currentOutputLine)
          previousOutputLine = currentOutputLine
        } else if (testingEndedMatcher.find()) {
          processTestingEndingLine(previousOutputLine, testingEndedMatcher, startedBuildTarget)
          startedBuildTarget = null
        }
      }
    }
  }

  private fun processTestingEndingLine(
    previousOutputLine: TestOutputLine?,
    testingEndedMatcher: Matcher,
    startedBuildTarget: StartedBuildTarget?,
  ) {
    previousOutputLine?.let { startAndFinishTest(it) }
    while (startedSuites.isNotEmpty()) {
      finishTopmostSuite()
    }
    val time = testingEndedMatcher.group("time").toLongOrNull() ?: 0
  }

  private fun processPreviousOutputLine(previousOutputLine: TestOutputLine?, currentOutputLine: TestOutputLine) {
    if (previousOutputLine != null) {
      if (currentOutputLine.indent > previousOutputLine.indent) {
        startSuite(previousOutputLine)
      } else {
        startAndFinishTest(previousOutputLine)
        removeAllFinishedSuites(currentOutputLine)
      }
    }
  }

  private fun removeAllFinishedSuites(currentOutputLine: TestOutputLine) {
    while (startedSuites.isNotEmpty() && startedSuites.last().indent >= currentOutputLine.indent) {
      finishTopmostSuite()
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
        taskId = TaskId(testUUID()),
      )
    } else {
      null
    }
  }

  private fun parseStartedBuildTarget(line: String): StartedBuildTarget? {
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
    bspClientTestNotifier.beginTestTarget(Label.parse(startedBuildTarget.uri), startedBuildTarget.taskId)
  }

  private fun startSuite(suite: TestOutputLine) {
    bspClientTestNotifier.startTest(suite.name, suite.taskId)
    startedSuites.addLast(suite)
  }

  private fun finishTopmostSuite() {
    with(startedSuites.removeLastOrNull() ?: return) {
      bspClientTestNotifier.finishTest(
        displayName = name,
        taskId = taskId,
        status = status,
        message = message,
      )
    }
  }

  private fun startAndFinishTest(test: TestOutputLine) {
    test.taskId.parents = generateParentList()
    bspClientTestNotifier.startTest(test.name, test.taskId)
    bspClientTestNotifier.finishTest(
      displayName = test.name,
      taskId = test.taskId,
      status = test.status,
      message = test.message,
      data = createTestCaseData(test.message),
    )
  }

  private fun generateParentList(): List<String> = startedSuites.toList().reversed().mapNotNull { it.taskId.id }

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
