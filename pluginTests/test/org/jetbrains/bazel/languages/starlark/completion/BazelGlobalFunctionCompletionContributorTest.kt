package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.collections.shouldBeEmpty
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

    lookups shouldContainExactlyInAnyOrder listOf("max", "exec_group", "module_extension", "exec_transition", "ktlint_fix")
  }

  @Test
  fun `should complete build functions`() {
    myFixture.configureByText("BUILD", "")
    myFixture.type("x")

    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainAll listOf("max", "existing_rule", "existing_rules", "exports_files")
  }

  @Test
  fun `should complete value returning declarations used as argument value`() {
    myFixture.configureByText("BUILD", "filegroup(srcs = ex<caret>)")
    val lookups = myFixture.completeBasic().orEmpty().flatMap { it.allLookupStrings }
    lookups shouldContainExactlyInAnyOrder listOf("existing_rule", "existing_rules")
  }

  @Test
  fun `should complete value producing functions as argument value in bzl files`() {
    myFixture.configureByText("ext.bzl", "x = foo(di<caret>)")
    val lookups = myFixture.completeBasic().orEmpty().flatMap { it.allLookupStrings }
    lookups shouldContainExactlyInAnyOrder listOf("dict", "dir")
  }

  @Test
  fun `should complete void returning declaration in a statement position inside an if body`() {
    myFixture.configureByText("ext.bzl", "if True:\n    pr<caret>")
    val lookups = myFixture.completeBasic().orEmpty().flatMap { it.allLookupStrings }
    lookups shouldContainExactlyInAnyOrder listOf("print", "provider", "repr")
  }

  @Test
  fun `should not complete void returning functions used as argument value`() {
    myFixture.configureByText("ext.bzl", "x = foo(prin<caret>)")
    val lookups = myFixture.completeBasic().orEmpty().flatMap { it.allLookupStrings }
    lookups.shouldBeEmpty()
  }

  @Test
  fun `should not complete none returning declarations used as argument value`() {
    myFixture.configureByText("BUILD", "filegroup(srcs = py_<caret>)")
    val lookups = myFixture.completeBasic().orEmpty().flatMap { it.allLookupStrings }
    lookups.shouldBeEmpty()
  }

  @Test
  fun `should complete declarations at top level`() {
    myFixture.configureByText("BUILD", "")
    myFixture.type("py_")
    val lookups = myFixture.completeBasic().orEmpty().flatMap { it.allLookupStrings }
    lookups shouldContainExactlyInAnyOrder listOf("py_binary", "py_library", "py_proto_library", "py_runtime", "py_test")
  }

  @Test
  fun `should complete void returning declarations when parentheses already present`() {
    myFixture.configureByText("BUILD", "py_<caret>()")
    val lookups = myFixture.completeBasic().orEmpty().flatMap { it.allLookupStrings }
    lookups shouldContainExactlyInAnyOrder listOf("py_binary", "py_library", "py_proto_library", "py_runtime", "py_test")
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
