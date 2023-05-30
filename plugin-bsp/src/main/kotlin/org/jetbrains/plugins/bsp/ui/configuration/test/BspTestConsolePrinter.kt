package org.jetbrains.plugins.bsp.ui.configuration.test

import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.ServiceMessageBuilder
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.testframework.ui.BaseTestsOutputConsoleView
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageTypes
import org.jetbrains.plugins.bsp.ui.configuration.BspConsolePrinter
import org.jetbrains.plugins.bsp.ui.configuration.BspProcessHandler

public class BspTestConsolePrinter(
  private val processHandler: BspProcessHandler,
  properties: SMTRunnerConsoleProperties
) : BspConsolePrinter by processHandler {

  public var console: BaseTestsOutputConsoleView =
    SMTestRunnerConnectionUtil.createAndAttachConsole("BSP", processHandler, properties)

  init {
    processHandler.startNotify()
  }

  /**
   * Ends the testing process. Tests executed after this method's invocation will not have their results shown
   */
  public fun endTesting() {
    processHandler.shutdown()
  }

  /**
   * Starts a single test or a test suite
   *
   * @param isSuite `true` if a test suite has been started, `false` if it was a single test instead
   * @param name name of the started test (or the test suite)
   */
  public fun startTest(isSuite: Boolean, name: String) {
    executeCommand(
      if (isSuite) ServiceMessageTypes.TEST_SUITE_STARTED else ServiceMessageTypes.TEST_STARTED, "name" to name
    )
  }

  /**
   * Finishes a single test (as a success) or a test suite
   *
   * @param isSuite `true` if a test suite has been finished, `false` if it was a single test instead
   * @param name name of the finished test (or the test suite)
   */
  public fun passTest(isSuite: Boolean, name: String) {
    executeCommand(
      if (isSuite) ServiceMessageTypes.TEST_SUITE_FINISHED else ServiceMessageTypes.TEST_FINISHED,
      "name" to name
    )
  }

  /**
   * Finishes a single test as a failure. Result of finishing test suites is decided automatically, so for those
   * use `passTest(true, ...)`
   *
   * @param name name of the finished test (or the test suite)
   * @param message additional information about the failure
   */
  public fun failTest(name: String, message: String) {
    executeCommand(
      ServiceMessageTypes.TEST_FAILED,
      "name" to name,
      "error" to "true",
      "message" to message
    )
  }

  /**
   * Finishes a single test as ignored. Result of finishing test suites is decided automatically, so for those
   * use `passTest(true, ...)`
   *
   * @param name name of the finished test (or the test suite)
   */
  public fun ignoreTest(name: String) {
    executeCommand(ServiceMessageTypes.TEST_IGNORED, "name" to name)
  }

  private fun executeCommand(command: String, vararg pairs: Pair<String, String>) {
    val testSuiteStarted = ServiceMessageBuilder(command)
    pairs.iterator().forEach { testSuiteStarted.addAttribute(it.first, it.second) }
    printOutput(testSuiteStarted.toString())
  }
}
