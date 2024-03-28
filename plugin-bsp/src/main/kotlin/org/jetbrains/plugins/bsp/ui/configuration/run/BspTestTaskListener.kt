package org.jetbrains.plugins.bsp.ui.configuration.run

import ch.epfl.scala.bsp4j.StatusCode
import ch.epfl.scala.bsp4j.TestFinish
import ch.epfl.scala.bsp4j.TestReport
import ch.epfl.scala.bsp4j.TestStart
import ch.epfl.scala.bsp4j.TestStatus
import ch.epfl.scala.bsp4j.TestTask
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.testframework.sm.ServiceMessageBuilder
import com.intellij.openapi.util.Key
import org.jetbrains.plugins.bsp.services.BspTaskListener
import org.jetbrains.plugins.bsp.services.TaskId
import org.jetbrains.plugins.bsp.ui.configuration.BspProcessHandler

public class BspTestTaskListener(private val handler: BspProcessHandler<out Any>) : BspTaskListener {
  private val ansiEscapeDecoder = AnsiEscapeDecoder()

  init {
    handler.addProcessListener(object : ProcessListener {
      override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {
        // Not having this line causes the test tree to show "TERMINATED"
        handler.notifyTextAvailable("##teamcity[testingFinished]\n", ProcessOutputType.STDOUT)
      }
    })
  }

  override fun onTaskStart(taskId: TaskId, parentId: TaskId?, message: String, data: Any?) {
    when (data) {
      is TestTask -> {
        val testSuiteStarted = ServiceMessageBuilder.testSuiteStarted(data.target.uri).toString()
        handler.notifyTextAvailable(testSuiteStarted, ProcessOutputType.STDOUT)
      }

      is TestStart -> {
        val testStarted = ServiceMessageBuilder.testStarted(data.displayName).toString()
        handler.notifyTextAvailable(testStarted, ProcessOutputType.STDOUT)
      }
    }
  }

  override fun onTaskFinish(taskId: TaskId, message: String, status: StatusCode, data: Any?) {
    when (data) {
      is TestReport -> {
        val testSuiteFinished = ServiceMessageBuilder.testSuiteFinished(data.target.uri).toString()
        handler.notifyTextAvailable(testSuiteFinished, ProcessOutputType.STDOUT)
      }

      is TestFinish -> {
        val failureMessageBuilder = when (data.status!!) {
          TestStatus.FAILED -> {
            ServiceMessageBuilder.testFailed(data.displayName)
          }

          TestStatus.CANCELLED -> {
            ServiceMessageBuilder.testIgnored(data.displayName)
          }

          TestStatus.IGNORED -> {
            ServiceMessageBuilder.testIgnored(data.displayName)
          }

          TestStatus.SKIPPED -> {
            ServiceMessageBuilder.testIgnored(data.displayName)
          }

          else -> null
        }

        if (failureMessageBuilder != null) {
          failureMessageBuilder.addAttribute("message", data.message ?: "No message")
          handler.notifyTextAvailable(failureMessageBuilder.toString(), ProcessOutputType.STDOUT)
        }

        val testFinished = ServiceMessageBuilder.testFinished(data.displayName).toString()
        handler.notifyTextAvailable(testFinished, ProcessOutputType.STDOUT)
      }
    }
  }

  override fun onOutputStream(taskId: TaskId?, text: String) {
    ansiEscapeDecoder.escapeText(text, ProcessOutputType.STDOUT) { s: String, key: Key<Any> ->
      handler.notifyTextAvailable(s, key)
    }
  }

  override fun onErrorStream(taskId: TaskId?, text: String) {
    ansiEscapeDecoder.escapeText(text, ProcessOutputType.STDERR) { s: String, key: Key<Any> ->
      handler.notifyTextAvailable(s, key)
    }
  }

  // For compatibility with older BSP servers
  // TODO: Log messages in the correct place
  override fun onLogMessage(message: String) {
    val messageWithNewline = if (message.endsWith("\n")) message else "$message\n"
    ansiEscapeDecoder.escapeText(messageWithNewline, ProcessOutputType.STDOUT) { s: String, key: Key<Any> ->
      handler.notifyTextAvailable(s, key)
    }
  }
}
