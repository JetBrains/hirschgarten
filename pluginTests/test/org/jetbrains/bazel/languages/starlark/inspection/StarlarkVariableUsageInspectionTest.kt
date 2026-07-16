package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkVariableUsageInspectionTest : BasePlatformTestCase() {
  private val descriptionGlobal = StarlarkBundle.message("inspection.description.variable.referenced.before.assignment", "Global", "x")
  private val descriptionLocal = StarlarkBundle.message("inspection.description.variable.referenced.before.assignment", "Local", "x")
  private val descriptionFree = StarlarkBundle.message("inspection.description.variable.referenced.before.assignment", "Free", "x")
  private val descriptionUndefined = StarlarkBundle.message("inspection.description.variable.undefined", "x")

  override fun setUp() {
    super.setUp()
    myFixture.enableInspections(StarlarkVariableUsageInspection())
  }

  @Test
  fun `local variable referenced before assignment should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f():
        print(<error descr="$descriptionLocal">x</error>)
        x = "ok"
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `global variable referenced before assignment should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      print(<error descr="$descriptionGlobal">x</error>)
      x = "ok"
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `free variable referenced before assignment should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def outer():
        def inner():
          print(<error descr="$descriptionFree">x</error>)
        inner()
        x = "ok"
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `function referenced before declaration should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      <error descr="$descriptionGlobal">x</error>()
      def x():
        pass
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `variable referenced before assignment in for loop should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
        def fun():
          print(<error descr="$descriptionLocal">x</error>())
          for x in [1,2]:
            print(x)
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `symbol referenced before string load should be highlighted`() {
    myFixture.configureByText(
      "BUILD",
      """
      <error descr="$descriptionGlobal">x</error>()
      load("//com/example:test.bzl", "x")
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `symbol referenced before named load should be highlighted`() {
    myFixture.configureByText(
      "BUILD",
      """
      <error descr="$descriptionGlobal">x</error>()
      load("//com/example:test.bzl", x="fun")
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `free variable referenced after assignment should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def outer():
        def inner():
          print(x)
        x = "ok"
        inner()
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `free variable overwritten as local referenced after assignment should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def outer():
        def inner():
          x = "local"
          print(x)
        inner()
        x = "free"
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }


  @Test
  fun `overwritten free variable referenced after assignment should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def outer():
        def inner():
          x = "local"
          print(x)
        inner()
        x = "free"
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `variable assigned before usage should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f():
        x = "ok"
        print(x)
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `variable usage after if when assigned in both branches should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f(flag):
        if flag:
          x = "a"
        else:
          x = "b"
        print(x)
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }


  @Test
  fun `undefined variable should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f():
        print(<error descr="$descriptionUndefined">x</error>)
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `undefined function call should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      a=1
      <error descr="$descriptionUndefined">x</error>(a)
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `variable assigned in the same scope should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f():
        x = "ok"
        print(x)
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `builtins should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f():
        print("ok")
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `loaded function call should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      load(":ext.bzl", x="fun")
      a=1
      x(a)
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `function parameter should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f(x):
        print(x)
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `function parameter used before local reassignment should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f(x):
        print(x)
        x = "ok"
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `qualified member call should not be highlighted as undefined variable`() {
    myFixture.configureByText(
      "test.bzl",
      """
      out = []
      for item in [1, 2, 3]:
        out.append(str(item))
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }
}
