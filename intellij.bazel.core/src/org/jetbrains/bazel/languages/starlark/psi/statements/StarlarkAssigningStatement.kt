package org.jetbrains.bazel.languages.starlark.psi.statements

import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.StarlarkUtils.nearestRelevantChildAfter
import org.jetbrains.bazel.languages.starlark.StarlarkUtils.nearestRelevantBeforeOperator
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenType

@ApiStatus.Internal
interface StarlarkAssigningStatement: PsiElement {
  fun operator(): StarlarkTokenType
  fun leftHandSide(): PsiElement? = nearestRelevantBeforeOperator(firstChild)
  fun rightHandSide(): PsiElement? = nearestRelevantChildAfter(this, operator())
}
