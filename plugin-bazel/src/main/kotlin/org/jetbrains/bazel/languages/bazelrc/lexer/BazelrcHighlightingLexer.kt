package org.jetbrains.bazel.languages.bazelrc.lexer

import com.intellij.lexer.FlexAdapter

class BazelrcHighlightingLexer : FlexAdapter(_BazelrcLexer(null))
