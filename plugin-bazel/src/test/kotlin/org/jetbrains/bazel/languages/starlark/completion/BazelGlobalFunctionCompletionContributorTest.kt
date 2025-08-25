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
    // given
    myFixture.configureByText("dummy.bzl", "")
    myFixture.type("x")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainExactlyInAnyOrder listOf("max", "exec_group", "module_extension", "exec_transition")
  }

  @Test
  fun `should complete build functions`() {
    // given
    myFixture.configureByText("BUILD", "")
    myFixture.type("x")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainAll listOf("max", "existing_rule", "existing_rules", "exports_files")
  }

  @Test
  fun `should complete workspace functions`() {
    // given
    myFixture.configureByText("WORKSPACE", "")
    myFixture.type("x")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainExactlyInAnyOrder listOf("max", "register_execution_platforms", "use_extension")
  }

  @Test
  fun `should complete module functions`() {
    // given
    myFixture.configureByText("MODULE.bazel", "")
    myFixture.type("x")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainExactlyInAnyOrder listOf("max", "register_execution_platforms", "use_extension")
  }
}
