package org.jetbrains.bazel.languages.starlark.lexer

import com.intellij.lexer.FlexAdapter
import org.jetbrains.bazel.languages.starlark.lexer._StarlarkLexer

class StarlarkHighlightingLexer : FlexAdapter(
  _StarlarkLexer(
    null
  )
)