package org.jetbrains.bazel.run.task

import com.intellij.execution.testframework.sm.ServiceMessageBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.testFramework.junit5.TestApplication
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.BazelProcessHandler
import org.jetbrains.bazel.workspace.model.matchers.shouldBeEqual
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.jetbrains.bsp.protocol.JUnitStyleTestSuiteData
import org.jetbrains.bsp.protocol.TaskGroupId
import org.jetbrains.bsp.protocol.TestFinish
import org.jetbrains.bsp.protocol.TestStart
import org.jetbrains.bsp.protocol.TestStatus
import org.jetbrains.bsp.protocol.TestTask
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class MockBazelProcessHandler(project: Project) : BazelProcessHandler(project, CompletableDeferred(0)) {
  var latestText: String = ""

  override fun notifyTextAvailable(text: String, outputType: Key<*>) {
    latestText = text
  }

  override fun startNotify() {}

  override fun destroyProcessImpl() {}

  override fun detachProcessImpl() {}
}

@TestApplication
class BspTestTaskListenerTest : WorkspaceModelBaseTest() {
  private lateinit var handler: MockBazelProcessHandler
  private lateinit var listener: BazelTestTaskListener

  @BeforeEach
  fun init() {
    handler = MockBazelProcessHandler(project)
    listener = BazelTestTaskListener(handler)
  }

  @Test
  fun `test-task`() {
    // given
    val expectedText = ServiceMessageBuilder("testingStarted").toString() + "\n"
    val data = TestTask(Label.parse("id"))

    // when
    listener.onTaskStart(taskId = TaskGroupId.EMPTY.task("task-id"), message = "", data = data)

    // then
    handler.latestText shouldBeEqual expectedText
  }

  @Test
  fun `task-start with test suite`() {
    // given
    val taskId = TaskGroupId.EMPTY.task("task-id")
    val data = TestStart("testSuite", true,"test://testSuite")
    val expectedText =
      ServiceMessageBuilder
        .testSuiteStarted(
          data.displayName,
        ).addAttribute("nodeId", taskId.id)
        .addAttribute("parentNodeId", "0")
        .addAttribute("locationHint", data.locationHint)
        .toString() + "\n"

    // when
    listener.onTaskStart(taskId = taskId, message = "", data = data)

    // then
    handler.latestText shouldBeEqual expectedText
  }

  @Test
  fun `task-start with test case`() {
    // given
    val parentId = TaskGroupId.EMPTY.task("parent-id")
    val taskId = parentId.subTask("task-id")
    val data = TestStart("testCase", false,"test://testCase")
    val expectedText =
      ServiceMessageBuilder
        .testStarted(
          data.displayName,
        ).addAttribute("nodeId", taskId.id)
        .addAttribute("parentNodeId", "0")
        .addAttribute("locationHint", data.locationHint)
        .toString() + "\n"

    // when
    listener.onTaskStart(taskId = taskId, message = "", data = data)

    // then
    handler.latestText shouldBeEqual expectedText
  }

  @Test
  fun `task-finish with test suite`() {
    // given
    val durationSeconds = 1.23456
    val taskId = TaskGroupId.EMPTY.task("task-id")
    val testStartData = TestStart("testSuite", true, "test://testSuite")
    val testFinishData = TestFinish("testSuite", TestStatus.PASSED, data = JUnitStyleTestSuiteData(durationSeconds, null, null))

    val expectedDurationMillis = (1234).toLong()
    val expectedText =
      ServiceMessageBuilder
        .testSuiteFinished(testFinishData.displayName)
        .addAttribute("nodeId", taskId.id)
        .addAttribute("duration", expectedDurationMillis.toString())
        .toString() + "\n"

    // when
    listener.onTaskStart(taskId = taskId, message = "", data = testStartData)
    listener.onTaskFinish(taskId = taskId, message = "", data = testFinishData, status = BazelStatus.SUCCESS)

    // then
    handler.latestText shouldBeEqual expectedText
  }
}
