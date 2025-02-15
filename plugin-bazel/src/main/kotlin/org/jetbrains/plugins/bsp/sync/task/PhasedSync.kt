package org.jetbrains.plugins.bsp.sync.task

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.IncompleteDependenciesService
import com.intellij.openapi.project.IncompleteDependenciesService.IncompleteDependenciesAccessToken
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.BspFeatureFlags
import org.jetbrains.plugins.bsp.sync.scope.FirstPhaseSync
import org.jetbrains.plugins.bsp.sync.scope.SecondPhaseSync

class PhasedSync(private val project: Project) {
  suspend fun sync() {
    var incompleteState: IncompleteDependenciesAccessToken? = null
    try {
      ProjectSyncTask(project).sync(FirstPhaseSync, false)
      incompleteState =
        writeAction {
          project.service<IncompleteDependenciesService>().enterIncompleteState(this)
        }

      if (BspFeatureFlags.executeSecondPhaseOnSync) {
        ProjectSyncTask(project).sync(SecondPhaseSync, true)
      }
    } finally {
      if (incompleteState != null && BspFeatureFlags.executeSecondPhaseOnSync) {
        writeAction { incompleteState.finish() }
      }
    }
  }
}
