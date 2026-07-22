package org.jetbrains.bazel.buildTask

import com.intellij.task.ProjectTaskManager
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.enableGoHighlighting
import org.jetbrains.concurrency.await
import org.junit.jupiter.api.Test

@BazelTestApplication
internal class BazelProjectTaskRunnerTest {
  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDir = tempPathFixture()
  private val fixture by bazelSyncCodeInsightFixture(projectFixture, tempDir)

  @Test
  fun `test that building go project works`() = runBlocking(Dispatchers.Default) {
    fixture.enableGoHighlighting()
    fixture.copyBazelTestProject("redcodes/go_embed")
    fixture.performBazelSync()

    val buildResult = ProjectTaskManager.getInstance(fixture.project).rebuildAllModules().await()
    buildResult.hasErrors() shouldBe false
    buildResult.isAborted shouldBe false
  }
}
