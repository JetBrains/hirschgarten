package org.jetbrains.plugins.bsp.protocol.connection

import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.bsp.flow.open.wizard.ConnectionFileOrNewConnection
import org.jetbrains.plugins.bsp.flow.open.wizard.ImportProjectWizardStep
import org.jetbrains.plugins.bsp.services.BspCoroutineService
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.console.TaskConsole
import org.jetbrains.plugins.bsp.utils.withRealEnvs
import java.io.OutputStream
import java.nio.file.Path

public interface BspConnectionDetailsGenerator {
  public fun executeAndWait(command: List<String>, projectPath: VirtualFile, outputStream: OutputStream) {
    // TODO - consider verbosing what command is being executed
    val builder = ProcessBuilder(command)
      .directory(projectPath.toNioPath().toFile())
      .withRealEnvs()
      .redirectError(ProcessBuilder.Redirect.PIPE)

    val consoleProcess = builder.start()
    consoleProcess.inputStream.transferTo(outputStream)
    consoleProcess.logErrorOutputs(projectPath)
    consoleProcess.waitFor()
    if (consoleProcess.exitValue() != 0) {
      error(
        "An error has occurred while running the command: ${command.joinToString(" ")}"
      )
    }
  }

  public fun getChild(root: VirtualFile?, path: List<String>): VirtualFile? {
    val found: VirtualFile? = path.fold(root) { vf: VirtualFile?, child: String ->
      vf?.refresh(false, false)
      vf?.findChild(child)
    }
    found?.refresh(false, false)
    return found
  }

  public fun id(): String

  public fun displayName(): String

  public fun canGenerateBspConnectionDetailsFile(projectPath: VirtualFile): Boolean

  public fun calculateImportWizardSteps(
    projectBasePath: Path,
    connectionFileOrNewConnectionProperty: ObservableMutableProperty<ConnectionFileOrNewConnection>
  ): List<ImportProjectWizardStep> = emptyList()

  public fun generateBspConnectionDetailsFile(projectPath: VirtualFile, outputStream: OutputStream): VirtualFile
}

public class BspConnectionDetailsGeneratorProvider(
  private val projectPath: VirtualFile,
  bspConnectionDetailsGenerators: List<BspConnectionDetailsGenerator>,
) {

  public val availableBspConnectionDetailsGenerators: List<BspConnectionDetailsGenerator> by lazy {
    bspConnectionDetailsGenerators.filter { it.canGenerateBspConnectionDetailsFile(projectPath) }
  }

  public fun canGenerateAnyBspConnectionDetailsFile(): Boolean =
    availableBspConnectionDetailsGenerators.isNotEmpty()

  public fun availableGeneratorsNames(): List<String> =
    availableBspConnectionDetailsGenerators.map { it.id() }

  public fun calculateWizardSteps(
    generatorId: String,
    connectionFileOrNewConnectionProperty: ObservableMutableProperty<ConnectionFileOrNewConnection>
  ): List<ImportProjectWizardStep> =
    availableBspConnectionDetailsGenerators
      .find { it.id() == generatorId }
      ?.calculateImportWizardSteps(projectPath.toNioPath(), connectionFileOrNewConnectionProperty)
      .orEmpty()

  public fun generateBspConnectionDetailFileForGeneratorWithName(
    generatorId: String,
    outputStream: OutputStream
  ): VirtualFile? =
    availableBspConnectionDetailsGenerators
      .find { it.id() == generatorId }
      ?.generateBspConnectionDetailsFile(projectPath, outputStream)
}

public fun Process.logErrorOutputs(projectPath: VirtualFile) {
  if (!Registry.`is`("bsp.log.error.outputs")) return
  val project = ProjectLocator.getInstance().guessProjectForFile(projectPath)
  @Suppress("DeferredResultUnused")
  project?.let {
    val bspConsoleService = BspConsoleService.getInstance(project)
    BspCoroutineService.getInstance(project).startAsync {
      val bufferedReader = this.errorReader()
      bufferedReader.forEachLine { doLogErrorOutputLine(it, bspConsoleService) }
    }
  }
}

private fun doLogErrorOutputLine(line: String, bspConsoleService: BspConsoleService) {
  val taskConsole = bspConsoleService.getActiveConsole()
  taskConsole?.addMessage(line)
}

private fun BspConsoleService.getActiveConsole(): TaskConsole? =
  if (this.bspBuildConsole.hasTasksInProgress()) this.bspBuildConsole
  else if (this.bspSyncConsole.hasTasksInProgress()) this.bspSyncConsole
  else null
