package org.jetbrains.bazel.sync.projectStructure

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync.scope.ProjectSyncScope
import org.jetbrains.bsp.protocol.TaskId

/**
 * Represents a diff (snapshot) of the underlying project structure which should be updated during sync in the
 * background. After all the changes are done, diff should be applied on the underlying project structure.
 */
interface ProjectStructureDiff {
  /**
   * Applies the changes on the underlying project structure.
   * Method implementation is responsible for getting the write lock if needed.
   *
   * @param project project for which diff is applied
   * @param syncScope defines the scope of the sync for which the diff is applied
   * @param taskId id of the console parent task (sync console)
   */
  suspend fun apply(
    project: Project,
    syncScope: ProjectSyncScope,
    taskId: TaskId,
  )
}

/**
 * Represents all the available diffs - sometimes during sync, more than one project structure needs to be updated,
 * and this class keeps all of them in one place.
 */
class AllProjectStructuresDiff(private val project: Project, diffs: List<ProjectStructureDiff>) {
  private val diffs = diffs.associateBy { it::class.java }

  @Suppress("UNCHECKED_CAST")
  fun <TDiff : ProjectStructureDiff> diffOfType(diffClazz: Class<TDiff>): TDiff =
    diffs[diffClazz] as? TDiff ?: error("Cannot find a ProjectStructureDiff of type: ${diffClazz.simpleName}")

  suspend fun applyAll(syncScope: ProjectSyncScope, taskId: TaskId) {
    diffs.values.forEach { it.apply(project, syncScope, taskId) }
  }
}

/**
 * Provides a new diff of the project structure which will be used during sync.
 */
interface ProjectStructureProvider<TDiff : ProjectStructureDiff> {
  /**
   * Returns a new project structure diff. It should be a diff with a fresh state which later can be applied on the
   * underlying project structure.
   */
  fun newDiff(project: Project): TDiff

  companion object {
    internal val ep = ExtensionPointName.create<ProjectStructureProvider<*>>("org.jetbrains.bazel.projectStructureProvider")
  }
}

/**
 * Provides all available diffs for the sync.
 *
 * This provider should be used in the code when any project structure update is necessary. It takes care of obtaining
 * all the available diffs and wraps them in `AllProjectStructuresDiff`.
 */
class AllProjectStructuresProvider(private val project: Project) {
  fun newDiff(): AllProjectStructuresDiff {
    val providers = ProjectStructureProvider.ep.extensionList
    val diffs = providers.map { it.newDiff(project) }

    return AllProjectStructuresDiff(project, diffs)
  }
}
