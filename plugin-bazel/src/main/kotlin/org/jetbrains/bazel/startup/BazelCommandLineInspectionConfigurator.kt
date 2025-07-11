@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.jetbrains.bazel.startup

import com.intellij.ide.CommandLineInspectionProjectConfigurator
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.observation.EP_NAME
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.isBazelProject

private class BazelCommandLineInspectionConfigurator : CommandLineInspectionProjectConfigurator {
  override fun getName(): String = "Bsp Command Line Inspection"

  override fun getDescription(): String = BazelPluginBundle.message("inspection.configurator.bazel.commandline.description")

  override fun configureProject(project: Project, context: CommandLineInspectionProjectConfigurator.ConfiguratorContext) {
    if (project.isBazelProject) {
      runBlocking {
        EP_NAME.extensionList
          .filterIsInstance<BazelStartupActivityTracker>()
          .firstOrNull()
          ?.awaitConfiguration(project)
      }
    }
  }
}
