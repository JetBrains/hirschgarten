package org.jetbrains.bazel.languages.starlark.rename

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement
import org.jetbrains.bazel.project.BazelProjectFixtures
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkRenameTest : BasePlatformTestCase() {

  // region Functions

  @Test
  fun `should rename function with multiple call sites`() {
    myFixture.configureByText(
      "defs.bzl",
      """
      def helper(x):
          return x + 1

      helper(1)
      helper(2)
      """.trimIndent(),
    )
    myFixture.renameStarlarkElement("helper", "util")
    myFixture.checkResult(
      """
      def util(x):
          return x + 1

      util(1)
      util(2)
      """.trimIndent(),
    )
  }

  @Test
  fun `should rename top-level function across files`() {
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
    val consumerFile = myFixture.addFileToProject(
      "consumer.bzl",
      """
      load("//:defs.bzl", "helper")

      result = helper(42)
      """.trimIndent(),
    )
    myFixture.openFileInEditor(defsFile.virtualFile)
    myFixture.renameStarlarkElement("helper", "util")
    myFixture.checkResult(
      """
      def util(x):
          return x + 1
      """.trimIndent(),
    )
    assertEquals(
      """
      load("//:defs.bzl", "util")

      result = util(42)
      """.trimIndent(),
      consumerFile.text,
    )
  }

  // endregion

  // region Variables

  @Test
  fun `should rename variable and its usages`() {
    myFixture.configureByText(
      "defs.bzl",
      """
      x = 42
      y = x + 1
      """.trimIndent(),
    )
    myFixture.renameStarlarkElement("x", "value")
    myFixture.checkResult(
      """
      value = 42
      y = value + 1
      """.trimIndent(),
    )
  }

  @Test
  fun `should rename variable inside function`() {
    myFixture.configureByText(
      "defs.bzl",
      """
      def foo():
          x = 1
          y = x + x
      """.trimIndent(),
    )
    myFixture.renameStarlarkElement("x", "val")
    myFixture.checkResult(
      """
      def foo():
          val = 1
          y = val + val
      """.trimIndent(),
    )
  }

  @Test
  fun `should rename top-level variable across files`() {
    BazelProjectFixtures.initializeBazelProject(project, myFixture.tempDirPath)
    myFixture.addFileToProject("MODULE.bazel", "")
    myFixture.addFileToProject("BUILD", "")
    val defsFile = myFixture.addFileToProject(
      "defs.bzl",
      """
      MY_CONST = 42
      """.trimIndent(),
    )
    val consumerFile = myFixture.addFileToProject(
      "consumer.bzl",
      """
      load("//:defs.bzl", "MY_CONST")

      x = MY_CONST + 1
      """.trimIndent(),
    )
    myFixture.openFileInEditor(defsFile.virtualFile)
    myFixture.renameStarlarkElement("MY_CONST", "MY_VALUE")
    myFixture.checkResult(
      """
      MY_VALUE = 42
      """.trimIndent(),
    )
    assertEquals(
      """
      load("//:defs.bzl", "MY_VALUE")

      x = MY_VALUE + 1
      """.trimIndent(),
      consumerFile.text,
    )
  }

  // endregion

  // region Parameters

  @Test
  fun `should rename parameter and its usages in function body`() {
    myFixture.configureByText(
      "defs.bzl",
      """
      def foo(x, y):
          return x + y
      """.trimIndent(),
    )
    myFixture.renameStarlarkElement("x", "a")
    myFixture.checkResult(
      """
      def foo(a, y):
          return a + y
      """.trimIndent(),
    )
  }

  @Test
  fun `should rename lambda parameter`() {
    myFixture.configureByText(
      "defs.bzl",
      """
      f = lambda x, y: x + y
      """.trimIndent(),
    )
    myFixture.renameStarlarkElement("x", "a")
    myFixture.checkResult(
      """
      f = lambda a, y: a + y
      """.trimIndent(),
    )
  }

  // endregion

  // region Scope boundaries

  @Test
  fun `should not rename same-named variable in sibling function`() {
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
    myFixture.renameStarlarkElement("x", "renamed")
    myFixture.checkResult(
      """
      def foo():
          renamed = 1
          return renamed

      def bar():
          x = 2
          return x
      """.trimIndent(),
    )
  }

  @Test
  fun `should not rename across disjoint function scopes`() {
    myFixture.configureByText(
      "defs.bzl",
      """
      def foo():
          a = 1
      def bar():
          b = 2
      """.trimIndent(),
    )
    myFixture.renameStarlarkElement("a", "b")
    myFixture.checkResult(
      """
      def foo():
          b = 1
      def bar():
          b = 2
      """.trimIndent(),
    )
  }

  // endregion

  // region Unrelated results

  @Test
  fun `rename target should not modify non-starlark files`() {
    myFixture.addFileToProject("MODULE.bazel", "")
    myFixture.configureByText(
      "defs.bzl",
      """
      def foo():
          a = 1
          
      def bar():
          foo()
      """.trimIndent(),
    )
    val readme = myFixture.addFileToProject(
      "README.md",
      """
      # Project
      This project uses `foo` function.
      """.trimIndent(),
    )
    val readmeContentBefore = readme.text

    myFixture.renameStarlarkElement("foo", "new_foo")

    myFixture.checkResult(
      """
      def new_foo():
          a = 1
          
      def bar():
          new_foo()
      """.trimIndent(),
    )
    readme.text shouldBe readmeContentBefore
  }

  // endregion

  // region Conflicts

  @Test
  fun `should detect variable to variable conflict at file scope`() {
    myFixture.configureByText(
      "defs.bzl",
      """
      x = 1
      y = 2
      """.trimIndent(),
    )
    shouldThrowConflict(conflictMessage("y", "defs.bzl")) {
      myFixture.renameStarlarkElement("x", "y")
    }
  }

  @Test
  fun `should detect variable to variable conflict in function scope`() {
    myFixture.configureByText(
      "defs.bzl",
      """
      def foo():
          a = 1
          b = 2
      """.trimIndent(),
    )
    shouldThrowConflict(conflictMessage("b", "foo")) {
      myFixture.renameStarlarkElement("a", "b")
    }
  }

  @Test
  fun `should detect parameter to parameter conflict`() {
    myFixture.configureByText(
      "defs.bzl",
      """
      def foo(x, y):
          pass
      """.trimIndent(),
    )
    shouldThrowConflict(conflictMessage("y", "foo")) {
      myFixture.renameStarlarkElement("x", "y")
    }
  }

  @Test
  fun `should detect variable to parameter conflict`() {
    myFixture.configureByText(
      "defs.bzl",
      """
      def foo(x):
          y = 1
      """.trimIndent(),
    )
    shouldThrowConflict(conflictMessage("x", "foo")) {
      myFixture.renameStarlarkElement("y", "x")
    }
  }

  @Test
  fun `should detect function to function conflict`() {
    myFixture.configureByText(
      "defs.bzl",
      """
      def foo():
          pass
      def bar():
          pass
      """.trimIndent(),
    )
    shouldThrowConflict(conflictMessage("bar", "defs.bzl")) {
      myFixture.renameStarlarkElement("foo", "bar")
    }
  }

  @Test
  fun `should detect function to variable conflict`() {
    myFixture.configureByText(
      "defs.bzl",
      """
      x = 1
      def foo():
          pass
      """.trimIndent(),
    )
    shouldThrowConflict(conflictMessage("x", "defs.bzl")) {
      myFixture.renameStarlarkElement("foo", "x")
    }
  }

  @Test
  fun `should not detect conflict when names differ`() {
    myFixture.configureByText(
      "defs.bzl",
      """
      x = 1
      y = 2
      """.trimIndent(),
    )
    myFixture.renameStarlarkElement("x", "z")
    myFixture.checkResult(
      """
      z = 1
      y = 2
      """.trimIndent(),
    )
  }

  @Test
  fun `should detect lambda parameter to lambda parameter conflict`() {
    myFixture.configureByText(
      "defs.bzl",
      """
      f = lambda x, y: x + y
      """.trimIndent(),
    )
    shouldThrowConflict(conflictMessage("y", "defs.bzl")) {
      myFixture.renameStarlarkElement("x", "y")
    }
  }

  @Test
  fun `should not detect conflict between lambda parameter and outer variable`() {
    myFixture.configureByText(
      "defs.bzl",
      """
      x = 1
      f = lambda y: y
      """.trimIndent(),
    )
    myFixture.renameStarlarkElement("y", "x")
    myFixture.checkResult(
      """
      x = 1
      f = lambda x: x
      """.trimIndent(),
    )
  }

  @Test
  fun `should not detect conflict between comprehension variable and outer variable`() {
    myFixture.configureByText(
      "defs.bzl",
      """
      def foo():
          x = 1
          result = [y for y in range(10)]
      """.trimIndent(),
    )
    myFixture.renameStarlarkElement("y", "x")
    myFixture.checkResult(
      """
      def foo():
          x = 1
          result = [x for x in range(10)]
      """.trimIndent(),
    )
  }

  @Test
  fun `should not detect conflict between function parameter and comprehension variable`() {
    myFixture.configureByText(
      "defs.bzl",
      """
      def foo(x):
          result = [y for y in range(10)]
      """.trimIndent(),
    )
    myFixture.renameStarlarkElement("x", "y")
    myFixture.checkResult(
      """
      def foo(y):
          result = [y for y in range(10)]
      """.trimIndent(),
    )
  }

  @Test
  fun `should detect conflict between private top-level declarations`() {
    myFixture.configureByText(
      "defs.bzl",
      """
      _x = 1
      _y = 2
      """.trimIndent(),
    )
    shouldThrowConflict(conflictMessage("_y", "defs.bzl")) {
      myFixture.renameStarlarkElement("_x", "_y")
    }
  }

  @Test
  fun `should not detect conflict between private function and local variable in other function`() {
    myFixture.configureByText(
      "defs.bzl",
      """
      def _helper():
          pass
      def foo():
          x = 1
      """.trimIndent(),
    )
    myFixture.renameStarlarkElement("x", "_helper")
    myFixture.checkResult(
      """
      def _helper():
          pass
      def foo():
          _helper = 1
      """.trimIndent(),
    )
  }

  // endregion

  private fun conflictMessage(newName: String, fileName: String): String =
    StarlarkBundle.message("rename.name.conflict.already.exists", newName, fileName)

  private inline fun shouldThrowConflict(message: String, block: () -> Unit) {
    shouldThrowWithMessage<BaseRefactoringProcessor.ConflictsInTestsException>(message) {
      block()
    }
  }
}

private fun CodeInsightTestFixture.renameStarlarkElement(oldName: String, newName: String) {
  val element = PsiTreeUtil.collectElementsOfType(file, StarlarkNamedElement::class.java)
    .find { it.name == oldName }
    ?: error("StarlarkNamedElement with name '$oldName' not found")
  renameElement(element, newName)
}
