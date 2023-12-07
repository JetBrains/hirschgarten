package org.jetbrains.bsp.utils

import ch.epfl.scala.bsp4j.BspConnectionDetails
import com.google.gson.Gson
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText

public fun VirtualFile.parseBspConnectionDetails(): BspConnectionDetails? =
  try {
    Gson().fromJson(this.readText(), BspConnectionDetails::class.java)
  } catch (e: Exception) {
    thisLogger().warn("Parsing file '$this' to BspConnectionDetails failed!", e)
    null
  }
