package org.jetbrains.plugins.bsp.import

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.platform.PlatformProjectOpenProcessor.Companion.isNewProject
import org.jetbrains.magicmetamodel.MagicMetaModelDiff
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.connection.BspConnectionService
import org.jetbrains.plugins.bsp.connection.BspFileConnection
import org.jetbrains.plugins.bsp.connection.BspGeneratorConnection
import org.jetbrains.plugins.bsp.extension.points.BspConnectionDetailsGeneratorExtension
import org.jetbrains.plugins.bsp.import.wizzard.ConnectionFile
import org.jetbrains.plugins.bsp.import.wizzard.ImportProjectWizzard
import org.jetbrains.plugins.bsp.import.wizzard.NewConnection
import org.jetbrains.plugins.bsp.protocol.connection.BspConnectionDetailsGeneratorProvider
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.widgets.document.targets.BspDocumentTargetsWidget
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetFactory

/**
 * Runs actions after the project has started up and the index is up-to-date.
 *
 * @see BspProjectOpenProcessor for additional actions that
 * may run when a project is being imported for the first time.
 */
public class BspStartupActivity : StartupActivity {

  override fun runActivity(project: Project) {
    if (project.isBspProject()) {
      doRunActivity(project)
    }
  }

  private fun doRunActivity(project: Project) {
    val bspSyncConsole = BspConsoleService.getInstance(project).bspSyncConsole
    bspSyncConsole.startTask("bsp-import", "BSP: Import", "Importing...")

    if (project.isNewProject()) {
      showWizzardAndInitializeConnection(project)
    }

    val magicMetaModelService = MagicMetaModelService.getInstance(project)

    // TODO it's ugly
    val task = object : Task.Backgroundable(project, "Loading changes...", true) {

      private var magicMetaModelDiff: MagicMetaModelDiff? = null

      override fun run(indicator: ProgressIndicator) {
        val connection = BspConnectionService.getConnectionOrThrow(project)
        connection.connect("bsp-import")

        val bspResolver = VeryTemporaryBspResolver(project)
        val projectDetails = bspResolver.collectModel("bsp-import")

        magicMetaModelService.initializeMagicModel(projectDetails)
        val magicMetaModel = magicMetaModelService.magicMetaModel

        magicMetaModelDiff = magicMetaModel.loadDefaultTargets()
      }

      override fun onSuccess() {
        runWriteAction { magicMetaModelDiff?.applyOnWorkspaceModel() }

        ToolWindowManager.getInstance(project).registerToolWindow("BSP") {
          icon = BspPluginIcons.bsp
          canCloseContent = false
          anchor = ToolWindowAnchor.RIGHT
          contentFactory = BspAllTargetsWidgetFactory()
        }

        val statusBar = WindowManager.getInstance().getStatusBar(project)
        // TODO it's internal - we shouldnt use it
        statusBar.addWidget(BspDocumentTargetsWidget(project), "before git", BspDocumentTargetsWidget(project))
      }
    }
    task.queue()
  }

  private fun showWizzardAndInitializeConnection(
    project: Project,
  ) {
    val bspConnectionDetailsGeneratorProvider = BspConnectionDetailsGeneratorProvider(
      project.getProjectDirOrThrow(),
      BspConnectionDetailsGeneratorExtension.extensions()
    )

    val wizzard = ImportProjectWizzard(project, bspConnectionDetailsGeneratorProvider)
    if (wizzard.showAndGet()) {
      when (val connectionFileOrNewConnection = wizzard.connectionFileOrNewConnectionProperty.get()) {
        is NewConnection -> initializeNewConnection(project, bspConnectionDetailsGeneratorProvider)
        is ConnectionFile -> initializeConnectionFromFile(project, connectionFileOrNewConnection)
      }
    }
  }

  private fun initializeNewConnection(
    project: Project,
    bspConnectionDetailsGeneratorProvider: BspConnectionDetailsGeneratorProvider,
  ) {
    val name = bspConnectionDetailsGeneratorProvider.firstGeneratorTEMPORARY()
    // TODO
    val generator = BspConnectionDetailsGeneratorExtension.extensions().find { it.name() == name }!!
    val bspGeneratorConnection = BspGeneratorConnection(project, generator)

    val bspConnectionService = BspConnectionService.getInstance(project)
    bspConnectionService.init(bspGeneratorConnection)
  }

  private fun initializeConnectionFromFile(project: Project, connectionFileInfo: ConnectionFile) {
    val bspFileConnection = BspFileConnection(project, connectionFileInfo.locatedBspConnectionDetails)

    val bspConnectionService = BspConnectionService.getInstance(project)
    bspConnectionService.init(bspFileConnection)
  }
}
