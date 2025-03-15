/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.bazel.ogRun.smrunner

import com.google.common.base.Joiner

import com.google.common.collect.ImmutableMap
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.idea.blaze.base.command.buildresult.BuildResultHelper.GetArtifactsException
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.execution.testframework.TestConsoleProperties
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter
import com.intellij.execution.testframework.sm.runner.events.TestOutputEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.ProjectManager
import jetbrains.buildServer.messages.serviceMessages.TestSuiteStarted
import org.jetbrains.bazel.ogRun.testlogs.BlazeTestResult
import org.jetbrains.bazel.ogRun.testlogs.BlazeTestResultFinderStrategy
import org.jetbrains.bazel.ogRun.testlogs.BlazeTestResults
import java.util.*
import java.util.function.Consumer

/** Converts blaze test runner xml logs to smRunner events.  */
class BlazeXmlToTestEventsConverter(
  testFrameworkName: String,
  testConsoleProperties: TestConsoleProperties,
  testResultFinderStrategy: BlazeTestResultFinderStrategy,
) : OutputToGeneralTestEventsConverter(testFrameworkName, testConsoleProperties) {
  init {
    NO_ERROR.message = "No message" // cannot be null
  }

  private val testResultFinderStrategy: BlazeTestResultFinderStrategy

  override fun flushBufferOnProcessTermination(exitCode: Int) {
    super.flushBufferOnProcessTermination(exitCode)

    try {
      val testResults: BlazeTestResults? = testResultFinderStrategy.findTestResults()
      if (testResults == BlazeTestResults.NO_RESULTS) {
        reportError(exitCode)
      } else {
        processAllTestResults(testResults)
      }
    } catch (e: GetArtifactsException) {
      Logger.getInstance(this.javaClass).error(e.getMessage())
    } finally {
      testResultFinderStrategy.deleteTemporaryOutputFiles()
    }
  }

  private fun processAllTestResults(testResults: BlazeTestResults) {
    onStartTesting()
    getProcessor().onTestsReporterAttached()
    val futures: MutableList<ListenableFuture<ParsedTargetResults?>?> =
      ArrayList<ListenableFuture<ParsedTargetResults?>?>()
    for (label in testResults.perTargetResults.keySet()) {
      futures.add(
        FetchExecutor.EXECUTOR.submit(
          { Companion.parseTestXml(label, testResults.perTargetResults.get(label)) },
        ),
      )
    }
    val parsedResults: MutableList<ParsedTargetResults?>? =
      FuturesUtil.getIgnoringErrors<MutableList<ParsedTargetResults?>?>(
        Futures.allAsList<ParsedTargetResults?>(
          futures,
        ),
      )
    if (parsedResults != null) {
      parsedResults.forEach(
        Consumer { parsedResults: ParsedTargetResults? ->
          this.processParsedTestResults(
            parsedResults!!,
          )
        },
      )
    }
  }

  private fun reportError(exitCode: Int) {
    val exitStatus = BlazeTestExitStatus.forExitCode(exitCode)
    if (exitStatus == null) {
      reportTestRuntimeError(
        "Unknown Error",
        "Test runtime terminated unexpectedly with exit code " + exitCode + ".",
      )
    } else {
      reportTestRuntimeError(exitStatus.title, exitStatus.message!!)
    }
  }

  /** Parsed test output for a single target.  */
  private class ParsedTargetResults(
    label: Label,
    results: MutableCollection<BlazeTestResult?>,
    outputFiles: MutableList<BlazeArtifact>,
    targetSuites: MutableList<BlazeXmlSchema.TestSuite>,
  ) {
    private val label: Label
    private val results: MutableCollection<BlazeTestResult?>
    private val outputFiles: MutableList<BlazeArtifact>
    private val targetSuites: MutableList<BlazeXmlSchema.TestSuite>

    init {
      this.label = label
      this.results = results
      this.outputFiles = outputFiles
      this.targetSuites = targetSuites
    }
  }

  /** Process all parsed test XML files from a single test target.  */
  private fun processParsedTestResults(parsedResults: ParsedTargetResults) {
    if (noUsefulOutput(parsedResults.results, parsedResults.outputFiles)) {
      val status: BlazeTestResult.TestStatus? =
        parsedResults.results
          .stream()
          .map<Any?>(BlazeTestResult::getTestStatus)
          .findFirst()
      status.ifPresent(
        Consumer { testStatus: BlazeTestResult.TestStatus? ->
          reportTargetWithoutOutputFiles(
            parsedResults.label,
            testStatus,
          )
        },
      )
      return
    }

    val kind: Kind? =
      parsedResults.results
        .stream()
        .map<Any?>(BlazeTestResult::getTargetKind)
        .filter { obj: Any? -> Objects.nonNull(obj) }
        .findFirst()
        .orElse(null)
    val eventsHandler =
      BlazeTestEventsHandler.getHandlerForTargetKindOrFallback(kind)
    val suite =
      if (parsedResults.targetSuites.size == 1) {
        parsedResults.targetSuites.get(0)
      } else {
        BlazeXmlSchema.mergeSuites(parsedResults.targetSuites)
      }
    processTestSuite(getProcessor(), eventsHandler, parsedResults.label, kind, suite)
  }

  /**
   * If an error occurred when running the test the user should be informed with sensible error
   * messages to help them decide what to do next. (e.g. re-run the test?)
   */
  private fun reportTestRuntimeError(errorName: String?, errorMessage: String) {
    val processor: GeneralTestEventsProcessor = getProcessor()
    processor.onTestFailure(
      getTestFailedEvent(errorName, errorMessage, null, BlazeComparisonFailureData.NONE, 0, true),
    )
  }

  /**
   * If there are no output files, the test may have failed to build, or timed out. Provide a
   * suitable message in the test UI.
   */
  private fun reportTargetWithoutOutputFiles(label: Label, status: BlazeTestResult.TestStatus?) {
    if (status == BlazeTestResult.TestStatus.PASSED) {
      // Empty test targets do not produce output XML, yet technically pass. Ignore them.
      return
    }
    val processor: GeneralTestEventsProcessor = getProcessor()
    val suiteStarted = TestSuiteStarted(label.toString())
    processor.onSuiteStarted(TestSuiteStartedEvent(suiteStarted, /*locationUrl=*/null))
    val targetName = label.targetName().toString()
    processor.onTestStarted(TestStartedEvent(targetName, /*locationUrl=*/null))
    processor.onTestFailure(
      getTestFailedEvent(
        targetName,
        STATUS_EXPLANATIONS.getOrDefault(status, "No output found for test target.") +
          " See console output for details",
        // content=
        null,
        BlazeComparisonFailureData.NONE, // duration=
        0,
        true,
      ),
    )
    processor.onTestFinished(TestFinishedEvent(targetName, /*duration=*/0L))
    processor.onSuiteFinished(TestSuiteFinishedEvent(label.toString()))
  }

  init {
    this.testResultFinderStrategy = testResultFinderStrategy
  }

  companion object {
    private val NO_ERROR: ErrorOrFailureOrSkipped = ErrorOrFailureOrSkipped()
    private val removeZeroRunTimeCheck: BoolExperiment = BoolExperiment("remove.zero.run.time.check", true)

    /** Parse all test XML files from a single test target.  */
    private fun parseTestXml(label: Label, results: MutableCollection<BlazeTestResult?>): ParsedTargetResults {
      val outputFiles: MutableList<BlazeArtifact> = ArrayList<BlazeArtifact>()
      results.forEach(Consumer { result: BlazeTestResult? -> outputFiles.addAll(result.outputXmlFiles) })
      val targetSuites: MutableList<BlazeXmlSchema.TestSuite> = ArrayList<BlazeXmlSchema.TestSuite>()
      for (file in outputFiles) {
        try {
          file.getInputStream().use { input ->
            targetSuites.add(BlazeXmlSchema.parse(input)!!)
          }
        } catch (e: Exception) {
          // ignore parsing errors -- most common cause is user cancellation, which we can't easily
          // recognize.
        }
      }
      return ParsedTargetResults(label, results, outputFiles, targetSuites)
    }

    /** Return false if there's output XML which should be parsed.  */
    private fun noUsefulOutput(results: MutableCollection<BlazeTestResult?>, outputFiles: MutableList<BlazeArtifact>): Boolean {
      if (outputFiles.isEmpty()) {
        return true
      }
      val status: BlazeTestResult.TestStatus? =
        results
          .stream()
          .map<Any?>(BlazeTestResult::getTestStatus)
          .findFirst()
          .orElse(null)
      return status != null && BlazeTestResult.NO_USEFUL_OUTPUT.contains(status)
    }

    /** Status explanations for tests without output XML.  */
    private val STATUS_EXPLANATIONS: ImmutableMap<BlazeTestResult.TestStatus?, String?> =
      ImmutableMap
        .Builder<BlazeTestResult.TestStatus?, String?>()
        .put(BlazeTestResult.TestStatus.TIMEOUT, "Test target timed out.")
        .put(BlazeTestResult.TestStatus.INCOMPLETE, "Test output was incomplete.")
        .put(BlazeTestResult.TestStatus.REMOTE_FAILURE, "Remote failure during test execution.")
        .put(BlazeTestResult.TestStatus.FAILED_TO_BUILD, "Test target failed to build.")
        .put(BlazeTestResult.TestStatus.TOOL_HALTED_BEFORE_TESTING, "Test target failed to build.")
        .put(BlazeTestResult.TestStatus.NO_STATUS, "No output found for test target.")
        .build()

    private fun processTestSuite(
      processor: GeneralTestEventsProcessor,
      eventsHandler: BlazeTestEventsHandler,
      label: Label,
      kind: Kind?,
      suite: BlazeXmlSchema.TestSuite,
    ) {
      if (!hasRunChild(suite)) {
        return
      }
      // only include the innermost 'testsuite' element
      val logSuite = !eventsHandler.ignoreSuite(label, kind, suite)
      if (suite.name != null && logSuite) {
        val suiteStarted =
          TestSuiteStarted(eventsHandler.suiteDisplayName(label, kind, suite.name)!!)
        val locationUrl = eventsHandler.suiteLocationUrl(label, kind, suite.name)
        processor.onSuiteStarted(TestSuiteStartedEvent(suiteStarted, locationUrl))
      }

      for (child in suite.testSuites) {
        processTestSuite(processor, eventsHandler, label, kind, child)
      }
      for (decorator in suite.testDecorators) {
        Companion.processTestSuite(processor, eventsHandler, label, kind, decorator!!)
      }
      for (test in suite.testCases) {
        Companion.processTestCase(processor, eventsHandler, label, kind, suite, test!!)
      }

      if (suite.sysOut != null) {
        processor.onUncapturedOutput(suite.sysOut, ProcessOutputTypes.STDOUT)
      }
      if (suite.sysErr != null) {
        processor.onUncapturedOutput(suite.sysErr, ProcessOutputTypes.STDERR)
      }

      if (suite.name != null && logSuite) {
        processor.onSuiteFinished(
          TestSuiteFinishedEvent(eventsHandler.suiteDisplayName(label, kind, suite.name)),
        )
      }
    }

    /**
     * Does the test suite have at least one child which wasn't skipped? <br></br>
     * This prevents spurious warnings from entirely filtered test classes.
     */
    private fun hasRunChild(suite: BlazeXmlSchema.TestSuite): Boolean {
      for (child in suite.testSuites) {
        if (hasRunChild(child)) {
          return true
        }
      }
      for (child in suite.testDecorators) {
        if (Companion.hasRunChild(child!!)) {
          return true
        }
      }
      for (test in suite.testCases) {
        if (Companion.wasRun(test!!) && !Companion.isIgnored(test)) {
          return true
        }
      }
      return false
    }

    private fun isCancelled(test: BlazeXmlSchema.TestCase): Boolean =
      "interrupted".equals(test.result, ignoreCase = true) ||
        "cancelled".equals(
          test.result,
          ignoreCase = true,
        )

    private fun wasRun(test: BlazeXmlSchema.TestCase): Boolean {
      if (test.status != null) {
        return test.status == "run"
      }
      // 'status' is not always set. In cases where it's not,
      if (removeZeroRunTimeCheck.getValue() && bazelIsAtLeastVersion(0, 13, 0)) {
        // bazel 0.13.0 and after, tests which aren't run are skipped from the XML entirely.
        return true
      } else {
        // before 0.13.0, tests which aren't run have a 0 runtime.
        return parseTimeMillis(test.time) != 0L
      }
    }

    fun isIgnored(test: BlazeXmlSchema.TestCase): Boolean {
      if (test.skipped != null) {
        return true
      }
      return "suppressed".equals(test.result, ignoreCase = true) ||
        "skipped".equals(test.result, ignoreCase = true) ||
        "filtered".equals(test.result, ignoreCase = true)
    }

    private fun isFailed(test: BlazeXmlSchema.TestCase): Boolean = !test.failures.isEmpty() || !test.errors.isEmpty()

    private fun processTestCase(
      processor: GeneralTestEventsProcessor,
      eventsHandler: BlazeTestEventsHandler,
      label: Label,
      kind: Kind?,
      parent: BlazeXmlSchema.TestSuite,
      test: BlazeXmlSchema.TestCase,
    ) {
      if (test.name == null || !wasRun(test) || isCancelled(test)) {
        return
      }
      val displayName = eventsHandler.testDisplayName(label, kind, test.name)
      val locationUrl =
        eventsHandler.testLocationUrl(label, kind, parent.name, test.name, test.classname)
      processor.onTestStarted(TestStartedEvent(displayName, locationUrl))

      if (test.sysOut != null) {
        processor.onTestOutput(TestOutputEvent(displayName!!, test.sysOut!!, true))
      }
      if (test.sysErr != null) {
        processor.onTestOutput(TestOutputEvent(displayName!!, test.sysErr!!, false))
      }

      if (isIgnored(test)) {
        val err: ErrorOrFailureOrSkipped = if (test.skipped != null) test.skipped else NO_ERROR
        val message = if (err.message == null) "" else err.message
        processor.onTestIgnored(
          TestIgnoredEvent(displayName, message, BlazeXmlSchema.getErrorContent(err)),
        )
      } else if (isFailed(test)) {
        val errors: MutableList<ErrorOrFailureOrSkipped> =
          if (!test.failures.isEmpty()) {
            test.failures
          } else {
            if (!test.errors.isEmpty()) {
              test.errors
            } else {
              listOf<ErrorOrFailureOrSkipped?>(NO_ERROR)
            }
          }
        val isError = test.failures.isEmpty()
        for (err in errors) {
          processor.onTestFailure(getTestFailedEvent(displayName, err, parseTimeMillis(test.time), isError))
        }
      }
      processor.onTestFinished(TestFinishedEvent(displayName, parseTimeMillis(test.time)))
    }

    /**
     * Remove any duplicate copy of the brief error message from the detailed error content (generally
     * including a stack trace).
     */
    private fun pruneErrorMessage(message: String?, content: String?): String? {
      if (message == null) {
        return null
      }
      return if (content != null) content.replace(message, "") else null
    }

    private fun getTestFailedEvent(
      name: String?,
      error: ErrorOrFailureOrSkipped,
      duration: Long,
      isError: Boolean,
    ): TestFailedEvent {
      val message =
        if (error.message != null) error.message else "Test failed (no error message present)"
      val content: String? = pruneErrorMessage(error.message, BlazeXmlSchema.getErrorContent(error))
      return getTestFailedEvent(name, message, content, parseComparisonData(error), duration, isError)
    }

    private fun getTestFailedEvent(
      name: String?,
      message: String,
      content: String?,
      comparisonData: BlazeComparisonFailureData,
      duration: Long,
      isError: Boolean,
    ): TestFailedEvent =
      TestFailedEvent(
        name,
        null,
        message,
        content,
        isError,
        comparisonData.actual,
        comparisonData.expected,
        null,
        null,
        false,
        false,
        duration,
      )

    private fun parseComparisonData(error: ErrorOrFailureOrSkipped): BlazeComparisonFailureData? {
      if (error.actual != null || error.expected != null) {
        return BlazeComparisonFailureData(
          parseComparisonString(error.actual),
          parseComparisonString(error.expected),
        )
      }
      if (error.message != null) {
        return TestComparisonFailureParser.parse(error.message)
      }
      return BlazeComparisonFailureData.NONE
    }

    private fun parseComparisonString(values: BlazeXmlSchema.Values?): String? =
      if (values != null) Joiner.on("\n").skipNulls().join(values.values) else null

    private fun parseTimeMillis(time: String?): Long {
      if (time == null) {
        return -1
      }
      // if the number contains a decimal point, it's a value in seconds. Otherwise in milliseconds.
      try {
        if (time.contains(".")) {
          return Math.round(time.toFloat() * 1000).toLong()
        }
        return time.toLong()
      } catch (e: NumberFormatException) {
        return -1
      }
    }

    /**
     * @return true if bazel version is at least major.minor.bugfix, or if bazel version is not
     * applicable (i.e., is blaze, or bazel developmental version).
     */
    private fun bazelIsAtLeastVersion(
      major: Int,
      minor: Int,
      bugfix: Int,
    ): Boolean {
      for (project in ProjectManager.getInstance().getOpenProjects()) {
        if (Blaze.getBuildSystemName(project) === BuildSystemName.Bazel) {
          val projectData: BlazeProjectData? =
            BlazeProjectDataManager.getInstance(project).getBlazeProjectData()
          if (projectData != null) {
            return projectData.getBlazeVersionData().bazelIsAtLeastVersion(major, minor, bugfix)
          }
        }
      }
      return true // assume recent bazel by default.
    }
  }
}
