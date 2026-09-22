package org.jetbrains.bazel.golang.sync

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.OutputLocationCollection
import org.jetbrains.bsp.protocol.extractData

@ApiStatus.Internal
data class GoBuildTarget(
  val sdkHomePath: OutputLocation? = null,
  val importPath: String,
  val sources: OutputLocationCollection,
  val embed: List<WorkspaceTargetKey>,
) : BuildTargetData

