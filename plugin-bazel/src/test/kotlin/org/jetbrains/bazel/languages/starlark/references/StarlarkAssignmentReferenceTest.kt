package org.jetbrains.bazel.languages.starlark.references

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkReferencesTestCase
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkParenthesizedExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTupleExpression
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkExpressionStatement
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkAssignmentReferenceTest : StarlarkReferencesTestCase() {
  @Test
  fun `should resolve basic assignment`() {
    // given
    myFixture.configureByFile("AssignmentReference.bzl")
    val expectedResolved =
      myFixture.file
        .getChildOfType<StarlarkAssignmentStatement>()!!
        .getChildOfType<StarlarkTargetExpression>()
    // when
    val resolved = getUsage()?.reference?.resolve()
    // then
    resolved shouldBe expectedResolved
  }

  @Test
  fun `should resolve tuple assignment`() {
    // given
    myFixture.configureByFile("TupleAssignmentReference.bzl")
    val expectedResolved =
      myFixture.file
        .getChildOfType<StarlarkAssignmentStatement>()!!
        .getChildOfType<StarlarkTupleExpression>()!!
        .getChildOfType<StarlarkTargetExpression>()
    // when
    val resolved = getUsage()?.reference?.resolve()
    // then
    resolved shouldBe expectedResolved
  }

  @Test
  fun `should resolve parenthesized tuple assignment`() {
    // given
    myFixture.configureByFile("ParenthesizedTupleAssignmentReference.bzl")
    val expectedResolved =
      myFixture.file
        .getChildOfType<StarlarkAssignmentStatement>()!!
        .getChildOfType<StarlarkParenthesizedExpression>()!!
        .getChildOfType<StarlarkTupleExpression>()!!
        .getChildOfType<StarlarkTargetExpression>()
    // when
    val resolved = getUsage()?.reference?.resolve()
    // then
    resolved shouldBe expectedResolved
  }

  private fun getUsage(): StarlarkReferenceExpression? =
    myFixture.file
      .getChildOfType<StarlarkExpressionStatement>()
      ?.getChildOfType<StarlarkReferenceExpression>()
}
