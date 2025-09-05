package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.openapi.project.Project
import com.intellij.testFramework.ExtensionTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.languages.starlark.bazel.modules.BazelModuleResolver
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BazelDepModuleCompletionTest : BasePlatformTestCase() {
  @Before
  fun beforeEach() {
    project.isBazelProject = true
  }

  private val provider =
    object : BazelModuleResolver {
      override val id = "test_id"
      override val name = "test_name"

      override suspend fun getModuleNames(project: Project): List<String> = emptyList()

      override fun getCachedModuleNames(project: Project): List<String> = listOf("module_name")

      override suspend fun getModuleVersions(project: Project, moduleName: String): List<String> = emptyList()

      override fun getCachedModuleVersions(project: Project, moduleName: String): List<String> =
        if (moduleName == "module_name") listOf("1.0.0", "0.1.0")
        else emptyList()

      override suspend fun refreshModuleNames(project: Project) {}

      override fun clearCache(project: Project) {}
    }

  @Test
  fun `should suggest module name in bazel_dep name attribute`() {
    ExtensionTestUtil.maskExtensions(BazelModuleResolver.EP_NAME, listOf(provider), testRootDisposable)

    myFixture.configureByText(
      "BUILD",
      """
      bazel_dep(
        name = "<caret>",
      )
      """.trimMargin(),
    )

    val lookups = myFixture.completeBasic().map { it.lookupString }

    lookups.shouldContain("\"module_name\"")
  }

  @Test
  fun `should suggest module versions in bazel_dep version attribute`() {
    ExtensionTestUtil.maskExtensions(BazelModuleResolver.EP_NAME, listOf(provider), testRootDisposable)

    myFixture.configureByText(
      "BUILD",
      """
      bazel_dep(
        name = "module_name",
        version = "<caret>",
      )
      """.trimMargin(),
    )

    val lookups = myFixture.completeBasic().map { it.lookupString }

    lookups.shouldContainExactly(listOf("\"1.0.0\"", "\"0.1.0\""))
  }

  @Test
  fun `should not suggest any version in bazel_dep version attribute if module name is incorrect`() {
    ExtensionTestUtil.maskExtensions(BazelModuleResolver.EP_NAME, listOf(provider), testRootDisposable)

    myFixture.configureByText(
      "BUILD",
      """
      bazel_dep(
        name = "other_module_name",
        version = "<caret>",
      )
      """.trimMargin(),
    )

    val lookups = myFixture.completeBasic().map { it.lookupString }

    lookups.shouldBeEmpty()
  }
}
