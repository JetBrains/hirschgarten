package org.jetbrains.bazel.languages.starlark.lexer

import com.intellij.lexer.FlexAdapter
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.lexer.generated._StarlarkLexer

@ApiStatus.Internal
class StarlarkHighlightingLexer : FlexAdapter(_StarlarkLexer(null))
