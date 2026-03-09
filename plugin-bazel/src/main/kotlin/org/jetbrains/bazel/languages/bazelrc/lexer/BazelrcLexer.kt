package org.jetbrains.bazel.languages.bazelrc.lexer

import com.intellij.lexer.FlexAdapter
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
class BazelrcLexer : FlexAdapter(_BazelrcLexer(null))
