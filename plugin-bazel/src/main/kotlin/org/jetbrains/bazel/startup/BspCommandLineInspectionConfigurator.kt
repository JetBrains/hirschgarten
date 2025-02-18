@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.jetbrains.bazel.startup

import com.intellij.ide.CommandLineInspectionProjectConfigurator
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.observation.EP_NAME
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.config.isBspProject
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class BspCommandLineInspectionConfigurator : CommandLineInspectionProjectConfigurator {
  override fun getName(): String = "Bsp Command Line Inspection"

  override fun getDescription(): String = "Bsp Command Line Inspection"

  override fun configureProject(project: Project, context: CommandLineInspectionProjectConfigurator.ConfiguratorContext) {
    runBlocking {
      if (project.isBspProject) {
        EP_NAME.extensionList
          .firstIsInstanceOrNull<BspStartupActivityTracker>()
          ?.awaitConfiguration(project)
      }
    }
  }
}
