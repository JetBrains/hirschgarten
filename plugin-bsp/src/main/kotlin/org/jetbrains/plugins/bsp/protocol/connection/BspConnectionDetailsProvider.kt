package org.jetbrains.plugins.bsp.protocol.connection

import com.intellij.openapi.vfs.VirtualFile

public class BspConnectionDetailsProvider(
  private val bspConnectionDetailsGenerators: List<BspConnectionDetailsGenerator>
) {

  private lateinit var bspConnectionFilesProvider: BspConnectionFilesProvider

  private lateinit var bspConnectionDetailsGeneratorProvider: BspConnectionDetailsGeneratorProvider

  public fun canOpenBspProject(projectPath: VirtualFile): Boolean {
    initProvidersIfNeeded(projectPath)

    return bspConnectionFilesProvider.isAnyBspConnectionFileDefined()
  }


  private fun initProvidersIfNeeded(projectPath: VirtualFile) {
    if (!::bspConnectionFilesProvider.isInitialized) {
      bspConnectionFilesProvider = BspConnectionFilesProvider(projectPath)
    }

    if (!::bspConnectionDetailsGeneratorProvider.isInitialized) {
      bspConnectionDetailsGeneratorProvider =
        BspConnectionDetailsGeneratorProvider(projectPath, bspConnectionDetailsGenerators)
    }
  }
}
