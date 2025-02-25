package org.jetbrains.bazel.sync

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import org.jetbrains.bazel.impl.flow.sync.DisabledTestProjectSyncHook
import org.jetbrains.bazel.impl.flow.sync.TestProjectSyncHook
import org.jetbrains.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ProjectSyncHook tests")
class ProjectSyncHookTest : MockProjectBaseTest() {
  @Nested
  @DisplayName("Project.projectSyncHooks tests")
  inner class ProjectSyncHooks {
    @Test
    fun `should return all enabled project sync hooks`() {
      // given
      ProjectSyncHook.ep.registerExtension(TestProjectSyncHook())
      ProjectSyncHook.ep.registerExtension(DisabledTestProjectSyncHook())

      // when & then
      project.projectSyncHooks.map { it::class.java } shouldContain TestProjectSyncHook::class.java
      project.projectSyncHooks.map { it::class.java } shouldNotContain DisabledTestProjectSyncHook::class.java
    }
  }
}
