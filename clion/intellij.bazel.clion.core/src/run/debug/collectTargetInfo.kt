package org.jetbrains.bazel.clion.run.debug

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo
import com.intellij.build.events.MessageEvent
import com.intellij.execution.ExecutionException
import com.intellij.openapi.components.service
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.bazel.clion.BazelCLionCoreBundle
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.sync.workspace.snapshot.allTargets
import org.jetbrains.bsp.protocol.BuildTarget

@ApiStatus.Internal
data class CcDebugTargetInfo(
  val target: BuildTarget,
  val repoMapping: RepoMapping,
)

/**
 * Finds the target in the current workspace snapshot.
 *
 * @param targets the targets of the debug build, null if the debug aspect did not run
 * @param reportUnknownConfiguration false skips the warning for a configuration that the snapshot does not contain
 */
@Throws(ExecutionException::class)
context(ctx: CcDebugContext)
internal suspend fun collectTargetInfo(
  targets: Collection<TargetIdeInfo>,
  reportUnknownConfiguration: Boolean,
): CcDebugTargetInfo {
  val snapshot = ctx.project.service<WorkspaceSnapshotService>().currentSnapshot()

  val target = findSnapshotTarget(snapshot, targets, reportUnknownConfiguration)
    ?: throw ExecutionException(BazelCLionCoreBundle.message("debug.error.target.not.found", ctx.label))

  return CcDebugTargetInfo(target, snapshot.repoMapping)
}

/**
 * Finds the target in the configuration that the debug build used, because the snapshot can contain a label in more than one
 * configuration. Uses a different configuration if the snapshot does not contain this configuration, and reports a warning
 * if [reportUnknownConfiguration] is true.
 *
 * For example, the debug_flags project view section can change the configuration.
 */
@VisibleForTesting
@ApiStatus.Internal
context(ctx: CcDebugContext)
fun findSnapshotTarget(
  snapshot: WorkspaceSnapshot,
  targets: Collection<TargetIdeInfo>?,
  reportUnknownConfiguration: Boolean = true,
): BuildTarget? {
  val candidates = snapshot.allTargets.filter { it.key.label == ctx.label }

  // without the configuration of the debug build, any configuration is as good as the other
  val configuration = targets?.findDebugTarget(ctx.label)?.let { WorkspaceConfigurationId.of(it.key.configuration) }
    ?: return candidates.firstOrNull()

  val target = candidates.firstOrNull { it.key.configuration == configuration }
  if (target != null) return target

  val fallback = candidates.firstOrNull() ?: return null
  if (!reportUnknownConfiguration) return fallback

  ctx.reportEvent(
    severity = MessageEvent.Kind.WARNING,
    message = BazelCLionCoreBundle.message("debug.warning.target.configuration.unknown", ctx.label),
    description = BazelCLionCoreBundle.message("debug.warning.target.configuration.unknown.description", ctx.label),
  )

  return fallback
}
