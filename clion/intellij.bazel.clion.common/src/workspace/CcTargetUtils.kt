package org.jetbrains.bazel.clion.workspace

import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.clion.sync.CcToolchainBuildTarget
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.sync.workspace.snapshot.allTargets
import org.jetbrains.bazel.sync.workspace.snapshot.hasBuildData
import org.jetbrains.bsp.protocol.BuildTarget

@ApiStatus.Internal
object CcTargetUtils {

  /** Returns the keys of all cc_toolchain targets in the snapshot. */
  fun findAllToolchains(snapshot: WorkspaceSnapshot): Set<WorkspaceTargetKey> {
    return snapshot.allTargets
      .filter { it.hasBuildData<CcToolchainBuildTarget>() }
      .map { it.key }
      .toSet()
  }

  /** Returns the toolchain of a CC target, or null if the toolchain is ambiguous or not found. */
  fun findToolchain(snapshot: WorkspaceSnapshot, target: BuildTarget): WorkspaceTargetKey? {
    return findTargetToolchain(target, findAllToolchains(snapshot)).singleOrNull()
  }

  /** Returns the compiler of a CC target, or null if the toolchain or its compiler is unknown. */
  fun findCompiler(project: Project, snapshot: WorkspaceSnapshot, target: BuildTarget): CcCompilerInfo? {
    return findToolchain(snapshot, target)?.let { findToolchainCompiler(project, it) }
  }

  /** Returns the compiler of a cc_toolchain target. */
  fun findToolchainCompiler(project: Project, toolchain: WorkspaceTargetKey): CcCompilerInfo? {
    return CcCompilerInfoService.getInstance(project).get(toolchain)
  }
}
