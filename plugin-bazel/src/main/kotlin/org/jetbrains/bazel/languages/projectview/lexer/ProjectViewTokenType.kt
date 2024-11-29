package org.jetbrains.bazel.languages.projectview.lexer

import com.intellij.psi.tree.IElementType
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage

class ProjectViewTokenType private constructor(debugName: String) : IElementType(debugName, ProjectViewLanguage) {
  companion object {
    val COLON = ProjectViewTokenType("colon")
    val COMMENT = ProjectViewTokenType("comment")
    val IDENTIFIER = ProjectViewTokenType("identifier")
    val INDENT = ProjectViewTokenType("indent")
    val LIST_KEYWORD = ProjectViewTokenType("list_keyword")
    val NEWLINE = ProjectViewTokenType("newline")
    val SCALAR_KEYWORD = ProjectViewTokenType("scalar_keyword")
    val WHITESPACE = ProjectViewTokenType("whitespace")

    /** Keywords that are followed by a single identifier. */
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

    /** Keywords that are followed by multiple identifiers. */
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
  }

  override fun toString(): String = "ProjectView:" + super.toString()
}
