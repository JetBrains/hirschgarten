package org.jetbrains.bazel.languages.starlark.psi.functions

import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
interface StarlarkCallable: PsiElement {
  fun getNamedParameters(): List<StarlarkParameter>

  fun getParameters(): List<StarlarkParameter>
}
