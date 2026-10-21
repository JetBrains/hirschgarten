package org.jetbrains.bazel.languages.starlark.references

import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement

@ApiStatus.Internal
interface StarlarkLocalVariableElement : StarlarkElement {
  fun getNameIdentifier(): PsiElement?
}
