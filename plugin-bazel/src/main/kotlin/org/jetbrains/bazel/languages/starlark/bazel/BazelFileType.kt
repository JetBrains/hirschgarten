package org.jetbrains.bazel.languages.starlark.bazel

enum class BazelFileType {
  BUILD,
  EXTENSION,
  MODULE,
  WORKSPACE,
  ;

  companion object {
    val ofFileName =
      mapOf(
        "BUILD.bazel" to BUILD,
        "BUILD" to BUILD,
        "MODULE.bazel" to MODULE,
        "WORKSPACE.bazel" to WORKSPACE,
        "WORKSPACE" to WORKSPACE,
      )
  }
}
