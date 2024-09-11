package org.jetbrains.plugins.bsp.impl.flow.sync

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.platform.util.progress.SequentialProgressReporter
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.plugins.bsp.config.WithBuildToolId
import org.jetbrains.plugins.bsp.config.allWithBuildToolId
import org.jetbrains.plugins.bsp.config.bspBuildToolId
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.projectStructure.AllProjectStructuresDiff

/**
 * Represents a sync hook which will be executed on each sync (if `isEnabled` returns true).
 *
 * It should be used to perform sync actions, e.g., to add a language support.
 */
interface ProjectSyncHook : WithBuildToolId {
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
    internal val ep =
      ExtensionPointName.create<ProjectSyncHook>("org.jetbrains.bsp.projectSyncHook")
  }

  /**
   * @param project project on which sync is executed
   * @param server BSP server which is used during sync
   * @param capabilities capabilities of used BSP server
   * @param diff diff which was prepared before sync and which should be updated in the hook
   * @param taskId task id which should be used in the sync console as root
   * @param progressReporter should be used to report the progress of the hook
   * @param baseTargetInfos base info about all the available targets in the project
   */
  data class ProjectSyncHookEnvironment(
    val project: Project,
    val server: JoinedBuildServer,
    val capabilities: BazelBuildServerCapabilities,
    val diff: AllProjectStructuresDiff,
    val taskId: String,
    val progressReporter: SequentialProgressReporter,
    val baseTargetInfos: BaseTargetInfos,
  )
}

/**
 * Allows disabling default project sync hooks.
 *
 * Sometimes default project sync hooks need to be overridden in some way. For example, build-tool-specific hook can
 * perform the same action but in different way, and to prevent conflicts, it might be necessary to disable the
 * default hooks.
 */
interface DefaultProjectSyncHooksDisabler : WithBuildToolId {
  fun disabledProjectSyncHooks(project: Project): List<Class<out ProjectSyncHook>>

  companion object {
    internal val ep =
      ExtensionPointName.create<DefaultProjectSyncHooksDisabler>("org.jetbrains.bsp.defaultProjectSyncHooksDisabler")
  }
}

internal val Project.disabledDefaultProjectSyncHooks: List<Class<out ProjectSyncHook>>
  get() =
    DefaultProjectSyncHooksDisabler.ep
      .allWithBuildToolId(buildToolId)
      .flatMap { it.disabledProjectSyncHooks(this) }

internal val Project.defaultProjectSyncHooks: List<ProjectSyncHook>
  get() {
    val disabled = disabledDefaultProjectSyncHooks

    return ProjectSyncHook.ep
      .allWithBuildToolId(bspBuildToolId)
      .filter { it.isEnabled(this) }
      .filterNot { it::class.java in disabled }
  }

internal val Project.additionalProjectSyncHooks: List<ProjectSyncHook>
  get() =
    if (buildToolId != bspBuildToolId) {
      ProjectSyncHook.ep.allWithBuildToolId(buildToolId).filter { it.isEnabled(this) }
    } else {
      emptyList()
    }
