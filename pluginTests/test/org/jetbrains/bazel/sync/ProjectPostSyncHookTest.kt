package org.jetbrains.bazel.sync

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.use
import com.intellij.platform.util.progress.reportSequentialProgress
import com.intellij.testFramework.ExtensionTestUtil
import com.intellij.testFramework.junit5.SystemProperty
import com.intellij.testFramework.registerOrReplaceServiceInstance
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.impl.flow.sync.DisabledTestProjectPostSyncHook
import org.jetbrains.bazel.impl.flow.sync.TestProjectPostSyncHook
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.bazelversion.psi.BazelVersionLiteral
import org.jetbrains.bazel.languages.bazelversion.service.BazelVersionResolver
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.jetbrains.bazel.server.BazelServerService
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.workspace.model.test.framework.BuildServerMock
import org.jetbrains.bazel.workspace.model.test.framework.createRawBuildTarget
import org.jetbrains.bazel.workspace.model.test.framework.MockBuildServerService
import org.jetbrains.bazel.workspace.model.test.framework.MockProjectBaseTest
import org.jetbrains.bazel.golang.sync.GoBuildTarget
import org.jetbrains.bsp.protocol.TaskGroupId
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ProjectPostSyncHook tests")
class ProjectPostSyncHookTest : MockProjectBaseTest() {
  @Test
  fun `should return all enabled project post-sync hooks`(): Unit = Disposer.newDisposable().use { disposable ->
    project.registerOrReplaceServiceInstance(BazelServerService::class.java, MockBuildServerService(BuildServerMock()), disposable)

    // given
    ProjectPostSyncHook.ep.registerExtension(TestProjectPostSyncHook())
    ProjectPostSyncHook.ep.registerExtension(DisabledTestProjectPostSyncHook())

    // when & then
    project.projectPostSyncHooks.map { it::class.java } shouldContain TestProjectPostSyncHook::class.java
    project.projectPostSyncHooks.map { it::class.java } shouldNotContain DisabledTestProjectPostSyncHook::class.java
  }

  @Test
  @SystemProperty(propertyKey = BazelFeatureFlags.GO_SUPPORT, propertyValue = "true")
  fun `registered project post-sync hooks should tolerate unchanged project model`(): Unit = runBlocking {
    initializeBazelProject(project, projectDir.get())
    project.registerOrReplaceServiceInstance(BazelServerService::class.java, MockBuildServerService(BuildServerMock()), disposable)
    ExtensionTestUtil.maskExtensions(BazelVersionResolver.ep, listOf(TestBazelVersionResolver()), disposable)
    seedStaleGoTarget()

    reportSequentialProgress { progressReporter ->
      val environment =
        ProjectPostSyncHook.ProjectPostSyncHookEnvironment(
          project = project,
          taskId = TaskGroupId("test-post-sync-hooks").task("project-sync"),
          progressReporter = progressReporter,
          projectModelUpdated = false,
        )
      val hooks = ProjectPostSyncHook.ep.extensionList
      hooks.shouldNotBeEmpty()
      val enabledHooks = hooks.filter { hook -> hook.isEnabledForTest() }
      enabledHooks.shouldNotBeEmpty()

      enabledHooks.forEach { hook ->
        try {
          hook.onPostSync(environment)
        }
        catch (e: Throwable) {
          throw AssertionError("Post-sync hook ${hook::class.java.name} failed when projectModelUpdated=false", e)
        }
      }
    }
  }

  private fun seedStaleGoTarget() {
    val source = projectDir.get().resolve("app/main.go")
    val target =
      createRawBuildTarget(
        id = Label.parse("//app:go_default_library"),
        kind = TargetKind(
          kind = "go_library",
          ruleType = RuleType.LIBRARY,
          languageClasses = setOf(LanguageClass("go", setOf("go"))),
        ),
        sources = listOf(source),
        baseDirectory = projectDir.get(),
        data = listOf(
          GoBuildTarget(
            importPath = "example.com/app",
            sources = listOf(source),
            libraryLabels = emptyList(),
          ),
        ),
      )
    project.targetUtils.setTargets(listOf(target))
  }

  private fun ProjectPostSyncHook.isEnabledForTest(): Boolean =
    try {
      isEnabled(project)
    }
    catch (e: Throwable) {
      throw AssertionError("Post-sync hook ${this::class.java.name} failed in isEnabled", e)
    }
}

private class TestBazelVersionResolver : BazelVersionResolver {
  override val id: String = "test"
  override val name: String = "Test"

  override suspend fun resolveLatestBazelVersion(project: Project, currentVersion: BazelVersionLiteral?): BazelVersionLiteral? =
    currentVersion
}
