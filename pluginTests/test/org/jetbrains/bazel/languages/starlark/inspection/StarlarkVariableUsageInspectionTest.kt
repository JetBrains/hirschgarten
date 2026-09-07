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
  fun `assignment to subscription expression using undefined variable as index should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      items = [0]
      items[<error descr="$descriptionUndefined">x</error>] = 1
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `assignment to subscription expression using undefined receiver should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      <error descr="$descriptionUndefined">x</error>[0] = 1
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
  fun `undefined variable as function parameter default should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      y = 1  
      def f(x = <error descr="$descriptionUndefined">x</error>):
        return x
      def g(y = y):
        return y  
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `function parameter default should not see later parameter`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f(a = <error descr="$descriptionUndefined">x</error>, x = 1):
        return x
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

  @Test
  fun `first comprehension iterable should use outer scope`() {
    myFixture.configureByText(
      "test.bzl",
      """
      [x for x in <error descr="$descriptionUndefined">x</error>]
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `first comprehension iterable should resolve outer variable`() {
    myFixture.configureByText(
      "test.bzl",
      """
      x = [1, 2]
      [x for x in x]
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `comprehension body should see target declared later textually`() {
    myFixture.configureByText(
      "test.bzl",
      """
      xs = [1, 2]
      [x for x in xs]
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `first destructuring comprehension iterable should use outer scope`() {
    myFixture.configureByText(
      "test.bzl",
      """
      [x for x, y in <error descr="$descriptionUndefined">x</error>]
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `first parenthesized destructuring comprehension iterable should use outer scope`() {
    myFixture.configureByText(
      "test.bzl",
      """
      [x for (x, y) in <error descr="$descriptionUndefined">x</error>]
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `comprehension body should see destructuring targets declared later textually`() {
    myFixture.configureByText(
      "test.bzl",
      """
      pairs = [(1, 2)]
      [x + y for x, y in pairs]
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `second comprehension iterable should see previous comprehension target`() {
    myFixture.configureByText(
      "test.bzl",
      """
      xs = [1, 2]
      [(x, y) for x in xs for y in [x]]
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `third comprehension iterable should see previous comprehension targets`() {
    myFixture.configureByText(
      "test.bzl",
      """
      xs = [1, 2]
      [(x, y, z) for x in xs for y in [x] for z in [y]]
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `later comprehension iterable should see destructuring target from previous clause`() {
    myFixture.configureByText(
      "test.bzl",
      """
      pairs = [(1, 2)]
      [(x, y, z) for x, y in pairs for z in [x + y]]
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `second comprehension iterable should not see later comprehension target`() {
    myFixture.configureByText(
      "test.bzl",
      """
      zs = [1, 2]
      [(z, y, x) for z in zs for y in [<error descr="$descriptionUndefined">x</error>] for x in [1]]
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `second comprehension if clause should not see later comprehension target`() {
    myFixture.configureByText(
      "test.bzl",
      """
      ys = [1, 2]
      [(y, x) for y in ys if <error descr="$descriptionUndefined">x</error> for x in [1]]
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `redeclared comprehension target iterable should see previous target with same name`() {
    myFixture.configureByText(
      "test.bzl",
      """
      xs = [1, 2]
      [x for x in xs for x in [x]]
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `first comprehension iterable should use outer scope even when later target has same name`() {
    myFixture.configureByText(
      "test.bzl",
      """
      [x for y in <error descr="$descriptionUndefined">x</error> for x in [1]]
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `nested first comprehension iterable should see outer comprehension target`() {
    myFixture.configureByText(
      "test.bzl",
      """
      result = [[y for y in x] for x in [[1]]]
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }


  @Test
  fun `lambda parameter should not be highlighted as undefined`() {
    myFixture.configureByText(
      "test.bzl",
      """
      f = lambda x: x
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `undefined variable in lambda body should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      f = lambda y: <error descr="$descriptionUndefined">x</error> + y
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `undefined variable as lambda parameter default should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      y = 1
      f = lambda x = <error descr="$descriptionUndefined">x</error>: x
      g = lambda y = y: y
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `nested lambda should use nearest lambda parameter`() {
    myFixture.configureByText(
      "test.bzl",
      """
      f = lambda x: lambda x: x + 1
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }
}
