package org.jetbrains.bazel.languages.projectview.lexer

import com.intellij.psi.tree.IElementType
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage

/**
 * A token type for the project view language.
 *
 * See the Project Views documentation for more information.
 *
 * @param debugName the name of the element type, used for debugging purposes.
 */
class ProjectViewTokenType private constructor(debugName: String) : IElementType(debugName, ProjectViewLanguage) {
  companion object {
    /**
     * A Project View section keyword.
     *
     * Defined as a sequence of characters that starts at the beginning of the line that is not a comment.
     * Its characters neither whitespace nor colons.
     */
    val KEYWORD = ProjectViewTokenType("keyword")

    /**
     * An identifier belonging to a Project View section.
     *
     * Cannot start at the beginning of the line.
     */
    val IDENTIFIER = ProjectViewTokenType("identifier")

    /** The character ':' */
    val COLON = ProjectViewTokenType("colon")

    /**
     * A comment starting with '#'. No non-white characters are present before the comment.
     */
    val COMMENT = ProjectViewTokenType("comment")

    /** A series of whitespaces at the beginning of a line NOT followed by a comment or '\n'. */
    val INDENT = ProjectViewTokenType("indent")

    /** The character '\n'. */
    val NEWLINE = ProjectViewTokenType("newline")

    /** A consecutive sequence of characters from the set { ' ', '\r', '\t' }. */
    val WHITESPACE = ProjectViewTokenType("whitespace")
  }

  override fun toString(): String = "ProjectView:" + super.toString()
}
