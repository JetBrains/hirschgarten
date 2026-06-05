package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkFunctionParameterValidationInspectionTest : BasePlatformTestCase() {
  val descriptionDuplicate = StarlarkBundle.message("inspection.description.parameters.duplicate.name", "a")
  val descriptionMultipleVariadic = StarlarkBundle.message("inspection.description.parameters.multiple.variadic")
  val descriptionAfterKeywordVariadic = StarlarkBundle.message("inspection.description.parameters.after.keyword.variadic")
  val descriptionMandatoryAfterOptional = StarlarkBundle.message("inspection.description.parameters.mandatory.after.optional")
  val descriptionAsterisk = StarlarkBundle.message("inspection.description.parameters.bare.asterisk")
  @Before
  fun beforeEach() {
    myFixture.enableInspections(StarlarkFunctionParameterValidationInspection())
  }

  @Test
  fun `duplicate parameter name in function should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f(a, <error descr="$descriptionDuplicate">a</error>):
          pass
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `duplicate parameter name in lambda should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      x = lambda a, <error descr="$descriptionDuplicate">a</error>: a
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `duplicate parameter across kinds should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f1(a, <error descr="$descriptionDuplicate">*a</error>):
          pass
      def f2(*a, <error descr="$descriptionDuplicate">a = 1</error>):
          pass
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `multiple variadic parameters should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f(*args, <error descr="$descriptionMultipleVariadic">*more</error>):
          pass
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `mandatory after optional before variadic in function should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f(a = 1, <error descr="$descriptionMandatoryAfterOptional">b</error>):
          pass
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `mandatory after optional before variadic in lambda should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      x = lambda a = 1, <error descr="$descriptionMandatoryAfterOptional">b</error>: b
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `mandatory parameter after keyword variadic should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f(**kwargs, <error descr="$descriptionAfterKeywordVariadic">a</error>):
          pass
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `optional parameter after keyword variadic should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f(**kwargs, <error descr="$descriptionAfterKeywordVariadic">a=1</error>):
          pass
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `variadic parameter after keyword variadic should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f(**kwargs, <error descr="$descriptionAfterKeywordVariadic">*args</error>):
          pass
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `multiple keyword variadic should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f(**kwargs, <error descr="$descriptionAfterKeywordVariadic">**more</error>):
          pass
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }


  @Test
  fun `correct parameter order should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f(a, b = 1, *args, c, d = 2, **kwargs):
          pass
      x = lambda p, q = 1, *args, r, **kwargs: p
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `bare star followed by keyword variadic should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def e8(a, <error descr="$descriptionAsterisk">*</error>, **k):
          pass
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `bare star followed by variadic should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f(a, <error descr="$descriptionAsterisk">*</error>, <error descr="$descriptionMultipleVariadic">*more</error>):
          pass
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `bare star followed by keyword-only mandatory should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f(a, *, b):
          pass
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `bare star followed by keyword-only optional should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f(a, *, b = 1):
          pass
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }
}
