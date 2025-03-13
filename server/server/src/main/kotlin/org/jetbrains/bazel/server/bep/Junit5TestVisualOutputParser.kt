package org.jetbrains.bazel.server.bep

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.logger.BspClientTestNotifier
import org.jetbrains.bsp.protocol.JUnitStyleTestCaseData
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.bsp.protocol.TestStatus
import java.util.UUID
import java.util.regex.Pattern

private class TestResultTreeNode(
  val name: String,
  val taskId: TaskId,
  val status: TestStatus?,
  val message: String,
  val indent: Int,
  val parent: TestResultTreeNode? = null,
  var time: Long? = null,
) {
  val children: MutableMap<String, TestResultTreeNode> = mutableMapOf()
  val stacktrace: MutableList<String> = mutableListOf()

  fun isLeafNode() = children.isEmpty()

  fun notifyClient(bspClientTestNotifier: BspClientTestNotifier) {
    if (isLeafNode()) {
      val fullMessage = generateMessage()
      bspClientTestNotifier.startTest(name, taskId)
      bspClientTestNotifier.finishTest(
        displayName = name,
        taskId = taskId,
        status = status!!,
        message = fullMessage,
        data = createTestCaseData(fullMessage, time),
      )
    } else {
      bspClientTestNotifier.startTest(name, taskId)
      children.forEach { it.value.notifyClient(bspClientTestNotifier) }
      bspClientTestNotifier.finishTest(
        displayName = name,
        taskId = taskId,
        status = status!!,
        message = message,
      )
    }
  }

  private fun generateMessage(): String = message + "\n" + stacktrace.joinToString("\n")
}

/**
 * Parses the nice-looking test execution tree Junit5 produces
 */
class Junit5TestVisualOutputParser(private val bspClientTestNotifier: BspClientTestNotifier) {
  private val startedSuites: ArrayDeque<TestOutputLine> = ArrayDeque()

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
        while (realParent.indent >= indent) {
          realParent = realParent.parent!!
        }
        realParent
      }
    }

  private fun isStackTraceStartingLine(line: String) = line.trim().startsWith("JUnit Jupiter:")

  private fun findNodeByPath(path: String, root: TestResultTreeNode): TestResultTreeNode? {
    val segment = path.trim().split(":")
    var result = root
    for (i in 1 until segment.size) {
      result = result.children[segment[i]] ?: return null
    }
    return result
  }

  private fun generateTestResultTree(output: String): Map<String, TestResultTreeNode> {
    val lines = output.lines()
    val targetToTestResultTree = mutableMapOf<String, TestResultTreeNode>()
    var treeRootForCurrentTarget: TestResultTreeNode? = null
    var previousNode: TestResultTreeNode? = null
    var i = 0
    while (i < lines.size) {
      val line = lines[i]
      val cleanLine = line.removeFormat()
      val testEndedMatcher = testingEndedPattern.matcher(line)
      val testLineMatcher = testLinePattern.matcher(cleanLine)
      if (treeRootForCurrentTarget == null) {
        parseBuildTargetName(line)?.let {
          treeRootForCurrentTarget = TestResultTreeNode(it, TaskId(testUUID()), null, "", -1)
          targetToTestResultTree[it] = treeRootForCurrentTarget
          previousNode = treeRootForCurrentTarget
        }
      } else if (testEndedMatcher.find()) {
        val time = testEndedMatcher.group("time").toLongOrNull()
        treeRootForCurrentTarget.time = time
        previousNode = null
        treeRootForCurrentTarget = null
      } else if (testLineMatcher.find()) {
        val indent = testLineMatcher.start("name")
        val parent = findParentByIndent(previousNode!!, indent)
        val newNode =
          TestResultTreeNode(
            name = testLineMatcher.group("name"),
            taskId = TaskId(testUUID(), parents = listOf(parent!!.taskId.id) + parent.taskId.parents),
            status = testLineMatcher.group("result").toTestStatus(),
            message = testLineMatcher.group("message"),
            indent = indent,
            parent = parent,
          )
        parent.children.put(newNode.name, newNode)
        previousNode = newNode
      } else if (isStackTraceStartingLine(cleanLine)) {
        val testNode = findNodeByPath(cleanLine, treeRootForCurrentTarget)
        if (testNode != null) {
          while (i + 1 < lines.size && lines[i + 1].isNotEmpty() && !isStackTraceStartingLine(lines[i + 1].removeFormat())) {
            testNode.stacktrace.add(lines[++i].removeFormat())
          }
        }
      }
      i++
    }
    return targetToTestResultTree
  }

  private fun notifyClient(trees: Map<String, TestResultTreeNode>) {
    for ((target, tree) in trees.entries) {
      bspClientTestNotifier.beginTestTarget(Label.parse(target), tree.taskId)
      tree.children.forEach { it.value.notifyClient(bspClientTestNotifier) }
      bspClientTestNotifier.endTestTarget(Label.parse(tree.name), tree.taskId, time = tree.time)
    }
  }

  fun processTestOutput(output: String) {
    val tree = generateTestResultTree(output)
    notifyClient(tree)
  }

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

private fun createTestCaseData(message: String, time: Long?): JUnitStyleTestCaseData =
  JUnitStyleTestCaseData(
    time = time as Double?,
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
