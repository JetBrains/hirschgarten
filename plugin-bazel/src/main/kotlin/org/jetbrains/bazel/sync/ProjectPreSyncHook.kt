package org.jetbrains.bazel.sync

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.platform.util.progress.SequentialProgressReporter

/**
 * Represents a pre-sync hook which will be executed before each sync (if `isEnabled` returns true).
 *
 * It should be used to perform pre-sync actions, e.g., run code generation before sync.
 * It's guaranteed that hook will be called *before* all [ProjectSyncHook] calls.
 */
interface ProjectPreSyncHook {
  /**
   * Tells the sync mechanism whatever this hook should be executed before a sync.
   * It will always be called before each `onPreSync` call.
   */
  fun isEnabled(project: Project): Boolean = true

  /**
   * Method which will be called before a sync.
   */
  suspend fun onPreSync(environment: ProjectPreSyncHookEnvironment)

  companion object {
    val ep = ExtensionPointName.create<ProjectPreSyncHook>("org.jetbrains.bazel.projectPreSyncHook")
  }

  /**
   * @param project project on which sync is executed
   * @param taskId task id which should be used in the sync console as root
   * @param progressReporter should be used to report the progress of the hook
   */
  data class ProjectPreSyncHookEnvironment(
    val project: Project,
    val taskId: String,
    val progressReporter: SequentialProgressReporter,
  )
}

val Project.defaultProjectPreSyncHooks: List<ProjectPreSyncHook>
  get() =
    ProjectPreSyncHook.ep
      .extensions
      .filter { it.isEnabled(this) }
