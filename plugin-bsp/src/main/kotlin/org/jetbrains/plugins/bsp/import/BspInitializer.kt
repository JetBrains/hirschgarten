package org.jetbrains.plugins.bsp.import

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.platform.PlatformProjectOpenProcessor.Companion.isNewProject
import com.intellij.project.stateStore
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
import org.jetbrains.plugins.bsp.services.BspBuildConsoleService
import org.jetbrains.plugins.bsp.services.BspSyncConsoleService
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import org.jetbrains.plugins.bsp.ui.widgets.document.targets.BspDocumentTargetsWidget
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetFactory

/**
 * Runs actions after the project has started up and the index is up-to-date.
 *
 * @see BspProjectOpenProcessor for additional actions that
 * may run when a project is being imported for the first time.
 */
public class BspInitializer : StartupActivity {
  override fun runActivity(project: Project) {
    val bspConnectionService = BspConnectionService.getInstance(project)

    val bspConnectionDetailsGeneratorProvider =
      BspConnectionDetailsGeneratorProvider(
        project.guessProjectDir()!!,
        BspConnectionDetailsGeneratorExtension.extensions()
      )
    val bspSyncConsole = BspSyncConsoleService.getInstance(project).bspSyncConsole

    val statusBar = WindowManager.getInstance().getStatusBar(project)
    statusBar.addWidget(BspDocumentTargetsWidget(project), "before git", BspDocumentTargetsWidget(project))

    if (project.isNewProject()) {
      println("BspInitializer.runActivity")
      bspSyncConsole.startImport("bsp-import", "BSP: Import", "Importing...")

      val wizzard = ImportProjectWizzard(project, bspConnectionDetailsGeneratorProvider)
      if (wizzard.showAndGet()) {

        if (wizzard.connectionFileOrNewConnectionProperty.get() is NewConnection) {
          val name = bspConnectionDetailsGeneratorProvider.firstGeneratorTEMPORARY()
          val gen = BspConnectionDetailsGeneratorExtension.extensions().find { it.name() == name }
          val aa = BspGeneratorConnection(project, gen!!)

          bspConnectionService.init(aa)
        } else {
          val g = wizzard.connectionFileOrNewConnectionProperty.get() as ConnectionFile
          val bb = BspFileConnection(project, g.locatedBspConnectionDetails)
          bspConnectionService.init(bb)
        }
      }

      val magicMetaModelService = MagicMetaModelService.getInstance(project)

      val task = object : Task.Backgroundable(project, "Loading changes...", true) {

        private var magicMetaModelDiff: MagicMetaModelDiff? = null

        override fun run(indicator: ProgressIndicator) {
          val bspBuildConsoleService = BspBuildConsoleService.getInstance(project)

          bspConnectionService.connection!!.connect()
          val bspResolver =
            VeryTemporaryBspResolver(
              project.stateStore.projectBasePath,
              bspConnectionService.connection!!.server!!,
              bspSyncConsole,
              bspBuildConsoleService.bspBuildConsole
            )

          val projectDetails = bspResolver.collectModel()

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
        }
      }
      task.queue()
    } else {
      bspSyncConsole.startImport("bsp-import", "BSP: Import", "Importing...")

      val magicMetaModelService = MagicMetaModelService.getInstance(project)

      val task = object : Task.Backgroundable(project, "Loading changes...", true) {

        private var magicMetaModelDiff: MagicMetaModelDiff? = null

        override fun run(indicator: ProgressIndicator) {
          val bspBuildConsoleService = BspBuildConsoleService.getInstance(project)

          bspConnectionService.connection!!.connect()
          val bspResolver =
            VeryTemporaryBspResolver(
              project.stateStore.projectBasePath,
              bspConnectionService.connection!!.server!!,
              bspSyncConsole,
              bspBuildConsoleService.bspBuildConsole
            )

          val projectDetails = bspResolver.collectModel()

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
        }
      }
      task.queue()
    }
  }
}
