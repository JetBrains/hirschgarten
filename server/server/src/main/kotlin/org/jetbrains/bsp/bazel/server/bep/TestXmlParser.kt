package org.jetbrains.bsp.bazel.server.bep

import ch.epfl.scala.bsp4j.TaskId
import ch.epfl.scala.bsp4j.TestStatus
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.jetbrains.bsp.bazel.logger.BspClientTestNotifier
import org.jetbrains.bsp.protocol.JUnitStyleTestCaseData
import org.jetbrains.bsp.protocol.JUnitStyleTestSuiteData
import java.io.File
import java.net.URI
import java.util.UUID

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

class TestXmlParser(private var parentId: TaskId, private var bspClientTestNotifier: BspClientTestNotifier) {
  private val fallbackTestXmlParser = FallbackTestXmlParser(parentId, bspClientTestNotifier)

  /**
   * Processes a test result xml file, reporting suite and test case results as task start and finish notifications.
   * Parent-child relationship is identified within each suite based on the TaskId.
   * @param testXmlUri Uri corresponding to the test result xml file to be processed.
   */
  fun parseAndReport(testXmlUri: String) {
    val testSuites = parseTestXml(testXmlUri, TestSuites::class.java)
    testSuites?.testsuite?.forEach { suite ->
      processSuite(suite)
    } ?: let {
      parseTestXml(testXmlUri, FallbackTestXmlParser.IncompleteTestSuites::class.java)
    }?.testsuite?.forEach { suite ->
      fallbackTestXmlParser.processIncompleteInfoSuite(suite)
    }
  }

  /**
   * Deserialize the given test report into the TestSuites/IncompleteTestSuites type as defined above.
   */
  private fun <T> parseTestXml(uri: String, valueType: Class<T>): T? {
    val xmlMapper =
      XmlMapper().apply {
        registerKotlinModule()
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      }

    var rawContent = File(URI.create(uri)).readText()
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
   * The parents field in each notification's TaskId will be used to indicate the parent-child relationship.
   * @param suite TestSuite to be processed.
   */
  private fun processSuite(suite: TestSuite) {
    val suiteTaskId = TaskId(UUID.randomUUID().toString())
    suiteTaskId.parents = emptyList()

    val suiteData = JUnitStyleTestSuiteData(suite.time, null, suite.systemErr?.toString())
    val suiteStatus =
      when {
        suite.failures > 0 -> TestStatus.FAILED
        suite.errors > 0 -> TestStatus.FAILED
        else -> TestStatus.PASSED
      }

    bspClientTestNotifier.startTest(suite.name, suiteTaskId)
    suite.testcase.forEach { case ->
      processTestCase(suiteTaskId.id, case)
    }
    bspClientTestNotifier.finishTest(
      suite.name,
      suiteTaskId,
      suiteStatus,
      suite.systemOut.toString(),
      JUnitStyleTestSuiteData.DATA_KIND,
      suiteData,
    )
  }

  /**
   * Convert a TestCase into a taskStart and taskFinish notification to the client.
   * The test case will be associated with its parent suite.
   * @param parentId String identifying the parent's TaskId. Used to indicate the proper tree structure.
   * @param testCase TestCase to be processed and sent to the client.
   */
  private fun processTestCase(parentId: String, testCase: TestCase) {
    val testCaseTaskId = TaskId(UUID.randomUUID().toString())
    testCaseTaskId.parents = listOf(parentId)

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
    bspClientTestNotifier.startTest(testCase.name, testCaseTaskId)
    bspClientTestNotifier.finishTest(
      testCase.name,
      testCaseTaskId,
      testStatusOutcome,
      "",
      JUnitStyleTestCaseData.DATA_KIND,
      testCaseData,
    )
  }
}

/** Bazel has a separate way of parsing JUnit4 and JUnit5 test results into a xml file, resulting in
 * incomplete data about the latter.
 * **/
private class FallbackTestXmlParser(private var parentId: TaskId, private var bspClientTestNotifier: BspClientTestNotifier) {
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
  )

  // A Bazel target is represented by a test suite containing one test case
  fun processIncompleteInfoSuite(suite: IncompleteTestSuite) {
    val suiteTaskId = TaskId(UUID.randomUUID().toString())
    suiteTaskId.parents = emptyList()
    val suiteStatus =
      when {
        suite.failures > 0 -> TestStatus.FAILED
        suite.errors > 0 -> TestStatus.FAILED
        else -> TestStatus.PASSED
      }
    val testSuiteData = JUnitStyleTestSuiteData(null, suite.systemOut, null)
    val testCase = suite.testcase.firstOrNull()

    bspClientTestNotifier.startTest(suite.name, suiteTaskId)
    testCase?.let { processIncompleteInfoCase(it, suiteTaskId.id, suiteStatus) }
    bspClientTestNotifier.finishTest(
      suite.name,
      suiteTaskId,
      suiteStatus,
      null,
      JUnitStyleTestSuiteData.DATA_KIND,
      testSuiteData,
    )
  }

  /**
   * Converts a TestCase into a testStart and a testFinish events.
   * @param testSuiteStatus - using test suite's status as test case status, because the xml one is not correct
   */
  private fun processIncompleteInfoCase(
    testCase: IncompleteTestCase,
    parentId: String,
    testSuiteStatus: TestStatus,
  ) {
    val testCaseTaskId = TaskId(UUID.randomUUID().toString())
    testCaseTaskId.parents = listOf(parentId)

    // Extract the error summary message.
    val outcomeMessage =
      when {
        testCase.error != null -> testCase.error.message
        testCase.failure != null -> testCase.failure.message
        testCase.skipped != null -> testCase.skipped.message
        else -> null
      }

    val testCaseData = JUnitStyleTestCaseData(null, null, outcomeMessage, null, null)

    // In the generated xml, suite name and test case name are the same, but in the Test Console test names have
    // to be unique
    val testCaseName = testCase.name.substringAfterLast('/')
    bspClientTestNotifier.startTest(testCaseName, testCaseTaskId)
    bspClientTestNotifier.finishTest(
      testCaseName,
      testCaseTaskId,
      testSuiteStatus,
      null,
      JUnitStyleTestCaseData.DATA_KIND,
      testCaseData,
    )
  }
}
