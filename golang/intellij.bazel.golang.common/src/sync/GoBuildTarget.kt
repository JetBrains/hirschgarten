package org.jetbrains.bazel.golang.sync

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.ClassDiscriminator
import org.jetbrains.bsp.protocol.utils.extractData
import java.nio.file.Path

@ClassDiscriminator(5)
@ApiStatus.Internal
data class GoBuildTarget(
  @Transient @JvmField val sdkHomePath: Path? = null,
  val importPath: String,
  val sources: List<Path>,
  val embed: List<Label>,
) : BuildTargetData

@ApiStatus.Internal
fun extractGoBuildTarget(target: BuildTarget): GoBuildTarget? = target.extractData()

