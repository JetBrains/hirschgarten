package org.jetbrains.plugins.bsp.flow.open

import com.intellij.ide.CommandLineInspectionProjectConfigurator
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.observation.ActivityTracker
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

public class BspCommandLineInspectionConfigurator : CommandLineInspectionProjectConfigurator {

  override fun getName(): String = "Bsp Command Line Inspection"

  override fun getDescription(): String = "Bsp Command Line Inspection"

  override fun configureProject(
    project: Project,
    context: CommandLineInspectionProjectConfigurator.ConfiguratorContext) {
    runBlocking {
      ActivityTracker.EP_NAME.extensionList
        .firstIsInstanceOrNull<BspStartupActivityTracker>()?.awaitConfiguration(project)
    }

  }
}