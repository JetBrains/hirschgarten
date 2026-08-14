package org.jetbrains.bazel.sync.workspace.snapshot

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadOptions
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetData

@ApiStatus.Internal
inline fun <reified T : BuildTargetData> BuildTarget.findBuildData(): T? = data.filterIsInstance<T>().firstOrNull()

@ApiStatus.Internal
inline fun <reified T : BuildTargetData> BuildTarget.hasBuildData(): Boolean = findBuildData<T>() != null

@ApiStatus.Internal
inline fun <reified T : BuildTargetData> Sequence<BuildTarget>.filterBuildTarget(): Sequence<Pair<BuildTarget, T>> =
  mapNotNull { target -> target.findBuildData<T>()?.let { data -> target to data } }

@get:ApiStatus.Internal
val WorkspaceSnapshot.allTargets: Sequence<BuildTarget>
  get(): Sequence<BuildTarget> = targetGraph.allTargets.mapNotNull { it.load(targets, TargetLoadOptions.ALL) }

@get:ApiStatus.Internal
val WorkspaceSnapshot.commonSyncConfig: CommonWorkspaceSyncConfig
  get() = syncConfigs.filterIsInstance<CommonWorkspaceSyncConfig>().first()
