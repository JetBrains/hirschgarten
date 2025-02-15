package org.jetbrains.plugins.bsp.sync

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import org.jetbrains.plugins.bsp.config.BuildToolId
import org.jetbrains.plugins.bsp.config.bspBuildToolId
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.sync.ProjectPostSyncHook
import org.jetbrains.plugins.bsp.sync.additionalProjectPostSyncHooks
import org.jetbrains.plugins.bsp.sync.defaultProjectPostSyncHooks
import org.jetbrains.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

private val testBuildToolId = BuildToolId("test-build-tool")

@DisplayName("ProjectPostSyncHook tests")
class ProjectPostSyncHookTest : MockProjectBaseTest() {
  @Nested
  @DisplayName("Project.defaultProjectPostSyncHooks tests")
  inner class DefaultProjectPostSyncHooks {
    @BeforeEach
    fun beforeEach() {
      // given
      project.buildToolId = testBuildToolId
      ProjectPostSyncHook.ep.registerExtension(
        _root_ide_package_.org.jetbrains.plugins.bsp.impl.flow.sync.TestProjectPostSyncHook(
          bspBuildToolId,
        ),
      )
    }

    @Test
    fun `should return all enabled default project post-sync hooks`() {
      // when & then
      ProjectPostSyncHook.ep.registerExtension(
        _root_ide_package_.org.jetbrains.plugins.bsp.impl.flow.sync.DisabledTestProjectPostSyncHook(
          bspBuildToolId,
        ),
      )

      project.defaultProjectPostSyncHooks.map { it::class.java } shouldContain
        _root_ide_package_.org.jetbrains.plugins.bsp.impl.flow.sync.TestProjectPostSyncHook::class.java
      project.defaultProjectPostSyncHooks.map { it::class.java } shouldNotContain
        _root_ide_package_.org.jetbrains.plugins.bsp.impl.flow.sync.DisabledTestProjectPostSyncHook::class.java
    }
  }

  @Nested
  @DisplayName("Project.additionalProjectPostSyncHooks tests")
  inner class AdditionalProjectPostSyncHooks {
    @Test
    fun `should return an empty list if imported as bsp (default) project`() {
      // given
      project.buildToolId = bspBuildToolId

      // when & then
      project.additionalProjectPostSyncHooks.map { it::class.java } shouldBe emptyList()
    }

    @Test
    fun `should return a list of hooks if imported as non-bsp project`() {
      // given
      project.buildToolId = testBuildToolId
      ProjectPostSyncHook.ep.registerExtension(
        _root_ide_package_.org.jetbrains.plugins.bsp.impl.flow.sync.TestProjectPostSyncHook(
          testBuildToolId,
        ),
      )
      ProjectPostSyncHook.ep.registerExtension(
        _root_ide_package_.org.jetbrains.plugins.bsp.impl.flow.sync.DisabledTestProjectPostSyncHook(
          testBuildToolId,
        ),
      )

      // when & then
      project.additionalProjectPostSyncHooks.map { it::class.java } shouldContain
        _root_ide_package_.org.jetbrains.plugins.bsp.impl.flow.sync.TestProjectPostSyncHook::class.java
      project.additionalProjectPostSyncHooks.map { it::class.java } shouldNotContain
        _root_ide_package_.org.jetbrains.plugins.bsp.impl.flow.sync.DisabledTestProjectPostSyncHook::class.java
    }
  }
}
