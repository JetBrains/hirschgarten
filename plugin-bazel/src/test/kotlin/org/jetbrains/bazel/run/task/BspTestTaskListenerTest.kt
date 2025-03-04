package org.jetbrains.bazel.run.task

import com.google.idea.testing.BazelTestApplication
import com.intellij.execution.testframework.sm.ServiceMessageBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import io.kotest.matchers.equals.shouldBeEqual
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.BspProcessHandler
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.jetbrains.bsp.protocol.JUnitStyleTestSuiteData
import org.jetbrains.bsp.protocol.StatusCode
import org.jetbrains.bsp.protocol.TestFinish
import org.jetbrains.bsp.protocol.TestStart
import org.jetbrains.bsp.protocol.TestStatus
import org.jetbrains.bsp.protocol.TestTask
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class MockBspProcessHandler(project: Project) : BspProcessHandler(project, CompletableDeferred(0)) {
  var latestText: String = ""

  override fun notifyTextAvailable(text: String, outputType: Key<*>) {
    latestText = text
  }

  override fun startNotify() {}

  override fun destroyProcessImpl() {}

  override fun detachProcessImpl() {}
}

@BazelTestApplication
class BspTestTaskListenerTest : WorkspaceModelBaseTest() {
  private lateinit var handler: MockBspProcessHandler
  private lateinit var listener: BspTestTaskListener

  @BeforeEach
  fun init() {
    handler = MockBspProcessHandler(project)
    listener = BspTestTaskListener(handler)
  }

  @Test
  fun `test-task`() {
    // given
    val expectedText = ServiceMessageBuilder("testingStarted").toString()
    val data = TestTask(Label.parse("id"))

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
    val testFinishData = TestFinish("testSuite", TestStatus.PASSED, data = JUnitStyleTestSuiteData(durationSeconds, null, null))

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
