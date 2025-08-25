package org.jetbrains.bazel.workspacecontext.provider

import org.jetbrains.bazel.projectview.model.ProjectView

/**
 * Maps `ProjectView` into `WorkpaceContextEntity`.
 * It takes entire `ProjectView` because sometimes in order to create one `WorkpaceContextEntity`
 * you may want to use multiple `ProjectView` sections.
 *
 * @param <T> type of the mapped entity
 * @see WorkpaceContextEntity
 * @see org.jetbrains.bazel.projectview.model.ProjectView
 */
interface WorkspaceContextEntityExtractor<T> {
  fun fromProjectView(projectView: ProjectView): T
}
