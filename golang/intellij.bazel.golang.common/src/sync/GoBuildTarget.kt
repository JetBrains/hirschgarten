package org.jetbrains.bazel.golang.sync

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.SourceFileCollection
import org.jetbrains.bsp.protocol.extractData
import java.nio.file.Path

@ApiStatus.Internal
data class GoBuildTarget(
  val sdkHomePath: Path? = null,
  val importPath: String,
  val sources: SourceFileCollection,
  val embed: List<WorkspaceTargetKey>,
) : BuildTargetData

@ApiStatus.Internal
fun extractGoBuildTarget(target: BuildTarget): GoBuildTarget? = target.extractData()

