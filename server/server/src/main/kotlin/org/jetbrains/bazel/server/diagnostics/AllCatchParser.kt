package org.jetbrains.bazel.server.diagnostics

object AllCatchParser : Parser {
  override fun tryParse(output: Output): List<Diagnostic> {
    output.tryTake()
    return emptyList()
  }
}
