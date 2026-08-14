package org.jetbrains.bazel.server.diagnostics

internal interface Parser {
  fun tryParse(output: Output): List<Diagnostic>

  companion object {
    // regex part to match the path
    const val PATH_PART = """(?:[a-zA-z]:[\/\\])?[^:\r\n]+"""
  }
}
