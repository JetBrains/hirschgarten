package org.jetbrains.bazel.clion.workspace

import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.lang.workspace.OCWorkspace
import com.jetbrains.cidr.lang.workspace.OCWorkspaceModificationTrackersImpl
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId

/**
 * Holds the configurations of the build of the last debug session. [CcDebugSessionChooser] selects a resolve configuration
 * that has one of them.
 *
 * The state lives as long as the project is open. A sync does not clear it, because a configuration that the new snapshot
 * does not contain matches no resolve configuration.
 */
@ApiStatus.Internal
@Service(Service.Level.PROJECT)
class CcDebugSessionConfigurations(private val project: Project) {

  /** The configuration of the debugged target comes first, the configurations of its dependencies follow. */
  @Volatile
  var configurations: List<WorkspaceConfigurationId> = emptyList()
    private set

  /** Stores the configurations of a new debug session and tells the resolution engine to select the resolve configurations again. */
  suspend fun update(newConfigurations: List<WorkspaceConfigurationId>) {
    val distinct = newConfigurations.distinct()
    if (distinct == configurations) return

    val trackers = OCWorkspace.getInstance(project).modificationTrackers as OCWorkspaceModificationTrackersImpl
    edtWriteAction {
      if (distinct == configurations) return@edtWriteAction
      configurations = distinct
      trackers.fireSelectedResolveConfigurationChanged()
    }
  }

  companion object {
    fun getInstance(project: Project): CcDebugSessionConfigurations = project.service()
  }
}
