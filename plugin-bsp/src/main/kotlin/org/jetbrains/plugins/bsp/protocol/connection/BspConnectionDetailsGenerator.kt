package org.jetbrains.plugins.bsp.protocol.connection

import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.bsp.flow.open.wizzard.ConnectionFileOrNewConnection
import org.jetbrains.plugins.bsp.flow.open.wizzard.ImportProjectWizzardStep
import java.io.OutputStream
import java.nio.file.Path

public interface BspConnectionDetailsGenerator {
  public fun executeAndWait(command: String, projectPath: VirtualFile, outputStream: OutputStream) {
    // TODO - consider verbosing what command is being executed
    val consoleProcess = Runtime.getRuntime().exec(
      command,
      emptyArray(),
      projectPath.toNioPath().toFile()
    )
    consoleProcess.inputStream.transferTo(outputStream)
    consoleProcess.waitFor()
  }

  public fun getChild(root: VirtualFile?, path: List<String>): VirtualFile? {
    val found: VirtualFile? = path.fold(root) { vf: VirtualFile?, child: String ->
      vf?.refresh(false, false)
      vf?.findChild(child)
    }
    found?.refresh(false, false)
    return found
  }

  public fun name(): String

  public fun canGenerateBspConnectionDetailsFile(projectPath: VirtualFile): Boolean

  public fun calculateImportWizzardSteps(
    projectBasePath: Path,
    connectionFileOrNewConnectionProperty: ObservableMutableProperty<ConnectionFileOrNewConnection>
  ): List<ImportProjectWizzardStep> = emptyList()

  public fun generateBspConnectionDetailsFile(projectPath: VirtualFile, outputStream: OutputStream): VirtualFile
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

  public fun firstGeneratorTEMPORARY(): String? =
    availableGeneratorsNames().firstOrNull()

  public fun calulateWizzardSteps(
    generatorName: String,
    connectionFileOrNewConnectionProperty: ObservableMutableProperty<ConnectionFileOrNewConnection>
  ): List<ImportProjectWizzardStep> =
    availableBspConnectionDetailsGenerators
      .find { it.name() == generatorName }
      ?.calculateImportWizzardSteps(projectPath.toNioPath(), connectionFileOrNewConnectionProperty).orEmpty()

  public fun generateBspConnectionDetailFileForGeneratorWithName(
    generatorName: String,
    outputStream: OutputStream
  ): VirtualFile? =
    availableBspConnectionDetailsGenerators
      .find { it.name() == generatorName }
      ?.generateBspConnectionDetailsFile(projectPath, outputStream)
}
