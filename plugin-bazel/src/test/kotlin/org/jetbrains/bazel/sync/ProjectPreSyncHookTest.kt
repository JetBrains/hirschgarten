package org.jetbrains.bazel.sync

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import org.jetbrains.bazel.impl.flow.sync.DisabledTestProjectPreSyncHook
import org.jetbrains.bazel.impl.flow.sync.TestProjectPreSyncHook
import org.jetbrains.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ProjectPreSyncHook tests")
class ProjectPreSyncHookTest : MockProjectBaseTest() {
  @Nested
  @DisplayName("Project.projectPreSyncHooks tests")
  inner class ProjectPreSyncHooks {
    @Test
    fun `should return all enabled project pre-sync hooks`() {
      // given
      ProjectPreSyncHook.ep.registerExtension(TestProjectPreSyncHook())
      ProjectPreSyncHook.ep.registerExtension(DisabledTestProjectPreSyncHook())

      // when & then
      project.projectPreSyncHooks.map { it::class.java } shouldContain TestProjectPreSyncHook::class.java
      project.projectPreSyncHooks.map { it::class.java } shouldNotContain DisabledTestProjectPreSyncHook::class.java
    }
  }
}
