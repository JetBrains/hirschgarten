package org.jetbrains.bazel.commons.utils

enum class OsFamily {
  WINDOWS,
  MACOS,
  LINUX,
  ;

  companion object {
    fun inferFromSystem(): OsFamily {
      val osName = System.getProperty("os.name").lowercase()
      return when {
        osName.startsWith("windows") -> WINDOWS
        osName.startsWith("mac") -> MACOS
        else -> LINUX
      }
    }
  }
}
