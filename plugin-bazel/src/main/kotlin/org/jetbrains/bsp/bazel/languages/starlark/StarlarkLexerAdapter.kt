package org.jetbrains.bsp.bazel.languages.starlark

import com.intellij.lexer.FlexAdapter

object StarlarkLexerAdapter : FlexAdapter(StarlarkLexer(null))
