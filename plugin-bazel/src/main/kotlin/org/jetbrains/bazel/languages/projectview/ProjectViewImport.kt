package org.jetbrains.bazel.languages.projectview

object ProjectViewImport {
  sealed interface Parser {
    data object Import : Parser

    data object TryImport : Parser
  }

  val KEYWORD_MAP: Map<String, Parser> =
    mapOf(
      "import" to Parser.Import,
      "try_import" to Parser.TryImport,
    )
}
