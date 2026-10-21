package org.jetbrains.bazel.languages.bazelquery.elements

import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.bazelquery.BazelQueryLanguage

@ApiStatus.Internal
class BazelQueryTokenType(debugName: String) : IElementType(debugName, BazelQueryLanguage) {
  override fun toString(): String = "BazelQuery:" + super.toString()

  fun completionText(): String = super.toString().lowercase()
}
