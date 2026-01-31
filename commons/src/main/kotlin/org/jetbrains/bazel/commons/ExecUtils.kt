package org.jetbrains.bazel.commons

import com.intellij.openapi.util.SystemInfo

object ExecUtils {
  fun calculateExecutableName(name: String): String {
    return when {
      SystemInfo.isWindows -> "$name.exe"
      else -> name
    }
  }
}
