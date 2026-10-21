package org.jetbrains.bazel.languages.bazelrc.elements

import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.bazelrc.BazelrcLanguage

@ApiStatus.Internal
class BazelrcTokenType(debugName: String) : IElementType(debugName, BazelrcLanguage) {
  override fun toString(): String = "Bazelrc:" + super.toString()
}
