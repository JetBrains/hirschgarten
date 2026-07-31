package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkRecursionInspectionTest : BasePlatformTestCase() {
  private val description = StarlarkBundle.message("inspection.description.recursion.detected")

  @Before
  fun beforeEach() {
    myFixture.enableInspections(StarlarkRecursionInspection())
  }

  @Test
  fun `recursive function call should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f():
        <error descr="$description">f</error>()
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `recursive function cycle should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f1():
        <error descr="$description">f2</error>()

      def f2():
        <error descr="$description">f3</error>()

      def f3():
        <error descr="$description">f1</error>()

      f1()
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `non recursive function call chain should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f1():
        f2()

      def f2():
        f3()

      def f3():
        pass

      f1()
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `top level call to recursive function should not be highlighted separately`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f():
        <error descr="$description">f</error>()

      f()
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `recursive lambda alias should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      f = lambda: <error descr="$description">f</error>()
      f()
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `recursive cycle through lambda alias should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f():
        <error descr="$description">g</error>()

      g = lambda: <error descr="$description">f</error>()

      f()
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `recursive cycle from lambda to function should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      l = lambda: <error descr="$description">f</error>()

      def f():
        <error descr="$description">l</error>()

      f()
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `direct recursive immediately invoked lambda should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      f = lambda: <error descr="$description">f</error>()
      (lambda: f())()
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `lambda call inside function should belong to lambda not enclosing function`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f():
        callback = lambda: f()
        print("ok")

      f()
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `recursive lambda inside function should be highlighted on lambda only`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def outer():
        callback = lambda: <error descr="$description">callback</error>()
        callback()

      outer()
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `builtin calls should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f():
        print("ok")
        len([1, 2, 3])

      f()
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `unresolved calls should not be highlighted by recursion inspection`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f():
        unknown()

      f()
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }
}
