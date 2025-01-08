package org.jetbrains.plugins.bsp.run.task

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.StatusCode
import ch.epfl.scala.bsp4j.TestFinish
import ch.epfl.scala.bsp4j.TestStart
import ch.epfl.scala.bsp4j.TestStatus
import ch.epfl.scala.bsp4j.TestTask
import com.google.idea.testing.BazelTestApplication
import com.intellij.execution.testframework.sm.ServiceMessageBuilder
import com.intellij.openapi.util.Key
import io.kotest.matchers.equals.shouldBeEqual
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.bsp.protocol.JUnitStyleTestSuiteData
import org.jetbrains.plugins.bsp.run.BspProcessHandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class MockBspProcessHandler : BspProcessHandler(CompletableDeferred(0)) {
  var latestText: String = ""

  override fun notifyTextAvailable(text: String, outputType: Key<*>) {
    latestText = text
  }

  override fun startNotify() {}

  override fun destroyProcessImpl() {}

  override fun detachProcessImpl() {}
}

@BazelTestApplication
class BspTestTaskListenerTest {
  private lateinit var handler: MockBspProcessHandler
  private lateinit var listener: BspTestTaskListener

  @BeforeEach
  fun init() {
    handler = MockBspProcessHandler()
    listener = BspTestTaskListener(handler)
  }

  @Test
  fun `test-task`() {
    // given
    val expectedText = ServiceMessageBuilder("testingStarted").toString()
    val data = TestTask(BuildTargetIdentifier("id"))

    // when
    listener.onTaskStart(taskId = "task-id", parentId = null, message = "", data = data)

    // then
    handler.latestText shouldBeEqual expectedText
  }

  @Test
  fun `task-start with test suite`() {
    // given
    val taskId = "task-id"
    val data = TestStart("testSuite")
    val expectedText =
      ServiceMessageBuilder
        .testSuiteStarted(
          data.displayName,
        ).addAttribute("nodeId", taskId)
        .addAttribute("parentNodeId", "0")
        .toString()

    // when
    listener.onTaskStart(taskId = taskId, parentId = null, message = "", data = data)

    // then
    handler.latestText shouldBeEqual expectedText
  }

  @Test
  fun `task-start with test case`() {
    // given
    val taskId = "task-id"
    val parentId = "parent-id"
    val data = TestStart("testCase")
    val expectedText =
      ServiceMessageBuilder
        .testStarted(
          data.displayName,
        ).addAttribute("nodeId", taskId)
        .addAttribute("parentNodeId", parentId)
        .toString()

    // when
    listener.onTaskStart(taskId = taskId, parentId = parentId, message = "", data = data)

    // then
    handler.latestText shouldBeEqual expectedText
  }

  @Test
  fun `task-finish with test suite`() {
    // given
    val durationSeconds = 1.23456
    val taskId = "task-id"
    val testFinishData = TestFinish("testSuite", TestStatus.PASSED)
    testFinishData.data = JUnitStyleTestSuiteData(durationSeconds, null, null)
    testFinishData.dataKind = JUnitStyleTestSuiteData.DATA_KIND

    val expectedDurationMillis = (1234).toLong()
    val expectedText =
      ServiceMessageBuilder
        .testSuiteFinished(testFinishData.displayName)
        .addAttribute("nodeId", taskId)
        .addAttribute("duration", expectedDurationMillis.toString())
        .toString()

    // when
    listener.onTaskFinish(taskId = taskId, parentId = null, message = "", data = testFinishData, status = StatusCode.OK)

    // then
    handler.latestText shouldBeEqual expectedText
  }
}
