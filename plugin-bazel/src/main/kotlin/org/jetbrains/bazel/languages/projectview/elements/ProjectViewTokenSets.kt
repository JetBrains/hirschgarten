package org.jetbrains.bazel.languages.projectview.elements

import com.intellij.psi.tree.TokenSet

object ProjectViewTokenSets {
  val WHITESPACE =
    TokenSet.create(
      ProjectViewTokenTypes.WHITESPACE,
      ProjectViewTokenTypes.NEWLINE,
      ProjectViewTokenTypes.INDENT,
    )

  val IDENTIFIER = TokenSet.create(ProjectViewTokenTypes.IDENTIFIER)

  val COMMENT = TokenSet.create(ProjectViewTokenTypes.COMMENT)
}
