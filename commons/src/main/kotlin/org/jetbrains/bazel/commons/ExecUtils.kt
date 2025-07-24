package org.jetbrains.bazel.commons

object ExecUtils {
  fun calculateExecutableName(name: String): String {
    val systemInfoProvider = SystemInfoProvider.getInstance()
    return when {
      systemInfoProvider.isWindows -> "$name.exe"
      else -> name
    }
  }
}
