package org.jetbrains.bazel.sync.workspace.snapshot

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Debug
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadOptions
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.OutputLocationCollection
import java.nio.file.Path

@Debug.Renderer(text = "this.getKey()")
internal class WorkspaceTarget(
  override val key: WorkspaceTargetKey,
  override val dependencies: List<DependencyLabel>,
  override val kind: TargetKind,
  override val sources: OutputLocationCollection,
  override val resources: OutputLocationCollection,
  override val baseDirectory: Path,
  override val data: List<BuildTargetData>,
  override val generatorName: String?,
  override val isWorkspace: Boolean,
  override val isTestOnly: Boolean,
  override val tags: List<String>,
) : BuildTarget {
  override val loaded: TargetLoadOptions
    get() = TargetLoadOptions.ALL
}

@ApiStatus.Internal
fun BuildTarget.isTestTarget(): Boolean = isTestOnly || kind.ruleType == RuleType.TEST
