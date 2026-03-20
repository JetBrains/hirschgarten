package org.jetbrains.bazel.startup

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.observation.ActivityTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

internal class BazelStartupActivityTracker : ActivityTracker {
  override val presentableName: String
    get() = "bsp-sync"

  override suspend fun awaitConfiguration(project: Project) {
    isStartupRunning(project).first { !it }
  }

  override suspend fun isInProgress(project: Project): Boolean = isStartupRunning(project).value

  private suspend fun isStartupRunning(project: Project): MutableStateFlow<Boolean> =
    BspConfigurationTrackerService.getInstanceAsync(project).isRunning
}

@Service(Service.Level.PROJECT)
internal class BspConfigurationTrackerService {
  companion object {
    @JvmStatic
    suspend fun getInstanceAsync(project: Project): BspConfigurationTrackerService = project.serviceAsync()
  }

  val isRunning: MutableStateFlow<Boolean> = MutableStateFlow(false)
}
