package org.jetbrains.plugins.bsp.run.task

import ch.epfl.scala.bsp4j.StatusCode
import ch.epfl.scala.bsp4j.TestFinish
import ch.epfl.scala.bsp4j.TestReport
import ch.epfl.scala.bsp4j.TestStart
import ch.epfl.scala.bsp4j.TestStatus
import ch.epfl.scala.bsp4j.TestTask
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.testframework.sm.ServiceMessageBuilder
import com.intellij.openapi.util.Key
import org.jetbrains.bsp.protocol.JUnitStyleTestCaseData
import org.jetbrains.bsp.protocol.JUnitStyleTestSuiteData
import org.jetbrains.plugins.bsp.run.BspProcessHandler
import org.jetbrains.plugins.bsp.taskEvents.BspTaskListener
import org.jetbrains.plugins.bsp.taskEvents.TaskId
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class BspTestTaskListener(private val handler: BspProcessHandler) : BspTaskListener {
  private val ansiEscapeDecoder = AnsiEscapeDecoder()
  private val gson = Gson()

  init {
    handler.addProcessListener(
      object : ProcessListener {
        override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {}
      },
    )
  }

  override fun onTaskStart(
    taskId: TaskId,
    parentId: TaskId?,
    message: String,
    data: Any?,
  ) {
    when (data) {
      is TestTask -> {
        // OutputToGeneralTestEventsConverter.MyServiceMessageVisitor.visitServiceMessage ignores the first testingStarted event
        handler.notifyTextAvailable(ServiceMessageBuilder("testingStarted").toString(), ProcessOutputType.STDOUT)
        handler.notifyTextAvailable(ServiceMessageBuilder("testingStarted").toString(), ProcessOutputType.STDOUT)
      }

      is TestStart -> {
        val serviceMessage =
          if (parentId != null) {
            ServiceMessageBuilder
              .testStarted(data.displayName)
              .addNodeId(taskId)
              .addAttribute("parentNodeId", parentId)
              .toString()
          } else {
            ServiceMessageBuilder
              .testSuiteStarted(data.displayName)
              .addNodeId(taskId)
              .addAttribute("parentNodeId", "0")
              .toString()
          }
        handler.notifyTextAvailable(serviceMessage, ProcessOutputType.STDOUT)
      }
    }
  }

  override fun onTaskFinish(
    taskId: TaskId,
    parentId: TaskId?,
    message: String,
    status: StatusCode,
    data: Any?,
  ) {
    when (data) {
      is TestReport -> {
        handler.notifyTextAvailable(ServiceMessageBuilder("testingFinished").toString(), ProcessOutputType.STDOUT)
      }

      is TestFinish -> {
        val serviceMessage =
          if (parentId != null) {
            processTestCaseFinish(taskId, data)
          } else {
            processTestSuiteFinish(taskId, data)
          }

        handler.notifyTextAvailable(serviceMessage.toString(), ProcessOutputType.STDOUT)
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

  private fun checkTestStatus(
    taskId: TaskId,
    data: TestFinish,
    details: JUnitStyleTestCaseData?,
  ) {
    val failureMessageBuilder =
      when (data.status!!) {
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
      failureMessageBuilder
        .addNodeId(taskId)
        .addMessage(details?.errorMessage)
        .let { if (details?.errorType == null) it else it.addAttribute("type", details.errorType) }
        .toString()
      handler.notifyTextAvailable(failureMessageBuilder.toString(), ProcessOutputType.STDOUT)
      details?.errorContent?.let { handler.notifyTextAvailable(it, ProcessOutputType.STDERR) }
    }
  }

  private fun processTestCaseFinish(taskId: TaskId, data: TestFinish): ServiceMessageBuilder {
    val details = extractTestFinishData<JUnitStyleTestCaseData>(data, JUnitStyleTestCaseData.DATA_KIND)

    checkTestStatus(taskId, data, details)

    return ServiceMessageBuilder
      .testFinished(data.displayName)
      .addNodeId(taskId)
      .addMessage(data.message)
      .addTime(details?.time)
  }

  private fun processTestSuiteFinish(taskId: TaskId, data: TestFinish): ServiceMessageBuilder {
    val details = extractTestFinishData<JUnitStyleTestSuiteData>(data, JUnitStyleTestSuiteData.DATA_KIND)

    details?.systemOut?.let { handler.notifyTextAvailable(it, ProcessOutputType.STDOUT) }
    details?.systemErr?.let { handler.notifyTextAvailable(it, ProcessOutputType.STDERR) }
    return ServiceMessageBuilder
      .testSuiteFinished(data.displayName)
      .addNodeId(taskId)
      .addTime(details?.time)
  }

  private fun ServiceMessageBuilder.addNodeId(nodeId: String): ServiceMessageBuilder = this.addAttribute("nodeId", nodeId)

  private fun ServiceMessageBuilder.addMessage(message: String?): ServiceMessageBuilder =
    message?.takeIf { it.isNotEmpty() }?.let { this.addAttribute("message", it) } ?: this

  private fun ServiceMessageBuilder.addTime(time: Double?): ServiceMessageBuilder =
    time
      ?.takeIf { it.isFinite() }
      ?.toDuration(DurationUnit.SECONDS)
      ?.inWholeMilliseconds
      ?.let { this.addAttribute("duration", it.toString()) }
      ?: this

  private inline fun <reified Data> extractTestFinishData(testFinishData: TestFinish, kind: String): Data? =
    if (testFinishData.data is Data) {
      testFinishData.data as Data
    } else if (testFinishData.dataKind == kind) {
      gson.fromJson(testFinishData.data as JsonObject, Data::class.java)
    } else {
      null
    }
}
