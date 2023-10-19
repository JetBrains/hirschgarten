package org.jetbrains.bsp.bazel.languages.starlark.lexer

import com.intellij.lexer.FlexAdapter

class StarlarkHighlightingLexer : FlexAdapter(_StarlarkLexer(null))