package org.jetbrains.bazel.languages.projectview.elements

import com.intellij.psi.tree.TokenSet
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewTokenType

object ProjectViewTokenSets {
  val WHITESPACE =
    TokenSet.create(
      ProjectViewTokenType.WHITESPACE,
      ProjectViewTokenType.NEWLINE,
      ProjectViewTokenType.INDENT,
    )

  val IDENTIFIER = TokenSet.create(ProjectViewTokenType.IDENTIFIER)

  val COMMENT = TokenSet.create(ProjectViewTokenType.COMMENT)
}
