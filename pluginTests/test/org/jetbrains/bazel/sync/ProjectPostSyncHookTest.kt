package org.jetbrains.bazel.sync

import com.intellij.openapi.project.Project
import com.intellij.platform.util.progress.reportSequentialProgress
import com.intellij.testFramework.ExtensionTestUtil
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.collections.shouldNotContain
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.golang.sync.GoBuildTarget
import org.jetbrains.bazel.impl.flow.sync.DisabledTestProjectPostSyncHook
import org.jetbrains.bazel.impl.flow.sync.TestProjectPostSyncHook
import org.jetbrains.bazel.languages.bazelversion.psi.BazelVersionLiteral
import org.jetbrains.bazel.languages.bazelversion.service.BazelVersionResolver
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.testproject.buildServer
import org.jetbrains.bazel.test.framework.testproject.buildTarget
import org.jetbrains.bazel.test.framework.testproject.testBazelProjectFixture
import org.jetbrains.bazel.test.framework.testproject.sync
import org.jetbrains.bazel.test.framework.testproject.workspaceLocations
import org.jetbrains.bazel.test.framework.target.TestBuildTarget
import org.jetbrains.bsp.protocol.TaskGroupId
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@BazelTestApplication
@DisplayName("ProjectPostSyncHook tests")
class ProjectPostSyncHookTest {
  private val testBazel by testBazelProjectFixture()

  @Test
  fun `should return all enabled project post-sync hooks`() {
    testBazel.buildServer()

    // given
    ProjectPostSyncHook.ep.point.registerExtension(TestProjectPostSyncHook(), testBazel.disposable)
    ProjectPostSyncHook.ep.point.registerExtension(DisabledTestProjectPostSyncHook(), testBazel.disposable)

    // when & then
    testBazel.project.projectPostSyncHooks.map { it::class.java } shouldContain TestProjectPostSyncHook::class.java
    testBazel.project.projectPostSyncHooks.map { it::class.java } shouldNotContain DisabledTestProjectPostSyncHook::class.java
  }

  @Test
  fun `registered project post-sync hooks should tolerate unchanged project model`(): Unit = runBlocking {
    testBazel.buildServer()
    ExtensionTestUtil.maskExtensions(BazelVersionResolver.ep, listOf(TestBazelVersionResolver()), testBazel.disposable)
    testBazel.sync { targets(staleGoTarget()) }

    reportSequentialProgress { progressReporter ->
      val environment =
        ProjectPostSyncHook.ProjectPostSyncHookEnvironment(
          project = testBazel.project,
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

  private fun staleGoTarget(): TestBuildTarget {
    val kind = TargetKind(kind = "go_library", ruleType = RuleType.LIBRARY, languageClasses = setOf(LanguageClass("go", setOf("go"))))
    return buildTarget("//app:go_default_library", kind) {
      srcs("app/main.go")
      data(GoBuildTarget(importPath = "example.com/app", sources = workspaceLocations("app/main.go"), embed = emptyList()))
    }
  }

  private fun ProjectPostSyncHook.isEnabledForTest(): Boolean =
    try {
      isEnabled(testBazel.project)
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
