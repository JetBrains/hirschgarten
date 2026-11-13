package org.jetbrains.bazel.jvm.run

import com.intellij.execution.testframework.sm.ServiceMessageBuilder
import org.jetbrains.bazel.run.testResults.BazelTestLocationHintProvider
import org.jetbrains.bazel.run.testResults.IncompleteTestSuiteParser
import org.jetbrains.bazel.run.testResults.testFailed
import org.jetbrains.bazel.sync.workspace.languages.jvm.JAVA_TEST_KIND
import org.jetbrains.bazel.testing.BazelTestDetails
import java.util.regex.Pattern

/**
 * Parses the nice-looking test execution tree Junit5 produces
 */
internal class Junit5TestVisualOutputParser : IncompleteTestSuiteParser {
  override fun parseTestOutput(systemOut: String): List<ServiceMessageBuilder>? {
    if (!textContainsJunit5VisualOutput(systemOut)) return null
    return generateTestResultTree(systemOut).flatMap { it.report() }
  }

  /**
   * generateTestResultTree parse the junit5 test report and return the test result tree for each test target
   *
   * The tree for each target is organized as follows:
   * - The root is a TestResultTreeNode where the name is the bazel test target, and its result is null.
   *  - The sibling nodes of the root are the test result tree for each junit5 test classes in this test target. It can recursively
   * contain test result or a test suit result.
   *
   * @return List of generated TestResultTreeNode instances
   * */
  private fun generateTestResultTree(output: String): List<TestResultTreeNode> {
    val lines = output.lines()
    val testResultTrees = mutableListOf<TestResultTreeNode>()

    var treeRootForCurrentTarget: TestResultTreeNode? = null
    var currentNode: TestResultTreeNode? = null
    var currentStackTraceNode: TestResultTreeNode? = null

    for (line in lines) {
      val cleanLine = line.removeFormat()
      val testEndedMatcher = testingEndedPattern.matcher(line)
      val testLineMatcher = testLinePattern.matcher(cleanLine)
      val failuresCountMatcher = failuresCountPattern.matcher(cleanLine)
      if (treeRootForCurrentTarget == null) {
        val extractedTargetName = parseBuildTargetName(line)
        val testResultsAreStarting = line.contains(TEST_START_CHARACTER)
        val rootNodeName =
          when {
            extractedTargetName != null -> extractedTargetName
            testResultsAreStarting -> ""
            else -> continue
          }
        treeRootForCurrentTarget = TestResultTreeNode(rootNodeName, null, mutableListOf(), -1)
        testResultTrees.add(treeRootForCurrentTarget)
        currentNode = treeRootForCurrentTarget
      } else if (testEndedMatcher.find()) {
        val time = testEndedMatcher.group("time").toLongOrNull()
        treeRootForCurrentTarget.time = time
        currentNode = null
        treeRootForCurrentTarget = null
        currentStackTraceNode = null
      } else if (failuresCountMatcher.find()) {
        currentNode = null
      } else if (testLineMatcher.find()) {
        val indent = testLineMatcher.start("name")
        val parent = currentNode?.let { findParentByIndent(it, indent) }

        val (testName, testTime) = separateTimeFromTestName(testLineMatcher.group("name"))

        val newNode =
          TestResultTreeNode(
            name = testName,
            result = testLineMatcher.group("result").toTestResult(),
            messageLines = mutableListOf(testLineMatcher.group("message")),
            indent = indent,
            parent = parent,
            time = testTime,
          )
        parent?.children?.put(newNode.name, newNode)
        currentNode = newNode
      } else if (isStackTraceStartingLine(cleanLine)) {
        currentStackTraceNode = findNodeByPath(cleanLine, treeRootForCurrentTarget)
        currentNode = null
      } else if (currentNode != null && currentNode.messageLines.isNotEmpty()) {
        currentNode.messageLines.add(parseMultilineMessageContinuation(line, cleanLine))
      } else if (currentStackTraceNode != null) {
        currentStackTraceNode.stacktrace.add(cleanLine.substringAfter("    => "))
      }
    }
    return testResultTrees
  }

  private fun parseBuildTargetName(line: String): String? {
    val testingStartMatcher = testingStartPattern.matcher(line)
    return if (testingStartMatcher.find()) {
      testingStartMatcher.group("target")
    } else {
      null
    }
  }

  private fun findParentByIndent(previousNode: TestResultTreeNode, indent: Int) =
    when {
      previousNode.indent < indent -> previousNode
      previousNode.indent == indent -> previousNode.parent
      else -> {
        var realParent = previousNode
        while (realParent.parent != null && realParent.indent >= indent) {
          realParent = realParent.parent
        }
        realParent
      }
    }

  private fun separateTimeFromTestName(testName: String): Pair<String, Long?> {
    val testNameWithTimeMatcher = testNameWithTimePattern.matcher(testName)
    return if (testNameWithTimeMatcher.find()) {
      testNameWithTimeMatcher.group("name") to testNameWithTimeMatcher.group("time").toLongOrNull()
    } else {
      testName to null
    }
  }

  private fun isStackTraceStartingLine(line: String) =
    line.trim().let {
      it.startsWith("JUnit Jupiter:") || it.startsWith("JUnit Platform Suite:")
    }

  private fun parseMultilineMessageContinuation(rawLine: String, cleanLine: String): String {
    // We want to preserve leading whitespaces, where possible. We need to find the starting offset for that.
    // Some versions of JUnit 5 include the vertical bar "│" before multiline error messages, some don't.
    // For an example of a test that doesn't have the vertical bar, try to break any test using intellij_integration_test_suite.
    // We can also try to use the color information to find the message offset, but it may be different between operating systems.
    return cleanLine.substringAfterOrNull("│        ") ?: rawLine.substringAfterOrNull("?[0m?[31m   ")?.removeFormat()
      ?: cleanLine.trimStart()
  }

  private fun String.substringAfterOrNull(delimiter: String): String? {
    val index = indexOf(delimiter)
    return if (index == -1) null else substring(index + delimiter.length, length)
  }

  private fun findNodeByPath(path: String, root: TestResultTreeNode): TestResultTreeNode? {
    val segment = path.trim().split(":")
    var result = root
    for (i in 1 until segment.size) {
      result = result.children[segment[i]] ?: return null
    }
    return result
  }

  private fun String.toTestResult(): TestResult =
    when (this) {
      "✔" -> TestResult.PASSED
      "✘" -> TestResult.FAILED
      else -> TestResult.SKIPPED
    }

  companion object {
    fun textContainsJunit5VisualOutput(text: String): Boolean = text.contains(TEST_START_CHARACTER)
  }
}

private const val TEST_START_CHARACTER = '\u2577' // every junit5 visual output starts with that character ("╷")

private val testingStartPattern = Pattern.compile("^Executing\\htests\\hfrom\\h(?<target>[^:]*:[^:]+)")
private val testLinePattern =
  Pattern.compile("^(?:[\\h└├│]{3})+[└├│]─\\h(?<name>.+)\\h(?<result>[✔✘↷])\\h?(?<message>.*)$")
private val testNameWithTimePattern = Pattern.compile("^(?<name>.+)\\h(?<time>\\d+)\\hms$")
private val testingEndedPattern = Pattern.compile("^Test\\hrun\\hfinished\\hafter\\h(?<time>\\d+)\\hms")
private val failuresCountPattern = Pattern.compile("^Failures \\(\\d+\\):$")

private fun String.removeFormat(): String =
  this.replace(Regex("[?\\u001b]\\[[;\\d]*m"), "") // '1B' symbol appears in console output and test logs, while '?' appears in test XMLs

private enum class TestResult { PASSED, FAILED, SKIPPED }

private class TestResultTreeNode(
  val name: String,
  val result: TestResult?,
  val messageLines: MutableList<String>,
  val indent: Int,
  val parent: TestResultTreeNode? = null,
  var time: Long? = null,
) {
  val children: MutableMap<String, TestResultTreeNode> = mutableMapOf()
  val stacktrace: MutableList<String> = mutableListOf()

  fun isLeafNode() = children.isEmpty()

  fun isRootNode() = parent == null

  fun report(): List<ServiceMessageBuilder> {
    val parentSuiteNames = getAncestorSuiteNamesExcludingRoot()
    if (isRootNode()) {
      return children.values.flatMap { it.report() }
    }
    if (isLeafNode()) {
      val messages = reportTest(name, parentSuiteNames, result ?: return emptyList(), generateMessage())
      if (result == TestResult.FAILED && parent?.isRootNode() == true) {
        // BAZEL-2080: if an exception happens at the start of a test suit, there will be no test case run
        // and no test case reported. Teamcity will mark a testsuit with no test case as success.
        // So in this case, we need to report a dummy failed test case.
        val displayName = "no tests found"
        return messages + reportTest(displayName, parentSuiteNames, TestResult.FAILED, displayName)
      }
      return messages
    }
    val suiteHint = locationHint(BazelTestDetails(name, isSuite = true, parentSuiteNames = parentSuiteNames))
    val suiteStarted = ServiceMessageBuilder.testSuiteStarted(name)
    if (suiteHint != null) suiteStarted.addAttribute("locationHint", suiteHint)
    return listOf(suiteStarted) +
           children.values.flatMap { it.report() } +
           ServiceMessageBuilder.testSuiteFinished(name)
  }

  private fun reportTest(
    testName: String,
    parentSuiteNames: List<String>,
    testResult: TestResult,
    message: String,
  ): List<ServiceMessageBuilder> {
    val testHint = locationHint(BazelTestDetails(testName, parentSuiteNames = parentSuiteNames))
    val testStarted = ServiceMessageBuilder.testStarted(testName)
    if (testHint != null) testStarted.addAttribute("locationHint", testHint)
    val testFinished = ServiceMessageBuilder.testFinished(testName)
    time?.let { testFinished.addAttribute("duration", it.toString()) }
    return listOfNotNull(
      testStarted,
      when (testResult) {
        TestResult.PASSED -> null
        TestResult.FAILED -> testFailed(testName, message)
        TestResult.SKIPPED -> ServiceMessageBuilder.testIgnored(testName).addAttribute("message", message)
      },
      testFinished,
    )
  }

  private fun locationHint(testDetails: BazelTestDetails): String? =
    BazelTestLocationHintProvider.getLocationHint(testDetails, JAVA_TEST_KIND)

  private fun getAncestorSuiteNamesExcludingRoot(): List<String> =
    generateSequence(parent) { it.parent }
      .toList()
      .dropLast(1)
      .map { it.name }
      .reversed()

  private fun generateMessage(): String = messageLines.joinLinesIgnoringLastEmpty() + "\n" + stacktrace.joinLinesIgnoringLastEmpty()

  private fun List<String>.joinLinesIgnoringLastEmpty(): String {
    val linesToJoin =
      if (this.lastOrNull()?.isEmpty() == true) {
        this.dropLast(1)
      } else {
        this
      }
    return linesToJoin.joinToString("\n")
  }
}
