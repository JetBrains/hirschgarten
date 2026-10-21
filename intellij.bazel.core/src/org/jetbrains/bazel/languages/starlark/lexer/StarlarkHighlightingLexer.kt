package org.jetbrains.bazel.languages.starlark.lexer

import com.intellij.lexer.FlexAdapter
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
class StarlarkHighlightingLexer : FlexAdapter(_StarlarkLexer(null))
