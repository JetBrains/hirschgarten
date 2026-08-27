package org.jetbrains.bazel.buildTask

import com.intellij.task.ProjectTaskManager
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelProjectFixture
import org.jetbrains.concurrency.await
import org.junit.jupiter.api.Test

@BazelTestApplication
internal class BazelProjectTaskRunnerTest {
  private val project by bazelProjectFixture("redcodes/go_embed")

  @Test
  fun `test that building go project works`() = runBlocking(Dispatchers.Default) {
    val buildResult = ProjectTaskManager.getInstance(project).rebuildAllModules().await()
    buildResult.hasErrors() shouldBe false
    buildResult.isAborted shouldBe false
  }
}
