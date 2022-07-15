package org.jetbrains.protocol.connection

import com.intellij.openapi.vfs.VirtualFile

public interface BspConnectionDetailsGenerator {

  public fun name(): String

  public fun canGenerateBspConnectionDetailsFile(projectPath: VirtualFile): Boolean

  public fun generateBspConnectionDetailsFile(projectPath: VirtualFile): VirtualFile
}

public class BspConnectionDetailsGeneratorProvider(
  private val projectPath: VirtualFile,
  bspConnectionDetailsGenerators: List<BspConnectionDetailsGenerator>,
) {

  private val availableBspConnectionDetailsGenerators by lazy {
    bspConnectionDetailsGenerators.filter { it.canGenerateBspConnectionDetailsFile(projectPath) }
  }

  public fun canGenerateAnyBspConnectionDetailsFile(): Boolean =
    availableBspConnectionDetailsGenerators.isNotEmpty()

  public fun availableGeneratorsNames(): List<String> =
    availableBspConnectionDetailsGenerators.map { it.name() }

  public fun generateBspConnectionDetailFileForGeneratorWithName(generatorName: String): VirtualFile? =
    availableBspConnectionDetailsGenerators
      .find { it.name() == generatorName }
      ?.generateBspConnectionDetailsFile(projectPath)
}
