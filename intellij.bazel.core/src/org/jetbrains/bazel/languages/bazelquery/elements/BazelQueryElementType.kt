package org.jetbrains.bazel.languages.bazelquery.elements

import com.intellij.psi.tree.IElementType
import org.jetbrains.bazel.languages.bazelquery.BazelQueryFlagsLanguage
import org.jetbrains.bazel.languages.bazelquery.BazelQueryLanguage

internal class BazelQueryElementType(debugName: String) : IElementType(debugName, BazelQueryLanguage)

internal class BazelQueryFlagsElementType(debugName: String) : IElementType(debugName, BazelQueryFlagsLanguage)
