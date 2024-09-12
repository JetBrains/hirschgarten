package org.jetbrains.plugins.bsp.utils

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import java.net.URI

private val SOURCE_EXTENSIONS = listOf("java", "kt", "scala", "py")

fun VirtualFile.isSourceFile(): Boolean {
  val isFile =
    try {
      this.isFile
    } catch (_: UnsupportedOperationException) {
      false
    }
  return isFile && extension?.lowercase() in SOURCE_EXTENSIONS
}

fun URI.isSourceFile(): Boolean = SOURCE_EXTENSIONS.any { path.endsWith(".$it") }
