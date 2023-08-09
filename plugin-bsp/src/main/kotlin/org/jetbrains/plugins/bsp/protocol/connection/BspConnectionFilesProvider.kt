package org.jetbrains.plugins.bsp.protocol.connection

import com.intellij.openapi.vfs.VirtualFile

public class BspConnectionFilesProvider(projectPath: VirtualFile) {
  public val connectionFiles: List<LocatedBspConnectionDetails> by lazy { calculateConnectionFile(projectPath) }

  private fun calculateConnectionFile(projectPath: VirtualFile): List<LocatedBspConnectionDetails> =
    projectPath.findChild(dotBspDir)
      ?.children
      ?.mapNotNull { LocatedBspConnectionDetailsParser.parseFromFile(it) }
      .orEmpty()

  public fun isAnyBspConnectionFileDefined(): Boolean = connectionFiles.isNotEmpty()

  private companion object {
    private const val dotBspDir = ".bsp"
  }
}
