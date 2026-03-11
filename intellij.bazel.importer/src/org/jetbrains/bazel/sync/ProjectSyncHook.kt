package org.jetbrains.bazel.sync

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.platform.util.progress.SequentialProgressReporter
import org.jetbrains.annotations.ApiStatus
import com.intellij.platform.workspace.storage.MutableEntityStorage
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.progress.syncConsole
import org.jetbrains.bazel.progress.withSubtask
import org.jetbrains.bazel.sync.scope.ProjectSyncScope
import org.jetbrains.bazel.sync.workspace.BazelResolvedWorkspace
import org.jetbrains.bazel.sync.workspace.BazelWorkspaceResolveService
import org.jetbrains.bsp.protocol.BazelServerFacade
import org.jetbrains.bsp.protocol.TaskId

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
    @ApiStatus.Internal
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
  @ConsistentCopyVisibility
  data class ProjectSyncHookEnvironment @ApiStatus.Internal constructor(
    val project: Project,
    val syncScope: ProjectSyncScope,
    val server: BazelServerFacade,
    val workspace: BazelResolvedWorkspace,
    val resolver: BazelWorkspaceResolveService,
    // TODO: exposed public API: TaskId
    val taskId: TaskId,
    val diff: MutableEntityStorage,
    val progressReporter: SequentialProgressReporter,
    internal val buildTargets: Map<Label, BspTargetInfo.TargetInfo>,
    val deferredApplyActions: MutableList<suspend () -> Unit> = mutableListOf(),
  )
}

val Project.projectSyncHooks: List<ProjectSyncHook>
  @ApiStatus.Internal
  get() =
    ProjectSyncHook.ep
      .extensionList
      .filter { it.isEnabled(this) }

@ApiStatus.Internal
suspend fun <T> ProjectSyncHook.ProjectSyncHookEnvironment.withSubtask(text: String, block: suspend (subtaskId: TaskId) -> T) =
  project.syncConsole.withSubtask(progressReporter, taskId.subTask(text), text, block)
