package org.jetbrains.bazel.languages.starlark.references

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkReferencesTestCase
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkExpressionStatement
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkArgumentReferenceTest : StarlarkReferencesTestCase() {
  @Test
  fun `should resolve arguments in function call`() {
    // given
    myFixture.configureByFile("ArgumentReference.bzl")
    // when
    val expectedResolved = getParameters()
    val arguments = getNamedArguments()
    val resolved = arguments?.map { it.reference?.resolve() }
    // then
    resolved shouldContainExactlyInAnyOrder expectedResolved
  }

  @Test
  fun `should resolve arguments as kwargs in function call`() {
    // given
    myFixture.configureByFile("KeywordVariadicArgumentReference.bzl")
    // when
    val expectedResolved = getParameters()
    val arguments = getNamedArguments()
    val resolved = arguments?.map { it.reference?.resolve() }
    // then
    resolved shouldContainExactlyInAnyOrder expectedResolved
  }

  private fun getParameters() =
    myFixture.file.children
      .filterIsInstance<StarlarkFunctionDeclaration>()
      .firstOrNull()
      ?.getParameters()

  private fun getNamedArguments() =
    myFixture.file.children
      .filterIsInstance<StarlarkExpressionStatement>()
      .firstOrNull()
      ?.children
      ?.filterIsInstance<StarlarkCallExpression>()
      ?.firstOrNull()
      ?.getArgumentList()
      ?.children
      ?.filterIsInstance<StarlarkNamedArgumentExpression>()
}
