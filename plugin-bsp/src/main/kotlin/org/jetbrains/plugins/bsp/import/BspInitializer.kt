package org.jetbrains.plugins.bsp.import

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import org.jetbrains.plugins.bsp.services.MagicMetaModelService

/**
 * Runs actions after the project has started up and the index is up to date.
 *
 * @see ProjectOpenActivity for actions that run earlier.
 * @see BspProjectOpenProcessor for additional actions that
 * may run when a project is being imported for the first time.
 */
public class BspInitializer : StartupActivity {
  override fun runActivity(project: Project) {
    println("BspInitializer.runActivity")
    Logger.getInstance(ProjectOpenActivity::class.java).info("BspInitializer.runActivity")

    ApplicationManager.getApplication().invokeLater {
      val magicMetaModelService = MagicMetaModelService.getInstance(project)
      magicMetaModelService.initializeMagicModel()

      println("ProjectOpenActivity.invokeLater")
      runWriteAction {
        val magicMetaModel = magicMetaModelService.magicMetaModel
        println("ProjectOpenActivity.runWriteAction")
        magicMetaModel.loadDefaultTargets()
      }
    }
  }
}