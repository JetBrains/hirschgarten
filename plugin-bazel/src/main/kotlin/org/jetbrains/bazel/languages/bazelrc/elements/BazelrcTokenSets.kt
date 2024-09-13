package org.jetbrains.bazel.languages.bazelrc.elements

import com.intellij.psi.TokenType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenTypes.COMMAND
import org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenTypes.COMMENT
import org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenTypes.DOUBLE_QUOTE
import org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenTypes.SINGLE_QUOTE

object BazelrcTokenSets {
  val WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE)

  val COMMENTS = TokenSet.create(COMMENT)
  val COMMANDS = TokenSet.create(COMMAND)

  val QUOTES = TokenSet.create(DOUBLE_QUOTE, SINGLE_QUOTE)
}
