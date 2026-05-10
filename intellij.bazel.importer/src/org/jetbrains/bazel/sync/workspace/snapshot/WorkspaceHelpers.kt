package org.jetbrains.bazel.sync.workspace.snapshot

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.RawBuildTarget

// TODO: remove this after moving RawBuildTarget to backend module

@get:ApiStatus.Internal
val DependencyLabel.targetKey: WorkspaceTargetKey
  get() = WorkspaceTargetKey(
    label = label,
    configuration = WorkspaceConfigurationId.of(configuration),
  )

@get:ApiStatus.Internal
val RawBuildTarget.targetKey: WorkspaceTargetKey
  get() = WorkspaceTargetKey(
    label = id,
    configuration = WorkspaceConfigurationId.of(configurationId),
  )


@ApiStatus.Internal
inline fun <reified T : BuildTargetData> WorkspaceTarget.findBuildData(): T? = rawBuildTarget.data.filterIsInstance<T>().firstOrNull()

@ApiStatus.Internal
inline fun <reified T : BuildTargetData> WorkspaceTarget.hasBuildData(): Boolean = findBuildData<T>() != null

@ApiStatus.Internal
inline fun <reified T : BuildTargetData> Sequence<WorkspaceTarget>.filterBuildTarget(): Sequence<Pair<WorkspaceTarget, T>> =
  mapNotNull { target -> target.findBuildData<T>()?.let { data -> target to data } }

@get:ApiStatus.Internal
val WorkspaceSnapshot.allTargets: Sequence<WorkspaceTarget>
  get(): Sequence<WorkspaceTarget> = targetGraph.allTargets.asSequence()

@get:ApiStatus.Internal
val WorkspaceTarget.kind: String
  get() = rawBuildTarget.kind.kind
