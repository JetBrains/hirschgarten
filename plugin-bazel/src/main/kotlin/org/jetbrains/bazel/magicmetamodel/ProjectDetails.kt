package org.jetbrains.bazel.magicmetamodel

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget

@ApiStatus.Internal
class ProjectDetails(
  val targetIds: List<Label>,
  val targets: Set<RawBuildTarget>,
  val libraries: List<LibraryItem>,
  val workspaceContext: WorkspaceContext? = null,
) {
  var defaultJdkName: String? = null
}
