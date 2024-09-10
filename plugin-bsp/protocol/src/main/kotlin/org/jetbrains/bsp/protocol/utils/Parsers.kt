package org.jetbrains.bsp.protocol.utils

import ch.epfl.scala.bsp4j.BspConnectionDetails
import com.google.gson.Gson
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.openapi.vfs.readText

fun VirtualFile.parseBspConnectionDetails(): BspConnectionDetails? =
  try {
    if (isFile) Gson().fromJson(this.readText(), BspConnectionDetails::class.java) else null
  } catch (e: Exception) {
    thisLogger().warn("Parsing file '$this' to BspConnectionDetails failed!", e)
    null
  }
