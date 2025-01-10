package org.jetbrains.bazel.commons.utils

enum class OsArch {
  X64,
  ARM64,
  UNKNOWN,
  ;

  companion object {
    fun inferFromSystem(): OsArch =
      System.getProperty("os.arch").lowercase().let { arch ->
        when (arch) {
          "x86_64", "amd64" -> X64
          "aarch64", "arm64" -> ARM64
          else -> UNKNOWN
        }
      }
  }
}
