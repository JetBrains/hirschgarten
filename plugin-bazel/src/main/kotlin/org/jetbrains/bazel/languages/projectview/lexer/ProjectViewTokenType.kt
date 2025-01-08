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
     * An inline comment.
     *
     * Starts with '#'. No non-white characters are present before the comment.
     */
    val COMMENT = ProjectViewTokenType("comment")

    /** A sequence of characters that are neither whitespace nor colon nor comment. */
    val IDENTIFIER = ProjectViewTokenType("identifier")

    /** A series of whitespaces at the beginning of a line NOT followed by a comment or '\n'. */
    val INDENT = ProjectViewTokenType("indent")

    /** An identifier that is a section keyword followed by a list of identifier. */
    val LIST_KEYWORD = ProjectViewTokenType("list_keyword")

    /** The character '\n'. */
    val NEWLINE = ProjectViewTokenType("newline")

    /** An identifier that is a section keyword followed by a single identifier. */
    val SCALAR_KEYWORD = ProjectViewTokenType("scalar_keyword")

    /** A consecutive sequence of characters from the set { ' ', '\r', '\t' }. */
    val WHITESPACE = ProjectViewTokenType("whitespace")

    /** Possible values of list keywords. */
    val LIST_KEYWORDS_SET =
      setOf(
        "additional_languages",
        "build_flags",
        "directories",
        "exclude_library",
        "exclude_target",
        "excluded_libraries",
        "excluded_sources",
        "import_run_configurations",
        "import_target_output",
        "sync_flags",
        "targets",
        "test_flags",
        "test_sources",
      )

    /** Possible values of scalar keywords. */
    val SCALAR_KEYWORDS_SET =
      setOf(
        "allow_manual_targets_sync",
        "bazel_binary",
        "derive_targets_from_directories",
        "import",
        "java_language_level",
        "shard_sync",
        "target_shard_size",
        "try_import",
        "use_exclusion_patterns",
        "use_query_sync",
        "view_project_root",
        "workspace_buildrc_file",
        "workspace_type",
      )
  }

  override fun toString(): String = "ProjectView:" + super.toString()
}
