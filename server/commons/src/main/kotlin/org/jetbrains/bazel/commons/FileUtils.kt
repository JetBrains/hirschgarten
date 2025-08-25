package org.jetbrains.bazel.commons

import java.io.File

object FileUtils {
  fun getCacheDirectory(subfolder: String): File? {
    val environmentProvider = EnvironmentProvider.getInstance()
    val systemInfoProvider = SystemInfoProvider.getInstance()
    val path =

      environmentProvider.getValue("XDG_CACHE_HOME") ?: run {
        when {
          systemInfoProvider.isWindows ->
            environmentProvider.getValue("LOCALAPPDATA") ?: environmentProvider.getValue("APPDATA")

          systemInfoProvider.isMac ->
            environmentProvider.getValue("HOME")?.let { "$it/Library/Caches" }

          else ->
            environmentProvider.getValue("HOME")?.let { "$it/.cache" }
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
