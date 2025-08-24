package org.jetbrains.bazel.server.bep

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.logger.BspClientTestNotifier
import org.jetbrains.bsp.protocol.JUnitStyleTestCaseData
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.bsp.protocol.TestStatus
import java.util.UUID
import java.util.regex.Pattern

/**
 * Parses the nice-looking test execution tree Junit5 produces
 */
class Junit5TestVisualOutputParser(private val bspClientTestNotifier: BspClientTestNotifier) {
  fun processTestOutput(output: String) {
    val tree = generateTestResultTree(output)
    notifyClient(tree)
  }

  /**
   * generateTestResultTree parse the junit5 test report and return the test result tree for each test target
   *
   * The tree for each target is organized as follows:
   * - The root is a TestResultTreeNode where the name is the bazel test target, and its status is null.
   *  - The sibling nodes of the root are the test result tree for each junit5 test classes in this test target. It can recursively
   * contain test result or a test suit result.
   *
   * The sibling's taskId include its parent's taskId as parent. However, one exception is that direct sibling of root node does not have parents in taskId.
   * It is because that when we notify the client, the root node will be converted to beginTestTarget call, while other nodes are converted into startTest
   * call.
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
        treeRootForCurrentTarget = TestResultTreeNode(rootNodeName, TaskId(testUUID()), null, mutableListOf(), -1)
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
        val parentId = if (parent?.isRootNode() == true) null else parent?.taskId?.id
        val (testName, testTime) = separateTimeFromTestName(testLineMatcher.group("name"))

        val newNode =
          TestResultTreeNode(
            name = testName,
            taskId = TaskId(testUUID(), parents = listOfNotNull(parentId)),
            status = testLineMatcher.group("result").toTestStatus(),
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

  private fun notifyClient(trees: List<TestResultTreeNode>) {
    for (tree in trees) {
      tree.notifyClient(bspClientTestNotifier)
    }
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

  private fun testUUID(): String = "test-" + UUID.randomUUID().toString()

  private fun String.toTestStatus(): TestStatus =
    when (this) {
      "✔" -> TestStatus.PASSED
      "✘" -> TestStatus.FAILED
      else -> TestStatus.SKIPPED
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

private fun createTestCaseData(message: String, time: Long?): JUnitStyleTestCaseData =
  JUnitStyleTestCaseData(
    time = time?.let { it / 1000.0 },
    className = null,
    errorMessage = message,
    errorContent = null,
    errorType = null,
  )

private class TestResultTreeNode(
  val name: String,
  val taskId: TaskId,
  val status: TestStatus?,
  val messageLines: MutableList<String>,
  val indent: Int,
  val parent: TestResultTreeNode? = null,
  var time: Long? = null,
) {
  val children: MutableMap<String, TestResultTreeNode> = mutableMapOf()
  val stacktrace: MutableList<String> = mutableListOf()

  fun isLeafNode() = children.isEmpty()

  fun isRootNode() = parent == null

  fun notifyClient(bspClientTestNotifier: BspClientTestNotifier) {
    if (isRootNode()) {
      bspClientTestNotifier.beginTestTarget(Label.parse(name), taskId)
      children.forEach { it.value.notifyClient(bspClientTestNotifier) }
    } else if (isLeafNode()) {
      val fullMessage = generateMessage()
      bspClientTestNotifier.startTest(name, taskId)

      if (status == TestStatus.FAILED && parent?.isRootNode() == true && children.isEmpty()) {
        // BAZEL-2080: if an exception happens at the start of a test suit, there will be no test case run
        // and no test case reported. Teamcity will mark a testsuit with no test case as success.
        // So in this case, we need to report a dummy test case with TestStatus.FAILED status.
        val displayName = "no tests found"
        val placeholderID = TaskId("empty-test-" + UUID.randomUUID().toString(), parents = listOfNotNull(taskId.id))
        bspClientTestNotifier.startTest(displayName, placeholderID)
        bspClientTestNotifier.finishTest(
          displayName = displayName,
          taskId = placeholderID,
          status =
            TestStatus.FAILED,
          message = displayName,
          data = createTestCaseData(displayName, time),
        )
      }

      bspClientTestNotifier.finishTest(
        displayName = name,
        taskId = taskId,
        status =
          status
            ?: return,
        // this should never be null, because every node except root nodes should have status. Just for compiling
        message = fullMessage,
        data = createTestCaseData(fullMessage, time),
      )
    } else {
      bspClientTestNotifier.startTest(name, taskId)
      children.forEach { it.value.notifyClient(bspClientTestNotifier) }
      bspClientTestNotifier.finishTest(
        displayName = name,
        taskId = taskId,
        status =
          status
            ?: return,
        // this should never be null, because every node except root nodes should have status. Just for compiling
        message = messageLines.joinLinesIgnoringLastEmpty(),
      )
    }
  }

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
