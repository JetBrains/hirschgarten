package org.jetbrains.bazel.sync

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectTracker
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.projectAware.BazelWorkspace
import java.nio.file.Path

/**
 * The Bazel targets/packages that changed since the last sync.
 *
 * @property paths the changed package directories
 * @property wholeProject true when the plugin cannot tell which part of the project changed
 */
@ApiStatus.Internal
data class ProjectDirtyState(
  val paths: Set<Path> = emptySet(),
  val wholeProject: Boolean = false,
) {
  val isEmpty: Boolean get() = paths.isEmpty() && !wholeProject

  companion object {
    val EMPTY: ProjectDirtyState = ProjectDirtyState()
  }
}

/**
 * Holds the Bazel files that changed since the last sync.
 *
 * The plugin tracks these changes itself. It does not use the platform settings-file CRC scan,
 * which recalculates the CRC of every tracked file on each change (BAZEL-3506).
 *
 * A future partial sync reads this state to find the scope of the next sync.
 */
@Service(Service.Level.PROJECT)
@ApiStatus.Internal
class ProjectDirtyStateService(private val project: Project, coroutineScope: CoroutineScope) {
  private val mutableState = MutableStateFlow(ProjectDirtyState.EMPTY)

  /** The current dirty state. A consumer can observe it. */
  val state: StateFlow<ProjectDirtyState> = mutableState.asStateFlow()

  init {
    coroutineScope.launch {
      state
        .map { !it.isEmpty }
        .distinctUntilChanged()
        .collect { dirty -> if (dirty) notifyPlatform() }
    }
  }

  /** Adds [paths] to the dirty set. Returns true if the state changed. */
  fun markDirty(paths: Collection<Path>): Boolean {
    if (paths.isEmpty()) return false
    val old = mutableState.value
    if (old.wholeProject) return false
    if (old.paths.containsAll(paths)) return false

    val new = old.paths + paths
    val next =
      if (new.size > MAX_TRACKED_PATHS) {
        logger.info("Too many dirty Bazel files in ${project.name}. Marking the whole project.")
        ProjectDirtyState(wholeProject = true)
      } else {
        old.copy(paths = new)
      }
    mutableState.value = next
    return true
  }

  /** Marks the whole project. Use it when the plugin cannot tell which files changed. */
  fun markWholeProjectDirty(reason: String): Boolean {
    if (mutableState.value.wholeProject) return false
    logger.info("Marking the whole Bazel project ${project.name} as dirty: $reason")
    mutableState.value = ProjectDirtyState(wholeProject = true)
    return true
  }

  fun current(): ProjectDirtyState = mutableState.value

  fun isDirty(): Boolean = !mutableState.value.isEmpty

  fun finishSync(scope: ProjectSyncScope) {
    mutableState.update { current ->
      // [todo] derive state with difference
      ProjectDirtyState.EMPTY
    }
  }

  fun clearAll() {
    mutableState.value = ProjectDirtyState.EMPTY
  }

  private fun notifyPlatform() {
    if (project.isDisposed) return
    ExternalSystemProjectTracker.getInstance(project)
      .markDirtyInternal(BazelWorkspace.getInstance(project).projectId)
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): ProjectDirtyStateService = project.service()
  }
}

/**
 * The largest number of dirty files to track. Above it the service marks the whole project.
 * A branch switch can change hundreds of thousands of files.
 */
private const val MAX_TRACKED_PATHS = 5_000

private val logger = Logger.getInstance(ProjectDirtyStateService::class.java)
