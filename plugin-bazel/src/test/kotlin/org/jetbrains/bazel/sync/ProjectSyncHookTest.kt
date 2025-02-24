package org.jetbrains.bazel.sync

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import org.jetbrains.bazel.impl.flow.sync.DisabledTestProjectSyncHook
import org.jetbrains.bazel.impl.flow.sync.TestDefaultProjectSyncDisabler
import org.jetbrains.bazel.impl.flow.sync.TestProjectSyncHook
import org.jetbrains.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ProjectSyncHook tests")
class ProjectSyncHookTest : MockProjectBaseTest() {
  @Nested
  @DisplayName("Project.defaultProjectSyncHooks tests")
  inner class DefaultProjectSyncHooks {
    @BeforeEach
    fun beforeEach() {
      // given
      ProjectSyncHook.ep.registerExtension(
        TestProjectSyncHook(),
      )
    }

    @Test
    fun `should return all enabled default project sync hooks if no disabler is defined`() {
      // when & then
      ProjectSyncHook.ep.registerExtension(
        DisabledTestProjectSyncHook(),
      )

      project.defaultProjectSyncHooks.map { it::class.java } shouldContain
        TestProjectSyncHook::class.java
      project.defaultProjectSyncHooks.map { it::class.java } shouldNotContain
        DisabledTestProjectSyncHook::class.java
    }

    @Test
    fun `should return all the default project sync hooks if disabler doesnt disable it`() {
      // given
      DefaultProjectSyncHooksDisabler.ep.registerExtension(
        TestDefaultProjectSyncDisabler(
          emptyList(),
        ),
      )

      // when & then
      project.defaultProjectSyncHooks.map { it::class.java } shouldContain
        TestProjectSyncHook::class.java
    }

    @Test
    fun `should return filtered default project sync hooks`() {
      // given
      DefaultProjectSyncHooksDisabler.ep.registerExtension(
        TestDefaultProjectSyncDisabler(
          listOf(TestProjectSyncHook::class.java),
        ),
      )

      // when & then
      project.defaultProjectSyncHooks.map { it::class.java } shouldNotContain
        TestProjectSyncHook::class.java
    }
  }
}
