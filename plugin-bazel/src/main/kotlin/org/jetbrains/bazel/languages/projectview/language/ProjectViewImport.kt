package org.jetbrains.bazel.languages.projectview.language

sealed interface ProjectViewImport {
  fun parse(path: String): Result<String>

  private sealed interface SimpleImport : ProjectViewImport {
    override fun parse(path: String): Result<String> =
      if (path == "") {
        Result.failure(Exception("Path missing"))
      } else {
        Result.success(path)
      }
  }

  data object Import : SimpleImport

  data object TryImport : SimpleImport

  companion object {
    val KEYWORD_MAP: Map<ProjectViewSyntaxKey, ProjectViewImport> =
      mapOf(
        "import" to Import,
        "try_import" to TryImport,
      )
  }

  sealed interface ParsingResult<out T> {
    data class Ok<out T>(val result: T) : ParsingResult<T>

    data class Error(val message: String) : ParsingResult<Nothing>
  }
}
