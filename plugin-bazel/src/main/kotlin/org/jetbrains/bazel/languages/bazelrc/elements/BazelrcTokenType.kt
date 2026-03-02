package org.jetbrains.bazel.languages.bazelrc.elements

import com.intellij.psi.tree.IElementType
import org.jetbrains.bazel.languages.bazelrc.BazelrcLanguage

internal class BazelrcTokenType(debugName: String) : IElementType(debugName, BazelrcLanguage) {
  override fun toString(): String = "Bazelrc:" + super.toString()
}
