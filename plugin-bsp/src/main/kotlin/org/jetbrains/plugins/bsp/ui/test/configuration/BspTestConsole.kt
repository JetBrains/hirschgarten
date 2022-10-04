package org.jetbrains.plugins.bsp.ui.test.configuration

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.ServiceMessageBuilder
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.testframework.ui.BaseTestsOutputConsoleView
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageTypes
import java.io.OutputStream

private class BspProcHandler : ProcessHandler() {
  override fun destroyProcessImpl() {
    super.notifyProcessTerminated(0)
  }

  override fun detachProcessImpl() {}

  override fun detachIsDefault(): Boolean = false

  override fun getProcessInput(): OutputStream? = null
}

public class BspTestConsole {
  private var processHandler = BspProcHandler()
  private var console: BaseTestsOutputConsoleView? = null
  private var executionResult: ExecutionResult? = null

  /**
   * Gets the execution result of the console
   *
   * @return execution result of the current console if it exists, last saved result otherwise
   */
  public fun getExecutionResult(): ExecutionResult? {
    return if (console != null) {
      DefaultExecutionResult(console, processHandler)
    } else executionResult
  }

  /**
   * Commences the testing process and displays the testing console. Tests executed before this method's invocation
   * will not have their results shown
   *
   * @param properties test console properties
   */
  public fun beginTesting(properties: SMTRunnerConsoleProperties) {
    executionResult = null
    processHandler = BspProcHandler()
    processHandler.startNotify()
    console = SMTestRunnerConnectionUtil.createAndAttachConsole("BSP", processHandler, properties)
  }

  /**
   * Ends the testing process. Tests executed after this method's invocation will not have their results shown
   *
   * @return the console's execution result
   */
  public fun endTesting(): ExecutionResult? {
    processHandler.destroyProcess()
    if (console != null) {
      val ret = DefaultExecutionResult(console, processHandler)
      executionResult = ret
      console = null
      return ret
    }
    return null
  }

  /**
   * Starts a single test or a test suite
   *
   * @param isSuite `true` if a test suite has been started, `false` if it was a single test instead
   * @param name name of the started test (or the test suite)
   */
  public fun startTest(isSuite: Boolean, name: String) {
    executeCommand(
      if (isSuite) ServiceMessageTypes.TEST_SUITE_STARTED else ServiceMessageTypes.TEST_STARTED,
      "name" to name
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
    executeCommand(ServiceMessageTypes.TEST_FAILED,
      "name" to name,
      "error" to "true",
      "message" to message)
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

  /**
   * Prints a line of text in the test console
   *
   * @param text line to be printed (new-line character will be appended automatically)
   */
  public fun sendConsoleText(text: String) {
    notifyProcess(text + "\n")
  }

  private fun executeCommand(command: String, vararg pairs: Pair<String, String>) {
    val testSuiteStarted = ServiceMessageBuilder(command)
    pairs.iterator().forEach { testSuiteStarted.addAttribute(it.first, it.second) }
    notifyProcess(testSuiteStarted.toString() + "\n")
  }

  private fun notifyProcess(message: String) {
    processHandler.notifyTextAvailable(message, ProcessOutputTypes.STDOUT)
  }
}