package org.jetbrains.bsp.bazel.commons

import org.jetbrains.bazel.commons.utils.OsFamily
import java.io.File

object FileUtils {
  fun getCacheDirectory(subfolder: String): File? {
    val path =
      System.getenv("XDG_CACHE_HOME") ?: run {
        val os = OsFamily.inferFromSystem()
        when (os) {
          OsFamily.WINDOWS ->
            System.getenv("LOCALAPPDATA") ?: System.getenv("APPDATA")

          OsFamily.LINUX ->
            System.getenv("HOME") + "/.cache"

          OsFamily.MACOS ->
            System.getenv("HOME") + "/Library/Caches"
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
