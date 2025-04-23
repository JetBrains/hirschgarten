package org.jetbrains.bazel.languages.starlark.bazel

enum class BazelFileType {
  BUILD,
  EXTENSION,
  MODULE,
  WORKSPACE,
  ;

  companion object {
    fun ofFileName(name: String): BazelFileType = ofFileName.getOrDefault(name, EXTENSION)

    private val ofFileName =
      mapOf(
        "BUILD.bazel" to BUILD,
        "BUILD" to BUILD,
        "MODULE.bazel" to MODULE,
        "WORKSPACE.bazel" to WORKSPACE,
        "WORKSPACE" to WORKSPACE,
      )
  }
}
