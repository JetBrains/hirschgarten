package org.jetbrains.bazel.languages.bazelquery.elements

import com.intellij.psi.tree.IElementType
import org.jetbrains.bazel.languages.bazelquery.BazelqueryLanguage
import org.jetbrains.bazel.languages.bazelquery.BazelqueryFlagsLanguage

class BazelqueryTokenType(debugName: String) : IElementType(debugName, BazelqueryLanguage) {
    override fun toString(): String = "Bazelquery:" + super.toString()
    fun completionText(): String = super.toString().lowercase()
}

class BazelqueryFlagsTokenType(debugName: String) : IElementType(debugName, BazelqueryFlagsLanguage) {
  override fun toString(): String = "Bazelquery:" + super.toString()
  fun completionText(): String = super.toString().lowercase()
}
