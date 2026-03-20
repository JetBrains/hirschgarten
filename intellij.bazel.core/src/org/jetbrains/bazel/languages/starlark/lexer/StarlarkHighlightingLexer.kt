package org.jetbrains.bazel.languages.starlark.lexer

import com.intellij.lexer.FlexAdapter

internal class StarlarkHighlightingLexer : FlexAdapter(_StarlarkLexer(null))
