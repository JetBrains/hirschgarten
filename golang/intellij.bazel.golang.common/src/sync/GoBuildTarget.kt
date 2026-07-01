package org.jetbrains.bazel.golang.sync

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.ClassDiscriminator
import org.jetbrains.bsp.protocol.extractData
import java.nio.file.Path

@ClassDiscriminator(5)
@ApiStatus.Internal
data class GoBuildTarget(
  @Transient @JvmField val sdkHomePath: Path? = null,
  @Transient val importPath: String,
  @Transient val sources: List<Path>,
  @Transient val embed: List<WorkspaceTargetKey>,
) : BuildTargetData

@ApiStatus.Internal
fun extractGoBuildTarget(target: BuildTarget): GoBuildTarget? = target.extractData()

