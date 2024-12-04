package org.jetbrains.plugins.bsp.impl.flow.sync

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.IncompleteDependenciesService
import com.intellij.openapi.project.IncompleteDependenciesService.IncompleteDependenciesAccessToken
import com.intellij.openapi.project.Project

class PhasedSync(private val project: Project) {
  suspend fun sync() {
    var incompleteState: IncompleteDependenciesAccessToken? = null
    try {
      ProjectSyncTask(project).sync(FirstPhaseSync, false)
      incompleteState =
        writeAction {
          project.service<IncompleteDependenciesService>().enterIncompleteState(this)
        }

      ProjectSyncTask(project).sync(FullProjectSync, true)
    } finally {
      if (incompleteState != null) {
        writeAction { incompleteState.finish() }
      }
    }
  }
}
