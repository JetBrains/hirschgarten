package org.jetbrains.bazel.languages.starlark.psi.statements

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.StarlarkUtils.nearestRelevantChildAfter
import org.jetbrains.bazel.languages.starlark.StarlarkUtils.nearestRelevantBeforeOperator

@ApiStatus.Internal
interface StarlarkAssigningStatement: PsiElement {
  fun operator(): IElementType
  fun leftHandSide(): PsiElement? = nearestRelevantBeforeOperator(firstChild)
  fun rightHandSide(): PsiElement? = nearestRelevantChildAfter(this, operator())
}
