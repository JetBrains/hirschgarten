package org.jetbrains.bazel.languages.bazelrc.elements

import com.intellij.psi.tree.IElementType
import org.jetbrains.bazel.languages.bazelrc.BazelrcLanguage

class BazelrcElementType(debugName: String) : IElementType(debugName, BazelrcLanguage)
