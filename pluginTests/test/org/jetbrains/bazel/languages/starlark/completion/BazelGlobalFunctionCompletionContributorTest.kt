package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.testFramework.runInEdtAndWait
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.jetbrains.bazel.test.framework.completeBasicLookupStrings
import org.junit.jupiter.api.Test

@BazelTestApplication
class BazelGlobalFunctionCompletionContributorTest {
  private val myFixture by bazelCodeInsightFixture(release = BazelRelease(9, 1))

  @Test
  fun `should complete extension functions`() {
    myFixture.configureByText("dummy.bzl", "")
    myFixture.type("x")

    val lookups = myFixture.completeBasicLookupStrings()

    lookups shouldContainExactlyInAnyOrder listOf("max", "exec_group", "module_extension", "exec_transition", "ktlint_fix")
  }

  @Test
  fun `should complete build functions`() {
    myFixture.configureByText("BUILD", "")
    myFixture.type("x")

    val lookups = myFixture.completeBasicLookupStrings()

    lookups shouldContainAll listOf("max", "existing_rule", "existing_rules", "exports_files")
  }

  @Test
  fun `should complete value returning declarations used as argument value`() {
    myFixture.configureByText("BUILD", "filegroup(srcs = ex<caret>)")
    val lookups = myFixture.completeBasicLookupStrings()
    lookups shouldContainExactlyInAnyOrder listOf("existing_rule", "existing_rules")
  }

  @Test
  fun `should complete value producing functions as argument value in bzl files`() {
    myFixture.configureByText("ext.bzl", "x = foo(di<caret>)")
    val lookups = myFixture.completeBasicLookupStrings()
    lookups shouldContainExactlyInAnyOrder listOf("dict", "dir")
  }

  @Test
  fun `should complete void returning declaration in a statement position inside an if body`() {
    myFixture.configureByText("ext.bzl", "if True:\n    pr<caret>")
    val lookups = myFixture.completeBasicLookupStrings()
    lookups shouldContainExactlyInAnyOrder listOf("print", "provider", "repr")
  }

  @Test
  fun `should not complete void returning functions used as argument value`() {
    myFixture.configureByText("ext.bzl", "x = foo(prin<caret>)")
    val lookups = myFixture.completeBasicLookupStrings()
    lookups.shouldBeEmpty()
  }

  @Test
  fun `should not complete none returning declarations used as argument value`() {
    myFixture.configureByText("BUILD", "filegroup(srcs = py_<caret>)")
    val lookups = myFixture.completeBasicLookupStrings()
    lookups.shouldBeEmpty()
  }

  @Test
  fun `should complete declarations at top level`() {
    myFixture.configureByText("BUILD", "")
    myFixture.type("py_")
    val lookups = myFixture.completeBasicLookupStrings()
    lookups shouldContainExactlyInAnyOrder listOf("py_binary", "py_library", "py_proto_library", "py_runtime", "py_test")
  }

  @Test
  fun `should complete void returning declarations when parentheses already present`() {
    myFixture.configureByText("BUILD", "py_<caret>()")
    val lookups = myFixture.completeBasicLookupStrings()
    lookups shouldContainExactlyInAnyOrder listOf("py_binary", "py_library", "py_proto_library", "py_runtime", "py_test")
  }

  @Test
  fun `should complete workspace functions`() {
    myFixture.configureByText("WORKSPACE", "")
    myFixture.type("x")

    val lookups = myFixture.completeBasicLookupStrings()

    lookups shouldContainExactlyInAnyOrder listOf("max", "register_execution_platforms", "use_extension")
  }

  @Test
  fun `should complete module functions`() {
    myFixture.configureByText("MODULE.bazel", "")
    myFixture.type("x")

    val lookups = myFixture.completeBasicLookupStrings()

    lookups shouldContainExactlyInAnyOrder listOf("max", "register_execution_platforms", "use_extension")
  }

  @Test
  fun `should insert a list literal when completing selected functions with list literal of strings as first argument`() {
    for (funName in listOf("glob", "exports_files", "licenses")) {
      assertListLiteralInserted(
        fileName = "BUILD",
        text = "$funName<caret>",
        lookupString = funName,
        expected = "$funName([\"<caret>\"])",
      )
    }
  }

  @Test
  fun `should place caret inside existing parentheses instead of inserting a second list literal`() {
    assertListLiteralInserted(
      fileName = "BUILD",
      text = "glo<caret>()",
      lookupString = "glo",
      expected = "glob(<caret>)",
    )
  }

  private fun assertListLiteralInserted(fileName: String, text: String, lookupString: String, expected: String) {
    myFixture.configureByText(fileName, text)
    val lookups = myFixture.completeBasic()
    if (lookups != null) {
      runInEdtAndWait { myFixture.lookup.currentItem = lookups.first { lookupString in it.allLookupStrings } }
      myFixture.type('\n')
    }
    myFixture.checkResult(expected)
  }
}
