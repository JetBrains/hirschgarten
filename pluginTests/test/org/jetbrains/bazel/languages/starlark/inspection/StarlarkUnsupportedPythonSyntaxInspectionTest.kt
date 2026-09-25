package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkUnsupportedPythonSyntaxInspectionTest : BasePlatformTestCase() {
  private val unsupportedPower = StarlarkBundle.message("inspection.description.unsupported.binary.operator", "**")
  private val chainedComparison = StarlarkBundle.message("inspection.description.unsupported.chained.comparison")
  private val generatorExpression = StarlarkBundle.message("inspection.description.unsupported.generator.expression")
  private val unpackingExpression = StarlarkBundle.message("inspection.description.unsupported.unpacking.expression")

  @Before
  fun beforeEach() {
    myFixture.enableInspections(StarlarkUnsupportedPythonSyntaxInspection())
  }

  @Test
  fun `unsupported python keywords should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f():
        while True:
          pass
        class C:
          pass
        try:
          pass
        except Error as e:
          pass
        finally:
          pass
        raise "error"
        x = yield 1
        import foo
        from foo import bar
        global x
        nonlocal y
        z = x is y
        with ctx:
          pass
        del x
        assert True
        async def g():
          await h()
      """.trimIndent(),
    )
    assertHasErrors(
      StarlarkBundle.message("inspection.description.unsupported.keyword", "while"),
      StarlarkBundle.message("inspection.description.unsupported.keyword", "class"),
      StarlarkBundle.message("inspection.description.unsupported.keyword", "try"),
      StarlarkBundle.message("inspection.description.unsupported.keyword", "except"),
      StarlarkBundle.message("inspection.description.unsupported.keyword", "as"),
      StarlarkBundle.message("inspection.description.unsupported.keyword", "finally"),
      StarlarkBundle.message("inspection.description.unsupported.keyword", "raise"),
      StarlarkBundle.message("inspection.description.unsupported.keyword", "yield"),
      StarlarkBundle.message("inspection.description.unsupported.keyword", "import"),
      StarlarkBundle.message("inspection.description.unsupported.keyword", "from"),
      StarlarkBundle.message("inspection.description.unsupported.keyword", "global"),
      StarlarkBundle.message("inspection.description.unsupported.keyword", "nonlocal"),
      StarlarkBundle.message("inspection.description.unsupported.keyword", "is"),
      StarlarkBundle.message("inspection.description.unsupported.keyword", "with"),
      StarlarkBundle.message("inspection.description.unsupported.keyword", "del"),
      StarlarkBundle.message("inspection.description.unsupported.keyword", "assert"),
      StarlarkBundle.message("inspection.description.unsupported.keyword", "async"),
      StarlarkBundle.message("inspection.description.unsupported.keyword", "await"),
    )
  }

  @Test
  fun `chained comparison should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      x = 1 < x <error descr="$chainedComparison"><</error> 5
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `chained comparison with in should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      x = a in b <error descr="$chainedComparison">in</error> c
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `chained comparison with not in should highlight whole not in operator`() {
    myFixture.configureByText(
      "test.bzl",
      """
      x = a in b <error descr="$chainedComparison">not in</error> c
      y = 1 not in [1, 2] <error descr="$chainedComparison">not in</error> [[1, 2]]
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `regular in and not in should not be highlighted as chained comparison`() {
    myFixture.configureByText(
      "test.bzl",
      """
      x = 1 in [1, 2]
      y = 1 not in [1, 2]
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `generator expression should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      x = <error descr="$generatorExpression">(i for i in [1, 2, 3])</error>
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `unsupported power operator should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      a = 1 <error descr="$unsupportedPower">**</error> 2
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `supported arithmetic boolean comparison and bitwise operators should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      a = 1 + 2
      b = 3 - 2
      c = 4 * 5
      d = 5 // 2
      e = 5 % 2
      f = 1 in [1, 2]
      g = 1 not in [1, 2]
      h = 1 == 1
      i = 1 != 2
      j = 1 < 2
      k = 1 <= 2
      l = 2 > 1
      m = 2 >= 1
      n = not False
      o = -1
      p = +1
      q = ~1
      r = 1 & 2
      s = 1 | 2
      t = 1 ^ 2
      u = 1 << 2
      v = 4 >> 1
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `unsupported keywords inside strings should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      text = "while class try except finally raise yield import global nonlocal is"
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `implicit string concatenation should be handled by parser`() {
    myFixture.configureByText(
      "test.bzl",
      """
      x = "a"<error descr="End of statement expected"> </error>"b"
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `parenthesized comparisons should not be highlighted as chained comparison`() {
    myFixture.configureByText(
      "test.bzl",
      """
      x = (1 < 2) == True
      y = 1 < (2 == 3)
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `list and dict comprehensions should not be highlighted as generator expressions`() {
    myFixture.configureByText(
      "test.bzl",
      """
      xs = [x for x in [1, 2, 3]]
      d = {x: x for x in [1, 2, 3]}
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `unsupported list unpacking should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      value = [<error descr="$unpackingExpression">*[1, 2]</error>]
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `unsupported dictionary unpacking should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      value = {<error descr="$unpackingExpression">**{"key": 1}</error>}
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `call argument unpacking should not be highlighted as unsupported syntax`() {
    myFixture.configureByText(
      "test.bzl",
      """
      f(*args)
      g(**kwargs)
      h(1, *args, **kwargs)
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `variadic parameters should not be highlighted as unsupported syntax`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f(*args):
        pass

      def g(**kwargs):
        pass

      def h(a, *args, **kwargs):
        pass
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  private fun assertHasErrors(vararg descriptions: String) {
    val actualDescriptions = myFixture.doHighlighting().mapNotNull { it.description }
    val missingDescriptions = descriptions.filterNot { it in actualDescriptions }
    assertTrue(
      "Expected errors:\n${descriptions.joinToString("\n")}\n\nActual highlights:\n${actualDescriptions.joinToString("\n")}",
      missingDescriptions.isEmpty(),
    )
  }
}
