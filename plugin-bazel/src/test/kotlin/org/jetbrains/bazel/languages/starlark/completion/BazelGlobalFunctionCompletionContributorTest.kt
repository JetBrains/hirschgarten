package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BazelGlobalFunctionCompletionContributorTest : BasePlatformTestCase() {
  @Test
  fun `should complete extension functions`() {
    myFixture.configureByText("dummy.bzl", "")
    myFixture.type("x")

    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainExactlyInAnyOrder listOf("max", "exec_group", "module_extension", "exec_transition")
  }

  @Test
  fun `should complete build functions`() {
    myFixture.configureByText("BUILD", "")
    myFixture.type("x")

    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainAll listOf("max", "existing_rule", "existing_rules", "exports_files")
  }

  @Test
  fun `should complete workspace functions`() {
    myFixture.configureByText("WORKSPACE", "")
    myFixture.type("x")

    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainExactlyInAnyOrder listOf("max", "register_execution_platforms", "use_extension")
  }

  @Test
  fun `should complete module functions`() {
    myFixture.configureByText("MODULE.bazel", "")
    myFixture.type("x")

    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainExactlyInAnyOrder listOf("max", "register_execution_platforms", "use_extension")
  }
}
