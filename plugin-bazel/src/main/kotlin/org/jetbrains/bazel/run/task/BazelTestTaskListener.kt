package org.jetbrains.bazel.run.task

import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.testframework.sm.ServiceMessageBuilder
import com.intellij.openapi.util.Key
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.run.BazelProcessHandler
import org.jetbrains.bazel.taskEvents.BazelTaskListener
import org.jetbrains.bsp.protocol.JUnitStyleTestCaseData
import org.jetbrains.bsp.protocol.JUnitStyleTestSuiteData
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.bsp.protocol.TestFinish
import org.jetbrains.bsp.protocol.TestStart
import org.jetbrains.bsp.protocol.TestStatus
import org.jetbrains.bsp.protocol.TestTask
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class BazelTestTaskListener(private val handler: BazelProcessHandler, private val coverageReportListener: ((Path) -> Unit)? = null) :
  BazelTaskListener {
  private val ansiEscapeDecoder = AnsiEscapeDecoder()

  init {
    handler.addProcessListener(
      object : ProcessListener {
        override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {}
      },
    )
  }

  private val testStartData = ConcurrentHashMap<TaskId, TestStart>()

  private fun findParentTaskId(taskId: TaskId): TaskId? {
    var id = taskId.parent
    while (id != null) {
      if (testStartData.containsKey(id))
        return id
      id = id.parent
    }
    return null
  }

  override fun onTaskStart(
    taskId: TaskId,
    message: String,
    data: Any?,
  ) {
    when (data) {
      is TestTask -> {
        // OutputToGeneralTestEventsConverter.MyServiceMessageVisitor.visitServiceMessage ignores the first testingStarted event
        handler.notifyTextAvailable(ServiceMessageBuilder("testingStarted").toString().toStringWithNewline(), ProcessOutputType.STDOUT)
        handler.notifyTextAvailable(ServiceMessageBuilder("testingStarted").toString().toStringWithNewline(), ProcessOutputType.STDOUT)
      }

      is TestStart -> {
        testStartData[taskId] = data
        val parentTaskId = findParentTaskId(taskId)
        val serviceMessage =
          if (data.isSuit) {
            ServiceMessageBuilder
              .testSuiteStarted(data.displayName)
              .addNodeId(taskId.id)
              .addAttribute("parentNodeId", parentTaskId?.id ?: "0")
              .addAttribute("locationHint", data.locationHint)
              .toString()
          }
          else {
            ServiceMessageBuilder
              .testStarted(data.displayName)
              .addNodeId(taskId.id)
              .addAttribute("parentNodeId", parentTaskId?.id ?: "0")
              .addAttribute("locationHint", data.locationHint)
              .toString()
          }
        handler.notifyTextAvailable(serviceMessage.toStringWithNewline(), ProcessOutputType.STDOUT)
      }
    }
  }

  override fun onTaskFinish(
    taskId: TaskId,
    message: String,
    status: BazelStatus,
    data: Any?,
  ) {
    val startData = testStartData.remove(taskId)

    when (data) {
      is TestFinish -> {
        val serviceMessage =
          if (startData?.isSuit == true) {
            processTestSuiteFinish(taskId, data)
          } else {
            processTestCaseFinish(taskId, data)
          }

        handler.notifyTextAvailable(serviceMessage.toString().toStringWithNewline(), ProcessOutputType.STDOUT)
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
  override fun onLogMessage(taskId: TaskId, message: String) {
    val messageWithNewline = message.toStringWithNewline()
    ansiEscapeDecoder.escapeText(messageWithNewline, ProcessOutputType.STDOUT) { s: String, key: Key<Any> ->
      handler.notifyTextAvailable(s, key)
    }
  }

  override fun onPublishCoverageReport(coverageReport: Path) {
    coverageReportListener?.invoke(coverageReport)
  }

  private fun checkTestStatus(
    taskId: TaskId,
    data: TestFinish,
    details: JUnitStyleTestCaseData?,
  ) {
    val failureMessageBuilder =
      when (data.status) {
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
      // if the error message is empty or blank, IntelliJ will see the test case as successful
      val errorMessage =
        details?.errorMessage?.takeIf { it.isNotBlank() } ?: "Failed"
      failureMessageBuilder
        .addNodeId(taskId.id)
        .addMessage(errorMessage)
        .let { if (details?.errorType == null) it else it.addAttribute("type", details.errorType) }
        .toString()
      handler.notifyTextAvailable(failureMessageBuilder.toString().toStringWithNewline(), ProcessOutputType.STDOUT)
      details?.output?.let { handler.notifyTextAvailable(it, ProcessOutputType.STDERR) }
    } else {
      details?.output?.let { handler.notifyTextAvailable(it, ProcessOutputType.STDOUT) }
    }
  }

  private fun processTestCaseFinish(taskId: TaskId, data: TestFinish): ServiceMessageBuilder {
    val details = extractTestFinishData<JUnitStyleTestCaseData>(data)

    checkTestStatus(taskId, data, details)

    return ServiceMessageBuilder
      .testFinished(data.displayName)
      .addNodeId(taskId.id)
      .addMessage(data.message)
      .addTime(details?.time)
  }

  private fun processTestSuiteFinish(taskId: TaskId, data: TestFinish): ServiceMessageBuilder {
    val details = extractTestFinishData<JUnitStyleTestSuiteData>(data)

    details?.systemOut?.let { handler.notifyTextAvailable(it, ProcessOutputType.STDOUT) }
    details?.systemErr?.let { handler.notifyTextAvailable(it, ProcessOutputType.STDERR) }
    return ServiceMessageBuilder
      .testSuiteFinished(data.displayName)
      .addNodeId(taskId.id)
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

  private inline fun <reified Data> extractTestFinishData(testFinishData: TestFinish): Data? = testFinishData.data as? Data
}

/**
 * If a system message sent to the process handler does not end with a newline,
 * it might connect to another message and not be parsed correctly
 * */
private fun String.toStringWithNewline(): String = if (this.endsWith("\n")) this else "$this\n"
