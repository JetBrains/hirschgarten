package org.jetbrains.bazel.sync

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import org.jetbrains.bazel.impl.flow.sync.DisabledTestProjectPreSyncHook
import org.jetbrains.bazel.impl.flow.sync.TestProjectPreSyncHook
import org.jetbrains.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ProjectPreSyncHook tests")
class ProjectPreSyncHookTest : MockProjectBaseTest() {
  @Nested
  @DisplayName("Project.defaultProjectPreSyncHooks tests")
  inner class DefaultProjectPreSyncHooks {
    @BeforeEach
    fun beforeEach() {
      // given
      ProjectPreSyncHook.ep.registerExtension(
        TestProjectPreSyncHook(),
      )
    }

    @Test
    fun `should return all enabled default project pre-sync hooks`() {
      // when & then
      ProjectPreSyncHook.ep.registerExtension(
        DisabledTestProjectPreSyncHook(),
      )

      project.defaultProjectPreSyncHooks.map { it::class.java } shouldContain
        TestProjectPreSyncHook::class.java
      project.defaultProjectPreSyncHooks.map { it::class.java } shouldNotContain
        DisabledTestProjectPreSyncHook::class.java
    }
  }
}
