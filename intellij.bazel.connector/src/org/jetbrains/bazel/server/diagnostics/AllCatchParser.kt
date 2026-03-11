package org.jetbrains.bazel.server.diagnostics

internal object AllCatchParser : Parser {
  override fun tryParse(output: Output): List<Diagnostic> {
    output.tryTake()
    return emptyList()
  }
}
