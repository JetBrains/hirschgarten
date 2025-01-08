package org.jetbrains.bsp.protocol.utils

import ch.epfl.scala.bsp4j.BspConnectionDetails
import com.google.gson.Gson
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import org.jetbrains.bsp.protocol.BSP_CONNECTION_DIR
import kotlin.io.path.bufferedReader

fun VirtualFile.parseBspConnectionDetails(): BspConnectionDetails? {
  try {
    if (!isBspConnectionFile()) return null
    return this.toNioPath().bufferedReader().use { reader ->
      Gson().fromJson(reader, BspConnectionDetails::class.java)
    }
  } catch (e: Exception) {
    thisLogger().warn("Parsing file '$this' to BspConnectionDetails failed!", e)
    return null
  }
}

private fun VirtualFile.isBspConnectionFile() = isFile && extension == "json" && parent?.name == BSP_CONNECTION_DIR
