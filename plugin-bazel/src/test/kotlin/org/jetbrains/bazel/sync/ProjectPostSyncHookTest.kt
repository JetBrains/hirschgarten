package org.jetbrains.bazel.sync

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import org.jetbrains.bazel.impl.flow.sync.DisabledTestProjectPostSyncHook
import org.jetbrains.bazel.impl.flow.sync.TestProjectPostSyncHook
import org.jetbrains.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ProjectPostSyncHook tests")
class ProjectPostSyncHookTest : MockProjectBaseTest() {
  @Nested
  @DisplayName("Project.defaultProjectPostSyncHooks tests")
  inner class DefaultProjectPostSyncHooks {
    @BeforeEach
    fun beforeEach() {
      // given
      ProjectPostSyncHook.ep.registerExtension(
        TestProjectPostSyncHook(),
      )
    }

    @Test
    fun `should return all enabled default project post-sync hooks`() {
      // when & then
      ProjectPostSyncHook.ep.registerExtension(
        DisabledTestProjectPostSyncHook(),
      )

      project.defaultProjectPostSyncHooks.map { it::class.java } shouldContain
        TestProjectPostSyncHook::class.java
      project.defaultProjectPostSyncHooks.map { it::class.java } shouldNotContain
        DisabledTestProjectPostSyncHook::class.java
    }
  }
}
