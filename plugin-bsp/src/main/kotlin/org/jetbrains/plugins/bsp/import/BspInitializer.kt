package org.jetbrains.plugins.bsp.import

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.project.stateStore
import org.jetbrains.plugins.bsp.services.BspConnectionService
import org.jetbrains.plugins.bsp.services.BspSyncConsoleService
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import org.jetbrains.plugins.bsp.services.VeryTemporaryBspResolver

/**
 * Runs actions after the project has started up and the index is up-to-date.
 *
 * @see BspProjectOpenProcessor for additional actions that
 * may run when a project is being imported for the first time.
 */
public class BspInitializer : StartupActivity {
  override fun runActivity(project: Project) {
    println("BspInitializer.runActivity")

    val connectionService = project.getService(BspConnectionService::class.java)
    val magicMetaModelService = MagicMetaModelService.getInstance(project)

    val task = object : Task.Backgroundable(project, "Loading changes...", true) {

      override fun run(indicator: ProgressIndicator) {
        val bspSyncConsoleService = BspSyncConsoleService.getInstance(project)

        val bspResolver =
          VeryTemporaryBspResolver(project.stateStore.projectBasePath, connectionService.server!!, bspSyncConsoleService.bspSyncConsole)

        val projectDetails = bspResolver.collectModel()

        magicMetaModelService.initializeMagicModel(projectDetails)
        val magicMetaModel = magicMetaModelService.magicMetaModel

        magicMetaModel.loadDefaultTargets()
      }

      override fun onSuccess() {
        val magicMetaModel = magicMetaModelService.magicMetaModel
        runWriteAction {
          magicMetaModel.save()
        }
      }

    }
    task.queue()
  }
}