package org.jetbrains.bazel.languages.starlark.references

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkGlobExpression

class StarlarkGlobReference(element: StarlarkGlobExpression)
  : PsiPolyVariantReferenceBase<StarlarkGlobExpression>(element), PsiPolyVariantReference {
  override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult?> {
    println("Resolving")
    return ResolveResult.EMPTY_ARRAY
  }

  override fun resolve(): PsiElement? {
    println("Resolving")
    return null
  }
}
