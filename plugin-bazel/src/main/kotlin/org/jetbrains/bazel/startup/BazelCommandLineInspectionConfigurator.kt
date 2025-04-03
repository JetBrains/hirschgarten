@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.jetbrains.bazel.startup

import com.intellij.ide.CommandLineInspectionProjectConfigurator
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.observation.EP_NAME
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class BazelCommandLineInspectionConfigurator : CommandLineInspectionProjectConfigurator {
  override fun getName(): String = "Bsp Command Line Inspection"

  override fun getDescription(): String = "Bsp Command Line Inspection"

  override fun configureProject(project: Project, context: CommandLineInspectionProjectConfigurator.ConfiguratorContext) {
    runBlocking {
      if (project.isBazelProject) {
        EP_NAME.extensionList
          .firstIsInstanceOrNull<BazelStartupActivityTracker>()
          ?.awaitConfiguration(project)
      }
    }
  }
}
