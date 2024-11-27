package org.jetbrains.bazel.languages.projectview.lexer

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage

class ProjectViewTokenType private constructor(debugName: String) : IElementType(debugName, ProjectViewLanguage) {
  companion object {
    val COMMENT = ProjectViewTokenType("comment")
    val WHITESPACE = ProjectViewTokenType("whitespace")
    val NEWLINE = ProjectViewTokenType("newline")
    val COLON = ProjectViewTokenType(":")
    val INDENT = ProjectViewTokenType("indent")
    val IDENTIFIER = ProjectViewTokenType("identifier")
    val LIST_KEYWORD = ProjectViewTokenType("list_keyword")
    val SCALAR_KEYWORD = ProjectViewTokenType("scalar_keyword")

    val IDENTIFIERS = TokenSet.create(IDENTIFIER, LIST_KEYWORD, SCALAR_KEYWORD)
  }

  override fun toString(): String = "ProjectView:" + super.toString()
}
