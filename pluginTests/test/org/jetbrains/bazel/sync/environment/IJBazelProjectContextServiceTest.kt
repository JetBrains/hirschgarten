package org.jetbrains.bazel.sync.environment

import com.intellij.openapi.project.ProjectManager
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.Test

internal class IJBazelProjectContextServiceTest : MockProjectBaseTest() {
  @Test
  fun `should not throw exception on DefaultProject`() {
    val defaultProject = ProjectManager.getInstance().defaultProject
    defaultProject.isBazelProject shouldBe false
  }
}
