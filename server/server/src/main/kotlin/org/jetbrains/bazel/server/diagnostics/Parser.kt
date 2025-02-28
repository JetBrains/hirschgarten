package org.jetbrains.bazel.server.diagnostics

interface Parser {
  fun tryParse(output: Output): List<Diagnostic>
}
