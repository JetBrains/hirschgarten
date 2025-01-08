package org.jetbrains.plugins.bsp.impl.flow.sync

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import org.jetbrains.plugins.bsp.config.BuildToolId
import org.jetbrains.plugins.bsp.config.bspBuildToolId
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

private val testBuildToolId = BuildToolId("test-build-tool")

@DisplayName("ProjectSyncHook tests")
class ProjectSyncHookTest : MockProjectBaseTest() {
  @Nested
  @DisplayName("Project.defaultProjectSyncHooks tests")
  inner class DefaultProjectSyncHooks {
    @BeforeEach
    fun beforeEach() {
      // given
      project.buildToolId = testBuildToolId
      ProjectSyncHook.ep.registerExtension(TestProjectSyncHook(bspBuildToolId))
    }

    @Test
    fun `should return all enabled default project sync hooks if no disabler is defined`() {
      // when & then
      ProjectSyncHook.ep.registerExtension(DisabledTestProjectSyncHook(bspBuildToolId))

      project.defaultProjectSyncHooks.map { it::class.java } shouldContain TestProjectSyncHook::class.java
      project.defaultProjectSyncHooks.map { it::class.java } shouldNotContain DisabledTestProjectSyncHook::class.java
    }

    @Test
    fun `should return all the default project sync hooks if disabler doesnt disable it`() {
      // given
      DefaultProjectSyncHooksDisabler.ep.registerExtension(TestDefaultProjectSyncDisabler(testBuildToolId, emptyList()))

      // when & then
      project.defaultProjectSyncHooks.map { it::class.java } shouldContain TestProjectSyncHook::class.java
    }

    @Test
    fun `should return filtered default project sync hooks`() {
      // given
      DefaultProjectSyncHooksDisabler.ep.registerExtension(
        TestDefaultProjectSyncDisabler(testBuildToolId, listOf(TestProjectSyncHook::class.java)),
      )

      // when & then
      project.defaultProjectSyncHooks.map { it::class.java } shouldNotContain TestProjectSyncHook::class.java
    }
  }

  @Nested
  @DisplayName("Project.additionalProjectSyncHooks tests")
  inner class AdditionalProjectSyncHooks {
    @Test
    fun `should return an empty list if imported as bsp (default) project`() {
      // given
      project.buildToolId = bspBuildToolId

      // when & then
      project.additionalProjectSyncHooks.map { it::class.java } shouldBe emptyList()
    }

    @Test
    fun `should return a list of hooks if imported as non-bsp project`() {
      // given
      project.buildToolId = testBuildToolId
      ProjectSyncHook.ep.registerExtension(TestProjectSyncHook(testBuildToolId))
      ProjectSyncHook.ep.registerExtension(DisabledTestProjectSyncHook(testBuildToolId))

      // when & then
      project.additionalProjectSyncHooks.map { it::class.java } shouldContain TestProjectSyncHook::class.java
      project.additionalProjectSyncHooks.map { it::class.java } shouldNotContain DisabledTestProjectSyncHook::class.java
    }
  }
}
