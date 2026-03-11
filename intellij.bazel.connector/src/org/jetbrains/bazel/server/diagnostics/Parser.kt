package org.jetbrains.bazel.server.diagnostics

internal interface Parser {
  fun tryParse(output: Output): List<Diagnostic>
}
