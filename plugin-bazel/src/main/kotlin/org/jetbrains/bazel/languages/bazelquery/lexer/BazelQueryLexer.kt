package org.jetbrains.bazel.languages.bazelquery.lexer

import com.intellij.lexer.FlexAdapter
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
class BazelQueryLexer : FlexAdapter(_BazelQueryLexer(null))
