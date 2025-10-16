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
    /** The character ':' */
    val COLON = ProjectViewTokenType("colon")

    /**
     * A comment starting with '#'.
     *
     * No non-white characters are present before the comment.
     */
    val COMMENT = ProjectViewTokenType("comment")

    /** A sequence of characters that are neither whitespace nor colon nor comment. */
    val IDENTIFIER = ProjectViewTokenType("identifier")

    /** A series of whitespaces at the beginning of a line NOT followed by a comment or '\n'. */
    val INDENT = ProjectViewTokenType("indent")

    /** An identifier that is a section keyword. */
    val SECTION_KEYWORD = ProjectViewTokenType("section_keyword")

    /** An identifier that is an import keyword. */
    val IMPORT_KEYWORD = ProjectViewTokenType("import_keyword")

    /** An identifier that is an try_import keyword. */
    val TRY_IMPORT_KEYWORD = ProjectViewTokenType("try_import_keyword")

    /** The character '\n'. */
    val NEWLINE = ProjectViewTokenType("newline")

    /** A consecutive sequence of characters from the set { ' ', '\r', '\t' }. */
    val WHITESPACE = ProjectViewTokenType("whitespace")
  }

  override fun toString(): String = "ProjectView:" + super.toString()
}
