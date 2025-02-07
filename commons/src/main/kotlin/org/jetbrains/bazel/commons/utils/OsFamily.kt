package org.jetbrains.bazel.commons.utils

enum class OsFamily {
  WINDOWS,
  MACOS,
  LINUX,
  ;

  companion object {
    fun inferFromSystem(): OsFamily =
      System.getProperty("os.name").lowercase().let { osName ->
        when {
          osName.startsWith("windows") -> WINDOWS
          osName.startsWith("mac") -> MACOS
          else -> LINUX
        }
      }
  }
}
