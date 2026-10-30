package org.jetbrains.bazel.sync.environment

import com.intellij.openapi.project.ProjectManager
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.junit.jupiter.api.Test

@BazelTestApplication
internal class IJBazelProjectInfoServiceTest {
  @Test
  fun `should not throw exception on DefaultProject`() {
    val defaultProject = ProjectManager.getInstance().defaultProject
    defaultProject.isBazelProject shouldBe false
  }
}
