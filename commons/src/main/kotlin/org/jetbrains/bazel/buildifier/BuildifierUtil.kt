package org.jetbrains.bazel.buildifier

import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.openapi.util.SystemInfo
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
object BuildifierUtil {
  fun detectBuildifierExecutable(): String? {
    val extension =
      when {
        SystemInfo.isWindows -> ".exe"
        else -> ""
      }
    return PathEnvironmentVariableUtil.findInPath("buildifier$extension")
      ?.absolutePath
  }
}
