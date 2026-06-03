package org.jetbrains.bazel.languages.starlark.findusages

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression
import org.jetbrains.bazel.project.BazelProjectFixtures
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkParameter
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkFindUsagesTest : BasePlatformTestCase() {

  // region Functions

  @Test
  fun `should find function call usages`() {
    myFixture.configureByText(
      "defs.bzl",
      """
      def helper(x):
          return x + 1

      helper(1)
      helper(2)
      """.trimIndent(),
    )
    myFixture.findStarlarkUsages("helper") shouldHaveSize 2
  }

  @Test
  fun `should find usage of loaded function in another file`() {
    BazelProjectFixtures.initializeBazelProject(project, myFixture.tempDirPath)
    myFixture.addFileToProject("MODULE.bazel", "")
    myFixture.addFileToProject("BUILD", "")
    val defsFile = myFixture.addFileToProject(
      "defs.bzl",
      """
      def helper(x):
          return x + 1
      """.trimIndent(),
    )
    myFixture.addFileToProject(
      "consumer.bzl",
      """
      load("//:defs.bzl", "helper")

      result = helper(42)
      """.trimIndent(),
    )
    myFixture.openFileInEditor(defsFile.virtualFile)
    myFixture.findStarlarkUsages("helper") shouldHaveSize 2
  }

  @Test
  fun `should not find usages for unreferenced function`() {
    myFixture.configureByText(
      "defs.bzl",
      """
      def helper(x):
          return x + 1

      y = 42
      """.trimIndent(),
    )
    myFixture.findStarlarkUsages("helper").shouldBeEmpty()
  }

  // endregion

  // region Variables

  @Test
  fun `should find variable usage in same file`() {
    myFixture.configureByText(
      "defs.bzl",
      """
      x = 42
      y = x + 1
      """.trimIndent(),
    )
    myFixture.findStarlarkUsages("x") shouldHaveSize 1
  }

  @Test
  fun `should find variable usage inside function`() {
    myFixture.configureByText(
      "defs.bzl",
      """
      def foo():
          x = 1
          y = x + x
      """.trimIndent(),
    )
    myFixture.findStarlarkUsages("x") shouldHaveSize 2
  }

  @Test
  fun `should find usage of loaded variable in another file`() {
    BazelProjectFixtures.initializeBazelProject(project, myFixture.tempDirPath)
    myFixture.addFileToProject("MODULE.bazel", "")
    myFixture.addFileToProject("BUILD", "")
    val defsFile = myFixture.addFileToProject(
      "defs.bzl",
      """
      MY_CONST = 42
      """.trimIndent(),
    )
    myFixture.addFileToProject(
      "consumer.bzl",
      """
      load("//:defs.bzl", "MY_CONST")

      x = MY_CONST + 1
      """.trimIndent(),
    )
    myFixture.openFileInEditor(defsFile.virtualFile)
    myFixture.findStarlarkUsages("MY_CONST") shouldHaveSize 2
  }

  @Test
  fun `should not find usages for unreferenced variable`() {
    myFixture.configureByText(
      "defs.bzl",
      """
      x = 42
      y = 1
      """.trimIndent(),
    )
    myFixture.findStarlarkUsages("x").shouldBeEmpty()
  }

  // endregion

  // region Parameters

  @Test
  fun `should find parameter usage in function body`() {
    myFixture.configureByText(
      "defs.bzl",
      """
      def foo(x, y):
          return x + y
      """.trimIndent(),
    )
    myFixture.findStarlarkUsages("x") shouldHaveSize 1
  }

  @Test
  fun `should find parameter usage in lambda`() {
    myFixture.configureByText(
      "defs.bzl",
      """
      f = lambda x, y: x + y
      """.trimIndent(),
    )
    myFixture.findStarlarkUsages("x") shouldHaveSize 1
  }

  @Test
  fun `should not find usages for unused parameter`() {
    myFixture.configureByText(
      "defs.bzl",
      """
      def foo(x, y):
          return y
      """.trimIndent(),
    )
    myFixture.findStarlarkUsages("x").shouldBeEmpty()
  }

  // endregion

  // region Scope boundaries

  @Test
  fun `should not find usages of same-named variable in sibling function`() {
    myFixture.configureByText(
      "defs.bzl",
      """
      def foo():
          x = 1
          return x

      def bar():
          x = 2
          return x
      """.trimIndent(),
    )
    myFixture.findStarlarkUsages("x") shouldHaveSize 1
  }

  @Test
  fun `should not find usages of parameter in sibling function`() {
    myFixture.configureByText(
      "defs.bzl",
      """
      def foo(x):
          return x

      def bar(x):
          return x
      """.trimIndent(),
    )
    myFixture.findStarlarkUsages("x") shouldHaveSize 1
  }

  @Test
  fun `should not leak comprehension variable to enclosing scope`() {
    myFixture.configureByText(
      "defs.bzl",
      """
      def foo():
          x = 10
          result = [x for x in range(5)]
          return x
      """.trimIndent(),
    )
    val compTarget = PsiTreeUtil.collectElementsOfType(myFixture.file, StarlarkTargetExpression::class.java)
      .findLast { it.name == "x" }
      .shouldNotBeNull()
    myFixture.findUsages(compTarget) shouldHaveSize 1
  }

  @Test
  fun `should not leak lambda parameter to enclosing scope`() {
    myFixture.configureByText(
      "defs.bzl",
      """
      x = 10
      f = lambda x: x + 1
      y = x + 2
      """.trimIndent(),
    )
    val lambdaParam = PsiTreeUtil.collectElementsOfType(myFixture.file, StarlarkParameter::class.java)
      .find { it.name == "x" }
      .shouldNotBeNull()
    myFixture.findUsages(lambdaParam) shouldHaveSize 1
  }

  // endregion

  // region Comprehensions

  @Test
  fun `should find comprehension variable usage`() {
    myFixture.configureByText(
      "defs.bzl",
      """
      result = [x * 2 for x in range(10)]
      """.trimIndent(),
    )
    myFixture.findStarlarkUsages("x") shouldHaveSize 1
  }

  // endregion

  // region Unrelated results

  @Test
  fun `should not find usages in non-starlark files`() {
    myFixture.configureByText(
      "defs.bzl",
      """
      def foo():
        a = 1
      """.trimIndent(),
    )
    myFixture.addFileToProject(
      "README.md",
      """
      # Project
      This project uses `foo` function.
      """.trimIndent(),
    )

    myFixture.findStarlarkUsages("foo").shouldBeEmpty()
  }

  // endregion
}

private fun com.intellij.testFramework.fixtures.CodeInsightTestFixture.findStarlarkUsages(name: String) =
  findUsages(
    PsiTreeUtil.collectElementsOfType(file, StarlarkNamedElement::class.java)
      .find { it.name == name }
      ?: error("StarlarkNamedElement with name '$name' not found"),
  )
