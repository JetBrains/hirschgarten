package org.jetbrains.bazel.clion.workspace

import com.intellij.execution.RunManager
import com.intellij.execution.RunManagerListener
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.lang.workspace.OCWorkspace
import com.jetbrains.cidr.lang.workspace.OCWorkspaceModificationTrackersImpl
import org.jetbrains.bazel.config.isBazelProject

internal class CcRunConfigurationListener(private val project: Project) : RunManagerListener {
  override fun runConfigurationSelected(settings: RunnerAndConfigurationSettings?) {
    updateResolveConfiguration()
  }

  override fun runConfigurationChanged(settings: RunnerAndConfigurationSettings) {
    if (RunManager.getInstance(project).selectedConfiguration == settings) updateResolveConfiguration()
  }

  private fun updateResolveConfiguration() {
    if (!project.isBazelProject) return

    val application = ApplicationManager.getApplication()
    application.invokeLater({ fireTrackerUpdates(application) }, project.disposed)
  }

  private fun fireTrackerUpdates(application: Application) {
    application.runWriteAction {
      val trackers = OCWorkspace.getInstance(project).modificationTrackers as OCWorkspaceModificationTrackersImpl
      trackers.fireSelectedResolveConfigurationChanged()
    }
  }
}
