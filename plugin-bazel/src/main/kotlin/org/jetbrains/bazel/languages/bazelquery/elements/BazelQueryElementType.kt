package org.jetbrains.bazel.languages.bazelquery.elements

import com.intellij.psi.tree.IElementType
import org.jetbrains.bazel.languages.bazelquery.BazelQueryLanguage

class BazelQueryElementType(debugName: String) : IElementType(debugName, BazelQueryLanguage)
