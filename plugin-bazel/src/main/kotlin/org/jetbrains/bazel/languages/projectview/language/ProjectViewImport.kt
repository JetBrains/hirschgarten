package org.jetbrains.bazel.languages.projectview.language

sealed interface ProjectViewImport {
  fun parse(path: String): Result<String> =
    if (path == "") {
      Result.failure(Exception("Path missing"))
    } else {
      Result.success(path)
    }

  data object Import : ProjectViewImport

  data object TryImport : ProjectViewImport

  companion object {
    val KEYWORD_MAP: Map<ProjectViewSyntaxKey, ProjectViewImport> =
      mapOf(
        "import" to Import,
        "try_import" to TryImport,
      )
  }
}
