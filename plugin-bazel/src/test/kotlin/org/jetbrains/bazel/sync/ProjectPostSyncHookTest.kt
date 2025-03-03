package org.jetbrains.bazel.sync

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import org.jetbrains.bazel.impl.flow.sync.DisabledTestProjectPostSyncHook
import org.jetbrains.bazel.impl.flow.sync.TestProjectPostSyncHook
import org.jetbrains.bazel.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ProjectPostSyncHook tests")
class ProjectPostSyncHookTest : MockProjectBaseTest() {
  @Nested
  @DisplayName("Project.projectPostSyncHooks tests")
  inner class ProjectPostSyncHooks {
    @Test
    fun `should return all enabled project post-sync hooks`() {
      // given
      ProjectPostSyncHook.ep.registerExtension(TestProjectPostSyncHook())
      ProjectPostSyncHook.ep.registerExtension(DisabledTestProjectPostSyncHook())

      // when & then
      project.projectPostSyncHooks.map { it::class.java } shouldContain TestProjectPostSyncHook::class.java
      project.projectPostSyncHooks.map { it::class.java } shouldNotContain DisabledTestProjectPostSyncHook::class.java
    }
  }
}
