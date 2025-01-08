package org.jetbrains.plugins.bsp.impl.flow.sync

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.platform.util.progress.SequentialProgressReporter
import org.jetbrains.plugins.bsp.config.WithBuildToolId
import org.jetbrains.plugins.bsp.config.allWithBuildToolId
import org.jetbrains.plugins.bsp.config.bspBuildToolId
import org.jetbrains.plugins.bsp.config.buildToolId

/**
 * Represents a post-sync hook which will be executed after each sync (if `isEnabled` returns true).
 *
 * It should be used to perform post-sync actions, e.g., code cleanup after sync.
 * It's guaranteed that hook will be called *after* all [ProjectSyncHook] calls.
 */
interface ProjectPostSyncHook : WithBuildToolId {
  /**
   * Tells the sync mechanism whatever this hook should be executed after a sync.
   * It will always be called before each `onPostSync` call.
   */
  fun isEnabled(project: Project): Boolean = true

  /**
   * Method which will be called after a sync.
   */
  suspend fun onPostSync(environment: ProjectPostSyncHookEnvironment)

  companion object {
    internal val ep =
      ExtensionPointName.create<ProjectPostSyncHook>("org.jetbrains.bsp.projectPostSyncHook")
  }

  /**
   * @param project project on which sync is executed
   * @param taskId task id which should be used in the sync console as root
   * @param progressReporter should be used to report the progress of the hook
   */
  data class ProjectPostSyncHookEnvironment(
    val project: Project,
    val taskId: String,
    val progressReporter: SequentialProgressReporter,
  )
}

internal val Project.defaultProjectPostSyncHooks: List<ProjectPostSyncHook>
  get() =
    ProjectPostSyncHook.ep
      .allWithBuildToolId(bspBuildToolId)
      .filter { it.isEnabled(this) }

internal val Project.additionalProjectPostSyncHooks: List<ProjectPostSyncHook>
  get() =
    if (buildToolId != bspBuildToolId) {
      ProjectPostSyncHook.ep.allWithBuildToolId(buildToolId).filter { it.isEnabled(this) }
    } else {
      emptyList()
    }
