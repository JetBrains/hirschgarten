package org.jetbrains.bazel.commons

import com.intellij.openapi.util.SystemInfo
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
object ExecUtils {
  fun calculateExecutableName(name: String): String {
    return when {
      SystemInfo.isWindows -> "$name.exe"
      else -> name
    }
  }
}
