package org.jetbrains.bazel.languages.starlark.references

import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkReferencesTestCase
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkParenthesizedExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTupleExpression
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkExpressionStatement
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.junit.jupiter.api.Test

@BazelTestApplication
class StarlarkAssignmentReferenceTest : StarlarkReferencesTestCase() {
  inline fun <reified T : PsiElement> PsiElement.getChildOfType(): T? =
    PsiTreeUtil.getChildOfType<T>(this, T::class.java)

  @Test
  fun `should resolve basic assignment`() {
    // given
    myFixture.configureByFile("AssignmentReference.bzl")
    val expectedResolved = runReadActionBlocking {
      myFixture.file.getChildOfType<StarlarkAssignmentStatement>()!!
        .getChildOfType<StarlarkTargetExpression>()
    }
    // when
    val resolved = resolveUsage()
    // then
    resolved shouldBe expectedResolved
  }

  @Test
  fun `should resolve tuple assignment`() {
    // given
    myFixture.configureByFile("TupleAssignmentReference.bzl")
    val expectedResolved = runReadActionBlocking {
      myFixture.file
        .getChildOfType<StarlarkAssignmentStatement>()!!
        .getChildOfType<StarlarkTupleExpression>()!!
        .getChildOfType<StarlarkTargetExpression>()
    }
    // when
    val resolved = resolveUsage()
    // then
    resolved shouldBe expectedResolved
  }

  @Test
  fun `should resolve parenthesized tuple assignment`() {
    // given
    myFixture.configureByFile("ParenthesizedTupleAssignmentReference.bzl")
    val expectedResolved = runReadActionBlocking {
      myFixture.file
        .getChildOfType<StarlarkAssignmentStatement>()!!
        .getChildOfType<StarlarkParenthesizedExpression>()!!
        .getChildOfType<StarlarkTupleExpression>()!!
        .getChildOfType<StarlarkTargetExpression>()
    }
    // when
    val resolved = resolveUsage()
    // then
    resolved shouldBe expectedResolved
  }

  private fun resolveUsage(): PsiElement? =
    runReadActionBlocking {
      myFixture.file
        .getChildOfType<StarlarkExpressionStatement>()
        ?.getChildOfType<StarlarkReferenceExpression>()
        ?.reference
        ?.resolve()
    }
}
