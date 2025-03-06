package org.jetbrains.bazel.languages.bazelquery.elements

import com.intellij.psi.tree.IElementType
import org.jetbrains.bazel.languages.bazelquery.BazelqueryLanguage
import org.jetbrains.bazel.languages.bazelquery.BazelqueryFlagsLanguage

class BazelqueryElementType(debugName: String) : IElementType(debugName, BazelqueryLanguage)

class BazelqueryFlagsElementType(debugName: String) : IElementType(debugName, BazelqueryFlagsLanguage)
