package org.jetbrains.bazel.commons

import com.intellij.util.EnvironmentUtil
import org.jetbrains.bazel.commons.utils.OsFamily
import java.io.File

object FileUtils {
  fun getCacheDirectory(subfolder: String): File? {
    val path =
      EnvironmentUtil.getValue("XDG_CACHE_HOME") ?: run {
        val os = OsFamily.inferFromSystem()
        when (os) {
          OsFamily.WINDOWS ->
            EnvironmentUtil.getValue("LOCALAPPDATA") ?: EnvironmentUtil.getValue("APPDATA")

          OsFamily.LINUX ->
            EnvironmentUtil.getValue("HOME")?.let { "$it/.cache" }

          OsFamily.MACOS ->
            EnvironmentUtil.getValue("HOME")?.let { "$it/Library/Caches" }
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
