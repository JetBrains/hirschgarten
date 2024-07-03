package org.jetbrains.plugins.bsp.flow.open

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.observation.ActivityTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

internal class BspStartupActivityTracker : ActivityTracker {
  override val presentableName: String = "BSP Activity Tracker"
  override suspend fun awaitConfiguration(project: Project) {
    project.bspTrackerServiceState().first { !it }
  }

  override suspend fun isInProgress(project: Project): Boolean =
    project.bspTrackerServiceState().value

  internal companion object {
    val log = logger<BspStartupActivityTracker>()
    suspend fun startConfigurationPhase(project: Project) {
      project.bspTrackerServiceState().update { true }
    }

    suspend fun stopConfigurationPhase(project: Project) {
      project.bspTrackerServiceState().update { false }
    }

    private suspend fun Project.bspTrackerServiceState() =
      this.serviceAsync<BspConfigurationTrackerService>().isRunning
  }
}

@Service(Service.Level.PROJECT)
internal class BspConfigurationTrackerService {
  val isRunning: MutableStateFlow<Boolean> = MutableStateFlow(false)
}
