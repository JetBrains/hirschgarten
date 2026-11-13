package org.jetbrains.bazel.run.testResults

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.intellij.execution.testframework.sm.ServiceMessageBuilder
import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.testing.BazelTestDetails
import org.jetbrains.bsp.protocol.BazelTaskEventsHandler
import org.jetbrains.bsp.protocol.TaskId
import java.nio.file.Path
import kotlin.io.path.readText

@ApiStatus.Internal
class TestXmlParser(private val taskEventsHandler: BazelTaskEventsHandler) {
  /**
   * Writes the test tree of a test result xml file to the test console of [taskId].
   * @param testXml Uri corresponding to the test result xml file to be processed.
   * @return whether any test was found and reported in the xml
   */
  fun parseAndReport(taskId: TaskId, testXml: Path, targetKind: TargetKind?): Boolean {
    val testSuites = parseTestXml(testXml, TestSuites::class.java)
    if (testSuites != null) {
      val testTreeRoot = mutableMapOf<String, TestTreeNode>()
      testSuites.testsuite.forEach { testTreeRoot.addSuite(it) }
      testTreeRoot.values.forEach { reportNode(taskId, it, targetKind) }
      return testTreeRoot.isNotEmpty()
    }
    val fallbackSuites = parseTestXml(testXml, FallbackTestXmlParser.IncompleteTestSuites::class.java)?.testsuite.orEmpty()
    fallbackSuites.forEach { processIncompleteInfoSuite(taskId, it, targetKind) }
    return fallbackSuites.isNotEmpty()
  }

  /**
   * Deserialize the given test report into the TestSuites/IncompleteTestSuites type
   */
  private fun <T> parseTestXml(testXml: Path, valueType: Class<T>): T? {
    val xmlMapper =
      XmlMapper().apply {
        registerKotlinModule()
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      }

    return runCatching {
      // Single empty tag does not deserialize properly, replace with empty pair.
      val rawContent = testXml.readText().replace("<skipped />", "<skipped></skipped>")
      xmlMapper.readValue(rawContent, valueType)
    }.getOrNull()
  }

  // Adds suite to the test tree. Names are split at separators, so suites and test cases can share parent nodes.
  // Suites are split with "/" and ".", test names are split only with "/" just to be extra cautious.
  // The split is best-effort, the tree does not always look like the original source file.
  // GoogleTest puts the instantiation before the fixture (https://google.github.io/googletest/advanced.html#how-to-write-value-parameterized-tests)
  // Catch2 can report nested sections in the wrong order (https://github.com/catchorg/Catch2/issues/417, https://github.com/catchorg/Catch2/pull/3219)
  private fun MutableMap<String, TestTreeNode>.addSuite(suite: TestSuite) {
    if (suite.testcase.isEmpty()) return
    val suitePath = splitTreePath(suite.name, '/', '.')
    val suiteNode = getOrCreateTreePath(suitePath)
    suiteNode.suites.add(suite)
    // A parameter that all test cases share is shown once on the suite
    val suiteParam = suite.testcase.map { it.param }.distinct().singleOrNull()
    suiteParam?.let { suiteNode.displayName = "${suiteNode.name} - $it" }
    // If the test name is duplicated in the suite name, drop it
    val testIsSuite = suite.testcase.all { it.name == suiteNode.name || it.name.startsWith("${suiteNode.name}/") }
    suite.testcase.forEach { testCase ->
      val testPath = splitTreePath(testCase.name, '/').let { if (testIsSuite) it.drop(1) else it }
      val node = getOrCreateTreePath(suitePath + testPath)
      node.testCases.add(TreeTestCase(suite.name, testCase))
      testCase.param?.takeIf { it != suiteParam }?.let { node.displayName = "${node.name} - $it" }
    }
  }

  private fun splitTreePath(treePath: String, vararg separators: Char): List<TreePathPart> {
    val parts = mutableListOf<TreePathPart>()
    var separator = ""
    var start = 0
    for (index in treePath.indices) {
      if (treePath[index] in separators) {
        parts.add(TreePathPart(separator, treePath.substring(start, index)))
        separator = treePath[index].toString()
        start = index + 1
      }
    }
    parts.add(TreePathPart(separator, treePath.substring(start)))
    return parts
  }

  private fun MutableMap<String, TestTreeNode>.getOrCreateTreePath(treePath: List<TreePathPart>): TestTreeNode {
    var children = this
    var node: TestTreeNode? = null
    for (part in treePath) {
      node = children.getOrPut(part.separator + part.name) {
        TestTreeNode(part.name, part.separator)
      }
      children = node.children
    }
    return checkNotNull(node)
  }

  private fun reportNode(taskId: TaskId, treeNode: TestTreeNode, targetKind: TargetKind?) {
    val node = collapse(treeNode)
    if (node.testCases.isNotEmpty() && node.children.isEmpty()) {
      node.testCases.forEach { reportTestCase(taskId, node.displayName, it, targetKind) }
      reportSuiteOutput(taskId, node)
      return
    }

    val details =
      node.testCases.firstOrNull()?.let { createTestDetails(it.suiteName, it.testCase) }
      ?: node.suites.firstOrNull()?.let { BazelTestDetails(it.name, isSuite = true) }
    val locationHint = details?.let { BazelTestLocationHintProvider.getLocationHint(it, targetKind) }
    val suiteStarted = ServiceMessageBuilder.testSuiteStarted(node.displayName)
    if (locationHint != null) suiteStarted.addAttribute("locationHint", locationHint)
    send(taskId, suiteStarted)
    // Only a leaf has the actions of a test, so each test case of the suite node is also a leaf in it
    node.testCases.forEach { reportTestCase(taskId, node.displayName, it, targetKind) }
    node.children.values.forEach { reportNode(taskId, it, targetKind) }
    reportSuiteOutput(taskId, node)
    val suiteFinished = ServiceMessageBuilder.testSuiteFinished(node.displayName)
    if (node.suites.isNotEmpty()) suiteFinished.addAttribute("duration", millis(node.suites.sumOf { it.time }))
    send(taskId, suiteFinished)
  }

  private fun reportSuiteOutput(taskId: TaskId, node: TestTreeNode) {
    node.suites.forEach { suite ->
      suite.systemOut?.let { taskEventsHandler.onTestConsoleOutput(taskId, it.toString(), isError = false) }
      suite.systemErr?.let { taskEventsHandler.onTestConsoleOutput(taskId, it.toString(), isError = true) }
    }
  }

  // Merges a node that has no suite or test case of its own into its only child, so `com.example` stays one node
  private fun collapse(node: TestTreeNode): TestTreeNode {
    val child = node.children.values.singleOrNull()
    if (child == null || node.suites.isNotEmpty() || node.testCases.isNotEmpty()) return node
    child.displayName = node.displayName + child.treePathSeparator + child.displayName
    return collapse(child)
  }

  private fun reportTestCase(
    taskId: TaskId,
    name: String,
    treeTestCase: TreeTestCase,
    targetKind: TargetKind?,
  ) {
    val testCase = treeTestCase.testCase
    val details = createTestDetails(treeTestCase.suiteName, testCase)
    val failure = testCase.error ?: testCase.failure
    val result = failure ?: testCase.skipped
    reportTest(
      taskId = taskId,
      name = name,
      locationHint = BazelTestLocationHintProvider.getLocationHint(details, targetKind),
      failure = failure,
      skipped = testCase.skipped,
      message = result?.message,
      output = if (result != null) result.content else testCase.systemOut,
      duration = millis(testCase.time),
    )
  }

  private fun processIncompleteInfoSuite(
    taskId: TaskId,
    suite: FallbackTestXmlParser.IncompleteTestSuite,
    targetKind: TargetKind?,
  ) {
    if (suite.systemOut != null) {
      for (parser in IncompleteTestSuiteParser.ep.extensionList) {
        val messages = parser.parseTestOutput(suite.systemOut) ?: continue
        messages.forEach { send(taskId, it) }
        return
      }
    }

    // A Bazel target is represented by a test suite containing one test case
    val suiteHint = BazelTestLocationHintProvider.getLocationHint(BazelTestDetails(suite.name, isSuite = true), targetKind)
    val suiteStarted = ServiceMessageBuilder.testSuiteStarted(suite.name)
    if (suiteHint != null) suiteStarted.addAttribute("locationHint", suiteHint)
    send(taskId, suiteStarted)
    val fallbackMessage = suite.systemOut.takeIf { suite.testcase.size == 1 }
    suite.testcase.forEach { testCase ->
      val details = FallbackTestXmlParser.createTestDetails(suite.name, testCase)
      val systemOut = testCase.systemOut ?: fallbackMessage
      val failure = testCase.error ?: testCase.failure
      val result = failure ?: testCase.skipped
      reportTest(
        taskId = taskId,
        name = details.displayName,
        locationHint = BazelTestLocationHintProvider.getLocationHint(details, targetKind),
        failure = failure,
        skipped = testCase.skipped,
        message = listOfNotNull(result?.message, systemOut).joinToString("\n"),
        output = if (result != null) result.content else systemOut,
        duration = testCase.time?.let(::millis),
      )
    }
    suite.systemOut?.let { taskEventsHandler.onTestConsoleOutput(taskId, it, isError = false) }
    send(taskId, ServiceMessageBuilder.testSuiteFinished(suite.name))
  }

  private fun reportTest(
    taskId: TaskId,
    name: String,
    locationHint: String?,
    failure: TestResultDetail?,
    skipped: TestResultDetail?,
    message: String?,
    output: String?,
    duration: String?,
  ) {
    val testStarted = ServiceMessageBuilder.testStarted(name)
    if (locationHint != null) testStarted.addAttribute("locationHint", locationHint)
    send(taskId, testStarted)
    if (failure != null) {
      val testFailed = testFailed(name, message)
      failure.type?.let { testFailed.addAttribute("type", it) }
      send(taskId, testFailed)
    }
    else if (skipped != null) {
      val testIgnored = ServiceMessageBuilder.testIgnored(name)
      if (message != null) testIgnored.addAttribute("message", message)
      send(taskId, testIgnored)
    }
    if (!output.isNullOrEmpty()) {
      val outputMessage = if (failure != null) ServiceMessageBuilder.testStdErr(name) else ServiceMessageBuilder.testStdOut(name)
      send(taskId, outputMessage.addAttribute("out", output))
    }
    val testFinished = ServiceMessageBuilder.testFinished(name)
    if (duration != null) testFinished.addAttribute("duration", duration)
    send(taskId, testFinished)
  }

  private fun send(taskId: TaskId, message: ServiceMessageBuilder) {
    taskEventsHandler.onTestConsoleOutput(taskId, message.toString(), isError = false)
  }

  private val TestCase.param: String? get() = valueParam ?: typeParam

  private data class TreePathPart(val separator: String, val name: String)

  private data class TreeTestCase(val suiteName: String, val testCase: TestCase)

  // Can be either a test suite (has children) or a test case
  private class TestTreeNode(val name: String, val treePathSeparator: String) {
    var displayName: String = name
    val children = mutableMapOf<String, TestTreeNode>()
    // Normally it's at most one suite or test case per node, but can be more if names clash after splitTreePath
    val suites = mutableListOf<TestSuite>()
    val testCases = mutableListOf<TreeTestCase>()
  }
}

@ApiStatus.Internal
interface IncompleteTestSuiteParser {
  /** The service messages of the test tree in [systemOut], or null if this parser does not know its format. */
  fun parseTestOutput(systemOut: String): List<ServiceMessageBuilder>?

  companion object {
    val ep: ExtensionPointName<IncompleteTestSuiteParser> = ExtensionPointName.create("org.jetbrains.bazel.incompleteTestSuiteParser")
  }
}

@JacksonXmlRootElement(localName = "testsuites")
internal data class TestSuites(
  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "testsuite")
  val testsuite: List<TestSuite> = emptyList(),
)

internal data class TestSuite(
  // Individual test cases are grouped within a suite.
  @JacksonXmlProperty(isAttribute = true)
  val name: String,
  @JacksonXmlProperty(isAttribute = true)
  val timestamp: String? = null,
  @JacksonXmlProperty(isAttribute = true)
  val hostname: String? = null,
  @JacksonXmlProperty(isAttribute = true)
  val tests: Int,
  @JacksonXmlProperty(isAttribute = true)
  val failures: Int,
  @JacksonXmlProperty(isAttribute = true)
  val errors: Int,
  @JacksonXmlProperty(isAttribute = true)
  val time: Double,
  @JacksonXmlProperty(isAttribute = true)
  val id: Int? = null,
  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "testcase")
  val testcase: List<TestCase> = emptyList(),
  @JacksonXmlProperty(localName = "system-out")
  val systemOut: Any? = null,
  @JacksonXmlProperty(localName = "system-err")
  val systemErr: Any? = null,
  @JacksonXmlProperty(isAttribute = true, localName = "package")
  val pkg: String?,
  val properties: Any? = null,
)

internal data class TestCase(
  // Name of the test case, typically method name.
  @JacksonXmlProperty(isAttribute = true)
  val name: String,
  // Class name corresponding to the test case.
  @JacksonXmlProperty(isAttribute = true)
  val classname: String,
  // Time value included with the test case.
  @JacksonXmlProperty(isAttribute = true)
  val time: Double,
  @JacksonXmlProperty(isAttribute = true)
  val file: String?,
  @JacksonXmlProperty(isAttribute = true)
  val line: Int?,
  @JacksonXmlProperty(isAttribute = true, localName = "value_param")
  val valueParam: String? = null,
  @JacksonXmlProperty(isAttribute = true, localName = "type_param")
  val typeParam: String? = null,
  // One of the following will be included if test did not pass.
  @JacksonXmlProperty(localName = "error")
  val error: TestResultDetail? = null,
  @JacksonXmlProperty(localName = "failure")
  val failure: TestResultDetail? = null,
  @JacksonXmlProperty(localName = "skipped")
  val skipped: TestResultDetail? = null,
  @JacksonXmlProperty(localName = "system-out")
  val systemOut: String? = null,
)

// This is a regular class due to limitations deserializing plain xml tag contents.
// https://github.com/FasterXML/jackson-dataformat-xml/issues/615
internal class TestResultDetail {
  // Shortened error message, as provided by the test framework.
  @JacksonXmlProperty(isAttribute = true)
  var message: String? = null

  // Error type information.
  // This typically gives the class name of the error, but may be absent or used for a similar alternative value.
  @JacksonXmlProperty(isAttribute = true)
  var type: String? = null

  // Content between the tags, which typically includes the full error stack trace.
  @JacksonXmlText(value = true)
  @JacksonXmlProperty(localName = "")
  var content: String? = null
}

/** Bazel has a separate way of parsing JUnit4 and JUnit5 test results into a xml file, resulting in
 * incomplete data about the latter.
 * **/
private object FallbackTestXmlParser {
  @JacksonXmlRootElement(localName = "testsuites")
  data class IncompleteTestSuites(
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "testsuite")
    val testsuite: List<IncompleteTestSuite> = emptyList(),
  )

  data class IncompleteTestSuite(
    @JacksonXmlProperty(isAttribute = true)
    val name: String,
    @JacksonXmlProperty(isAttribute = true)
    val failures: Int,
    @JacksonXmlProperty(isAttribute = true)
    val errors: Int,
    @JacksonXmlProperty(localName = "system-out")
    val systemOut: String? = null,
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "testcase")
    val testcase: List<IncompleteTestCase> = emptyList(),
  )

  data class IncompleteTestCase(
    @JacksonXmlProperty(isAttribute = true)
    val name: String,
    @JacksonXmlProperty(localName = "error")
    val error: TestResultDetail? = null,
    @JacksonXmlProperty(localName = "failure")
    val failure: TestResultDetail? = null,
    @JacksonXmlProperty(localName = "skipped")
    val skipped: TestResultDetail? = null,
    @JacksonXmlProperty(localName = "time")
    val time: Double? = null,
    @JacksonXmlProperty(localName = "system-out")
    val systemOut: String? = null,
  )

  // In the generated xml, suite name and test case name are the same, but in the Test Console test names have to be unique
  fun createTestDetails(suiteName: String, testCase: IncompleteTestCase): BazelTestDetails =
    BazelTestDetails(testCase.name.substringAfterLast('/'), parentSuiteNames = listOf(suiteName))
}

private fun createTestDetails(suiteName: String, testCase: TestCase): BazelTestDetails =
  BazelTestDetails(
    displayName = testCase.name,
    classname = testCase.classname,
    file = testCase.file,
    oneBasedLine = testCase.line?.plus(1),
    parentSuiteNames = listOf(suiteName),
    valueParam = testCase.valueParam,
    typeParam = testCase.typeParam,
  )

/** The SM runner takes milliseconds, and test.xml gives seconds. */
private fun millis(seconds: Double): String = (seconds * 1000).toLong().toString()
