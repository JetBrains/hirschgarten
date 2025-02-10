package org.jetbrains.bazel.languages.bazelquery.elements

import com.intellij.psi.tree.IElementType
import org.jetbrains.bazel.languages.bazelquery.BazelqueryLanguage

class BazelqueryTokenType(debugName: String) : IElementType(debugName, BazelqueryLanguage) {
    override fun toString(): String = "Bazelquery:" + super.toString()
    fun completionText(): String = super.toString().lowercase()
}
