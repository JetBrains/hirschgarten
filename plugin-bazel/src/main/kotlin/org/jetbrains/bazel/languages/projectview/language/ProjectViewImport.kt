package org.jetbrains.bazel.languages.projectview.language

object ProjectViewImport {
  sealed interface Parser {
    data object Import : Parser

    data object TryImport : Parser
  }

  val KEYWORD_MAP: Map<ProjectViewSyntaxKey, Parser> =
    mapOf(
      "import" to Parser.Import,
      "try_import" to Parser.TryImport,
    )
}
