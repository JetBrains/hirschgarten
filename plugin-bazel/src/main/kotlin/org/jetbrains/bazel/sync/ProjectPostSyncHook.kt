package org.jetbrains.bazel.sync

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.platform.util.progress.SequentialProgressReporter
import org.jetbrains.bazel.annotations.PublicApi
import org.jetbrains.bazel.ui.console.withSubtask

/**
 * Represents a post-sync hook which will be executed after each sync (if `isEnabled` returns true).
 *
 * It should be used to perform post-sync actions, e.g., code cleanup after sync.
 * It's guaranteed that hook will be called *after* all [ProjectSyncHook] calls.
 */
@PublicApi
interface ProjectPostSyncHook {
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
    @JvmField
    val ep = ExtensionPointName.create<ProjectPostSyncHook>("org.jetbrains.bazel.projectPostSyncHook")
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

val Project.projectPostSyncHooks: List<ProjectPostSyncHook>
  get() =
    ProjectPostSyncHook.ep.extensionList.filter { it.isEnabled(this) }

suspend fun <T> ProjectPostSyncHook.ProjectPostSyncHookEnvironment.withSubtask(text: String, block: suspend (subtaskId: String) -> T) =
  project.withSubtask(progressReporter, taskId, text, block)
