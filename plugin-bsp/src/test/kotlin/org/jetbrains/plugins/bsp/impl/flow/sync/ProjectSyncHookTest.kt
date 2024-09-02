package org.jetbrains.plugins.bsp.impl.flow.sync

import com.intellij.openapi.project.Project
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import org.jetbrains.plugins.bsp.config.BuildToolId
import org.jetbrains.plugins.bsp.config.bspBuildToolId
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.impl.flow.sync.ProjectSyncHook.ProjectSyncHookEnvironment
import org.jetbrains.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

private val testBuildToolId = BuildToolId("test-build-tool")

private class TestProjectSyncHook(override val buildToolId: BuildToolId) : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHookEnvironment) {
    // nothing to do, it's a test
  }
}

private class TestDefaultProjectSyncDisabler(private val toDisable: List<Class<out ProjectSyncHook>>) : DefaultProjectSyncHooksDisabler {
  override val buildToolId: BuildToolId = testBuildToolId

  override fun disabledProjectSyncHooks(project: Project): List<Class<out ProjectSyncHook>> = toDisable
}

@DisplayName("ProjectSyncHook tests")
class ProjectSyncHookTest : MockProjectBaseTest() {
  @Nested
  @DisplayName("Project.defaultProjectSyncHooks tests")
  inner class DefaultProjectSyncHooks {
    @BeforeEach
    fun beforeEach() {
      // given
      project.buildToolId = testBuildToolId
      ProjectSyncHook.ep.point.registerExtension(TestProjectSyncHook(bspBuildToolId), projectModel.disposableRule.disposable)
    }

    @Test
    fun `should return all the default project sync hooks if no disabler is defined`() {
      // when & then
      project.defaultProjectSyncHooks.map { it::class.java } shouldContain TestProjectSyncHook::class.java
    }

    @Test
    fun `should return all the default project sync hooks if disabler doesnt disable it`() {
      // given
      DefaultProjectSyncHooksDisabler.ep.point.registerExtension(
        TestDefaultProjectSyncDisabler(emptyList()),
        projectModel.disposableRule.disposable,
      )

      // when & then
      project.defaultProjectSyncHooks.map { it::class.java } shouldContain TestProjectSyncHook::class.java
    }

    @Test
    fun `should return filtered default project sync hooks`() {
      // given
      DefaultProjectSyncHooksDisabler.ep.point.registerExtension(
        TestDefaultProjectSyncDisabler(listOf(TestProjectSyncHook::class.java)),
        projectModel.disposableRule.disposable,
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
      ProjectSyncHook.ep.point.registerExtension(TestProjectSyncHook(testBuildToolId), projectModel.disposableRule.disposable)

      // when & then
      project.additionalProjectSyncHooks.map { it::class.java } shouldContain TestProjectSyncHook::class.java
    }
  }
}
