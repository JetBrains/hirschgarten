package org.jetbrains.bazel.languages.bazelquery.elements

import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.bazelquery.BazelQueryFlagsLanguage
import org.jetbrains.bazel.languages.bazelquery.BazelQueryLanguage

@ApiStatus.Internal
class BazelQueryElementType(debugName: String) : IElementType(debugName, BazelQueryLanguage)

@ApiStatus.Internal
class BazelQueryFlagsElementType(debugName: String) : IElementType(debugName, BazelQueryFlagsLanguage)
