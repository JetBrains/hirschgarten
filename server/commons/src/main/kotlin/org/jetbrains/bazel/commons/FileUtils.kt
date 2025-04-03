package org.jetbrains.bazel.commons

import com.intellij.openapi.util.SystemInfo
import com.intellij.util.EnvironmentUtil
import java.io.File

object FileUtils {
  fun getCacheDirectory(subfolder: String): File? {
    val path =
      EnvironmentUtil.getValue("XDG_CACHE_HOME") ?: run {
        when {
          SystemInfo.isWindows ->
            EnvironmentUtil.getValue("LOCALAPPDATA") ?: EnvironmentUtil.getValue("APPDATA")

          SystemInfo.isMac ->
            EnvironmentUtil.getValue("HOME")?.let { "$it/Library/Caches" }

          else ->
            EnvironmentUtil.getValue("HOME")?.let { "$it/.cache" }
        }
      }
    val file = File(path, subfolder)
    try {
      file.mkdirs()
    } catch (_: Exception) {
      return null
    }
    if (!file.exists() || !file.isDirectory) {
      return null
    }
    return file
  }
}
