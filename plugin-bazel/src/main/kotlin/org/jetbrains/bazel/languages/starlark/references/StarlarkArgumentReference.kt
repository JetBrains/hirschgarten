package org.jetbrains.bazel.languages.starlark.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.bazel.languages.starlark.completion.lookups.StarlarkLookupElement
import org.jetbrains.bazel.languages.starlark.completion.lookups.StarlarkParameterLookupElement
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentElement
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration

open class StarlarkArgumentReference(element: StarlarkArgumentElement, range: TextRange) :
  PsiReferenceBase<StarlarkArgumentElement>(element, range, true) {
  override fun resolve(): PsiElement? = null

  override fun getVariants(): Array<StarlarkLookupElement> {
    val originalFunction = resolveFunction() ?: return emptyArray()
    val passedArgumentNames = getArgumentNames()
    return originalFunction
      .getNamedParameters()
      .filterNot { passedArgumentNames.contains(it.name) }
      .map { StarlarkParameterLookupElement(it) }
      .toTypedArray()
  }

  protected fun resolveFunction(): StarlarkFunctionDeclaration? = getFunctionCall()?.reference?.resolve() as? StarlarkFunctionDeclaration

  private fun getArgumentNames(): Set<String> = getFunctionCall()?.getArgumentList()?.getArgumentNames() ?: emptySet()

  private fun getFunctionCall(): StarlarkCallExpression? = PsiTreeUtil.getParentOfType(element, StarlarkCallExpression::class.java)
}
