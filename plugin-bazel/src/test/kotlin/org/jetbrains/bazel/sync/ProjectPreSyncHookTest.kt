package org.jetbrains.bazel.sync

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.config.BuildToolId
import org.jetbrains.bazel.config.bspBuildToolId
import org.jetbrains.bazel.config.buildToolId
import org.jetbrains.bazel.sync.ProjectPreSyncHook
import org.jetbrains.bazel.sync.additionalProjectPreSyncHooks
import org.jetbrains.bazel.sync.defaultProjectPreSyncHooks
import org.jetbrains.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

private val testBuildToolId = BuildToolId("test-build-tool")

@DisplayName("ProjectPreSyncHook tests")
class ProjectPreSyncHookTest : MockProjectBaseTest() {
  @Nested
  @DisplayName("Project.defaultProjectPreSyncHooks tests")
  inner class DefaultProjectPreSyncHooks {
    @BeforeEach
    fun beforeEach() {
      // given
      project.buildToolId = testBuildToolId
      ProjectPreSyncHook.ep.registerExtension(
        _root_ide_package_.org.jetbrains.bazel.impl.flow.sync.TestProjectPreSyncHook(
          bspBuildToolId,
        ),
      )
    }

    @Test
    fun `should return all enabled default project pre-sync hooks`() {
      // when & then
      ProjectPreSyncHook.ep.registerExtension(
        _root_ide_package_.org.jetbrains.bazel.impl.flow.sync.DisabledTestProjectPreSyncHook(
          bspBuildToolId,
        ),
      )

      project.defaultProjectPreSyncHooks.map { it::class.java } shouldContain
        _root_ide_package_.org.jetbrains.bazel.impl.flow.sync.TestProjectPreSyncHook::class.java
      project.defaultProjectPreSyncHooks.map { it::class.java } shouldNotContain
        _root_ide_package_.org.jetbrains.bazel.impl.flow.sync.DisabledTestProjectPreSyncHook::class.java
    }
  }

  @Nested
  @DisplayName("Project.additionalProjectPreSyncHooks tests")
  inner class AdditionalProjectPreSyncHooks {
    @Test
    fun `should return an empty list if imported as bsp (default) project`() {
      // given
      project.buildToolId = bspBuildToolId

      // when & then
      project.additionalProjectPreSyncHooks.map { it::class.java } shouldBe emptyList()
    }

    @Test
    fun `should return a list of hooks if imported as non-bsp project`() {
      // given
      project.buildToolId = testBuildToolId
      ProjectPreSyncHook.ep.registerExtension(
        _root_ide_package_.org.jetbrains.bazel.impl.flow.sync.TestProjectPreSyncHook(
          testBuildToolId,
        ),
      )
      ProjectPreSyncHook.ep.registerExtension(
        _root_ide_package_.org.jetbrains.bazel.impl.flow.sync.DisabledTestProjectPreSyncHook(
          testBuildToolId,
        ),
      )

      // when & then
      project.additionalProjectPreSyncHooks.map { it::class.java } shouldContain
        _root_ide_package_.org.jetbrains.bazel.impl.flow.sync.TestProjectPreSyncHook::class.java
      project.additionalProjectPreSyncHooks.map { it::class.java } shouldNotContain
        _root_ide_package_.org.jetbrains.bazel.impl.flow.sync.DisabledTestProjectPreSyncHook::class.java
    }
  }
}
