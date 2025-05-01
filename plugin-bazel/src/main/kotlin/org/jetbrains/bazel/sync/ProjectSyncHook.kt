package org.jetbrains.bazel.sync

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.platform.util.progress.SequentialProgressReporter
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.projectStructure.AllProjectStructuresDiff
import org.jetbrains.bazel.sync.scope.ProjectSyncScope
import org.jetbrains.bsp.protocol.JoinedBuildServer

/**
 * Represents a sync hook which will be executed on each sync (if `isEnabled` returns true).
 *
 * It should be used to perform sync actions, e.g., to add a language support.
 */
interface ProjectSyncHook {
  /**
   * Tells the sync mechanism whatever this hook should be executed during sync.
   * It will always be called before each `onSync` call.
   */
  fun isEnabled(project: Project): Boolean = true

  /**
   * Method which will be called during sync. It can perform any type of activity that is part of sync.
   */
  suspend fun onSync(environment: ProjectSyncHookEnvironment)

  companion object {
    val ep = ExtensionPointName.create<ProjectSyncHook>("org.jetbrains.bazel.projectSyncHook")
  }

  /**
   * @param project project on which sync is executed
   * @param server BSP server which is used during sync
   * @param capabilities capabilities of used BSP server
   * @param diff diff which was prepared before sync and which should be updated in the hook
   * @param taskId task id which should be used in the sync console as root
   * @param progressReporter should be used to report the progress of the hook
   * @param buildTargets base info about all the available targets in the project
   */
  data class ProjectSyncHookEnvironment(
    val project: Project,
    val syncScope: ProjectSyncScope,
    val server: JoinedBuildServer,
    val diff: AllProjectStructuresDiff,
    val taskId: String,
    val progressReporter: SequentialProgressReporter,
    val buildTargets: Map<Label, BspTargetInfo.TargetInfo>,
  )
}

val Project.projectSyncHooks: List<ProjectSyncHook>
  get() =
    ProjectSyncHook.ep
      .extensionList
      .filter { it.isEnabled(this) }
