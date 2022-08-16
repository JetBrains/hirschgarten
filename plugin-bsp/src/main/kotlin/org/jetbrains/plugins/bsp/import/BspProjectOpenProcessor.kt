package org.jetbrains.plugins.bsp.import

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.PlatformProjectOpenProcessor
import com.intellij.projectImport.ProjectOpenProcessor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.layout.not
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.extension.points.BspConnectionDetailsGeneratorExtension
import org.jetbrains.plugins.bsp.services.BspConnectionService
import org.jetbrains.protocol.connection.BspConnectionDetailsGeneratorProvider
import org.jetbrains.protocol.connection.BspConnectionFilesProvider
import javax.swing.Icon
import javax.swing.JComponent

public class BspProjectOpenProcessor : ProjectOpenProcessor() {

  override fun getName(): String = BspPluginBundle.message("plugin.name")

  override fun getIcon(): Icon = BspPluginIcons.bsp

  override fun canOpenProject(file: VirtualFile): Boolean {
    val bspConnectionFilesProvider = BspConnectionFilesProvider(file)
    val bspConnectionDetailsGeneratorProvider =
      BspConnectionDetailsGeneratorProvider(file, BspConnectionDetailsGeneratorExtension.extensions())

    return bspConnectionFilesProvider.isAnyBspConnectionFileDefined() or
      bspConnectionDetailsGeneratorProvider.canGenerateAnyBspConnectionDetailsFile()
  }

  override fun doOpenProject(
    virtualFile: VirtualFile,
    projectToClose: Project?,
    forceOpenInNewFrame: Boolean
  ): Project? {
    val bspConnectionFilesProvider = BspConnectionFilesProvider(virtualFile)
    val bspConnectionDetailsGeneratorProvider =
      BspConnectionDetailsGeneratorProvider(virtualFile, BspConnectionDetailsGeneratorExtension.extensions())

    val dialog = TemporaryBspImportDialog(bspConnectionFilesProvider, bspConnectionDetailsGeneratorProvider)

    return if (dialog.showAndGet()) {
      val project = PlatformProjectOpenProcessor.getInstance().doOpenProject(virtualFile, projectToClose, forceOpenInNewFrame)

      if (project != null) {
        val connectionService = BspConnectionService.getInstance(project)

        connectionService.bspConnectionDetailsGeneratorProvider = bspConnectionDetailsGeneratorProvider
        if (dialog.buildToolUsed.selected()) {
          connectionService.dialogBuildToolUsed = true
          connectionService.dialogBuildToolName = dialog.buildTool
        } else {
          connectionService.dialogBuildToolUsed = false
          connectionService.dialogConnectionFile = bspConnectionFilesProvider.connectionFiles[dialog.connectionFileId]
        }

        project
      } else null
    } else null
  }

  private inner class TemporaryBspImportDialog(
    private val bspConnectionFilesProvider: BspConnectionFilesProvider,
    private val bspConnectionDetailsGeneratorProvider: BspConnectionDetailsGeneratorProvider
  ) : DialogWrapper(true) {

    lateinit var buildToolUsed: Cell<JBCheckBox>

    var connectionFileId = 0

    var buildTool = bspConnectionDetailsGeneratorProvider
      .availableGeneratorsNames().firstOrNull() ?: ""

    init {
      title = "Select An Import Method (Temporary)"
      init()
    }

    override fun createCenterPanel(): JComponent =
      panel {

        row {
          buildToolUsed = checkBox("Use build tool")
        }

        buttonsGroup(title = "Detected Connection Files:") {
          bspConnectionFilesProvider
            .connectionFiles
            .mapIndexed { id, a ->
              row {
                radioButton("name: ${a.bspConnectionDetails.name} location: ${a.connectionFileLocation.url}", id)
                  .enabledIf(buildToolUsed.selected.not())
              }
            }
        }.bind(::connectionFileId)

        buttonsGroup(title = "Detected Build Tools:") {
          bspConnectionDetailsGeneratorProvider
            .availableGeneratorsNames()
            .map {
              row {
                radioButton(it, it).enabledIf(buildToolUsed.selected)
              }
            }
        }.bind(::buildTool)
      }
  }
}
