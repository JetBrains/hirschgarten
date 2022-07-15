package org.jetbrains.plugins.bsp.import

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.startup.StartupManager
import org.jetbrains.plugins.bsp.services.BuildWindowSomething
import org.jetbrains.plugins.bsp.services.MagicMetaModelService

/**
 * Runs startup actions just after a project is opened, before it's indexed.
 *
 * @see BspInitializer for actions that run later.
 */
public class ProjectOpenActivity : StartupActivity, StartupActivity.DumbAware {
  override fun runActivity(project: Project) {
    println("ProjectOpenActivity.runActivity")
    Logger.getInstance(ProjectOpenActivity::class.java).info("ProjectOpenActivity.runActivity")

    BuildWindowSomething(project)


  }
}