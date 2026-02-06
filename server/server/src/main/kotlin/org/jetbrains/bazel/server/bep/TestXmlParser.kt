package org.jetbrains.bazel.server.bep

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.jetbrains.bazel.logger.BspClientTestNotifier
import org.jetbrains.bsp.protocol.JUnitStyleTestCaseData
import org.jetbrains.bsp.protocol.JUnitStyleTestSuiteData
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.bsp.protocol.TestStatus
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.readText

@JacksonXmlRootElement(localName = "testsuites")
data class TestSuites(
  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "testsuite")
  val testsuite: List<TestSuite> = emptyList(),
)

data class TestSuite(
  // Individual test cases are grouped within a suite.
  @JacksonXmlProperty(isAttribute = true)
  val name: String,
  @JacksonXmlProperty(isAttribute = true)
  val timestamp: String,
  @JacksonXmlProperty(isAttribute = true)
  val hostname: String,
  @JacksonXmlProperty(isAttribute = true)
  val tests: Int,
  @JacksonXmlProperty(isAttribute = true)
  val failures: Int,
  @JacksonXmlProperty(isAttribute = true)
  val errors: Int,
  @JacksonXmlProperty(isAttribute = true)
  val time: Double,
  @JacksonXmlProperty(isAttribute = true)
  val id: Int,
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

data class TestCase(
  // Name of the test case, typically method name.
  @JacksonXmlProperty(isAttribute = true)
  val name: String,
  // Class name corresponding to the test case.
  @JacksonXmlProperty(isAttribute = true)
  val classname: String,
  // Time value included with the test case.
  @JacksonXmlProperty(isAttribute = true)
  val time: Double,
  // One of the following will be included if test did not pass.
  @JacksonXmlProperty(localName = "error")
  val error: TestResultDetail? = null,
  @JacksonXmlProperty(localName = "failure")
  val failure: TestResultDetail? = null,
  @JacksonXmlProperty(localName = "skipped")
  val skipped: TestResultDetail? = null,
)

// This is a regular class due to limitations deserializing plain xml tag contents.
// https://github.com/FasterXML/jackson-dataformat-xml/issues/615
class TestResultDetail {
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

class TestXmlParser(private var bspClientTestNotifier: BspClientTestNotifier) {
  private val fallbackTestXmlParser = FallbackTestXmlParser(bspClientTestNotifier)

  /**
   * Processes a test result xml file, reporting suite and test case results as task start and finish notifications.
   * Parent-child relationship is identified within each suite based on the TaskId.
   * @param testXml Uri corresponding to the test result xml file to be processed.
   */
  fun parseAndReport(testXml: Path) {
    val testSuites = parseTestXml(testXml, TestSuites::class.java)
    if (testSuites != null) {
      testSuites
        .testsuite
        .forEach { processSuite(it) }
    } else {
      val fallbackTestSuites =
        parseTestXml(testXml, FallbackTestXmlParser.IncompleteTestSuites::class.java)
      fallbackTestSuites?.testsuite?.forEach {
        fallbackTestXmlParser.processIncompleteInfoSuite(it)
      }
    }
  }

  /**
   * Deserialize the given test report into the TestSuites/IncompleteTestSuites type as defined above.
   */
  private fun <T> parseTestXml(testXml: Path, valueType: Class<T>): T? {
    val xmlMapper =
      XmlMapper().apply {
        registerKotlinModule()
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      }

    var rawContent = testXml.readText()
    // Single empty tag does not deserialize properly, replace with empty pair.
    rawContent = rawContent.replace("<skipped />", "<skipped></skipped>")
    val testSuites: T? =
      runCatching {
        xmlMapper.readValue(rawContent, valueType)
      }.onFailure { }.getOrNull()
    return testSuites
  }

  /**
   * Convert each TestSuite into a series of taskStart and taskFinish notification to the client.
   * The `parents` field in each notification's TaskId will be used to indicate the parent-child relationship.
   *
   * Test suite is a collection of test cases (usually a test class).
   * If the number of tests inside is not zero, but no test case results are found, it means the test suite was filtered out by Bazel -
   * this happens when the user chooses to run only one test class, not the whole Bazel target.
   * In that case, there is no point in showing such an empty suite to the user.
   *
   * @param suite TestSuite to be processed.
   */
  private fun processSuite(suite: TestSuite) {
    val suiteStatus =
      when {
        suite.tests > 0 && suite.testcase.isEmpty() -> return
        suite.failures > 0 -> TestStatus.FAILED
        suite.errors > 0 -> TestStatus.FAILED
        else -> TestStatus.PASSED
      }

    val suiteTaskId = TaskId(UUID.randomUUID().toString(), parents = emptyList())
    val suiteData = JUnitStyleTestSuiteData(suite.time, null, suite.systemErr?.toString())

    bspClientTestNotifier.startTest(suite.name, suiteTaskId)
    suite.testcase.forEach { case ->
      processTestCase(suiteTaskId.id, suite.name, case)
    }
    bspClientTestNotifier.finishTest(
      suite.name,
      suiteTaskId,
      suiteStatus,
      suite.systemOut.toString(),
      suiteData,
    )
  }

  /**
   * Convert a TestCase into a taskStart and taskFinish notification to the client.
   * The test case will be associated with its parent suite.
   * @param parentId String identifying the parent's TaskId. Used to indicate the proper tree structure.
   * @param testCase TestCase to be processed and sent to the client.
   */
  private fun processTestCase(
    parentId: String,
    parentSuiteName: String,
    testCase: TestCase,
  ) {
    val testCaseTaskId = TaskId(UUID.randomUUID().toString(), listOf(parentId))

    // Extract the error summary message.
    val outcomeMessage =
      when {
        testCase.error != null -> testCase.error.message
        testCase.failure != null -> testCase.failure.message
        testCase.skipped != null -> testCase.skipped.message
        else -> null
      }

    // Extract the full error message content.
    val fullOutput =
      when {
        testCase.error != null -> testCase.error.content
        testCase.failure != null -> testCase.failure.content
        testCase.skipped != null -> testCase.skipped.content
        else -> ""
      }

    // Map the outcome into a TestStatus value.
    val testStatusOutcome =
      when {
        testCase.error != null -> TestStatus.FAILED
        testCase.failure != null -> TestStatus.FAILED
        testCase.skipped != null -> TestStatus.SKIPPED
        else -> TestStatus.PASSED
      }

    // Extract error type information if provided.
    val errorType =
      when {
        testCase.error != null -> testCase.error.type
        testCase.failure != null -> testCase.failure.type
        else -> null
      }
    val testCaseData =
      JUnitStyleTestCaseData(
        testCase.time,
        testCase.classname,
        outcomeMessage,
        fullOutput,
        errorType,
      )
    bspClientTestNotifier.startTest(
      displayName = testCase.name,
      taskId = testCaseTaskId,
      classname = testCaseData.className,
      parentSuites = listOf(parentSuiteName),
    )
    bspClientTestNotifier.finishTest(
      testCase.name,
      testCaseTaskId,
      testStatusOutcome,
      "",
      testCaseData,
    )
  }
}

/** Bazel has a separate way of parsing JUnit4 and JUnit5 test results into a xml file, resulting in
 * incomplete data about the latter.
 * **/
private class FallbackTestXmlParser(private var bspClientTestNotifier: BspClientTestNotifier) {
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

  fun processIncompleteInfoSuite(suite: IncompleteTestSuite) {
    val containsJunit5 = suite.systemOut?.let(Junit5TestVisualOutputParser::textContainsJunit5VisualOutput)
    if (containsJunit5 == true) {
      val parser = Junit5TestVisualOutputParser(bspClientTestNotifier)
      parser.processTestOutput(suite.systemOut)
    } else {
      defaultIncompleteInfoSuiteProcessing(suite)
    }
  }

  // A Bazel target is represented by a test suite containing one test case
  private fun defaultIncompleteInfoSuiteProcessing(suite: IncompleteTestSuite) {
    val suiteTaskId = TaskId(UUID.randomUUID().toString(), emptyList())
    val suiteStatus =
      when {
        suite.failures > 0 -> TestStatus.FAILED
        suite.errors > 0 -> TestStatus.FAILED
        else -> TestStatus.PASSED
      }
    val testSuiteData = JUnitStyleTestSuiteData(null, suite.systemOut, null)

    bspClientTestNotifier.startTest(suite.name, suiteTaskId)
    val fallbackMessage = suite.systemOut.takeIf { suite.testcase.size == 1 }
    suite.testcase.forEach { testCase ->
      processIncompleteInfoCase(testCase, suiteTaskId.id, suite.name, testCase.systemOut ?: fallbackMessage)
    }
    bspClientTestNotifier.finishTest(
      suite.name,
      suiteTaskId,
      suiteStatus,
      null,
      testSuiteData,
    )
  }

  /**
   * Converts a TestCase into a testStart and a testFinish events.
   */
  private fun processIncompleteInfoCase(
    testCase: IncompleteTestCase,
    parentId: String,
    parentSuiteName: String,
    systemOut: String?,
  ) {
    val testCaseTaskId = TaskId(UUID.randomUUID().toString(), listOf(parentId))

    // Extract the error summary message.
    val outcomeMessage =
      when {
        testCase.error != null -> testCase.error.message
        testCase.failure != null -> testCase.failure.message
        testCase.skipped != null -> testCase.skipped.message
        else -> null
      }
    val testStatus =
      when {
        testCase.error != null -> TestStatus.FAILED
        testCase.failure != null -> TestStatus.FAILED
        testCase.skipped != null -> TestStatus.SKIPPED
        else -> TestStatus.PASSED
      }
    val fullOutput =
      when {
        testCase.error != null -> testCase.error.content
        testCase.failure != null -> testCase.failure.content
        testCase.skipped != null -> testCase.skipped.content
        else -> systemOut
      }
    val errorType =
      when {
        testCase.error != null -> testCase.error.type
        testCase.failure != null -> testCase.failure.type
        else -> null
      }
    val message = listOfNotNull(outcomeMessage, systemOut).joinToString("\n")

    val testCaseData = JUnitStyleTestCaseData(testCase.time, testCase.name, message, fullOutput, errorType)

    // In the generated xml, suite name and test case name are the same, but in the Test Console test names have
    // to be unique
    val testCaseName = testCase.name.substringAfterLast('/')
    bspClientTestNotifier.startTest(testCaseName, testCaseTaskId, parentSuites = listOf(parentSuiteName))
    bspClientTestNotifier.finishTest(
      testCaseName,
      testCaseTaskId,
      testStatus,
      message,
      testCaseData,
    )
  }
}
