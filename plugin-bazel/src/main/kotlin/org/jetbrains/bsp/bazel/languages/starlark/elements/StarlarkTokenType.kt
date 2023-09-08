package org.jetbrains.bsp.bazel.languages.starlark.elements

import com.intellij.psi.tree.IElementType
import org.jetbrains.bsp.bazel.languages.starlark.StarlarkLanguage

class StarlarkTokenType(debugName: String) : IElementType(debugName, StarlarkLanguage) {
  override fun toString(): String = "Starlark:" + super.toString()
}
