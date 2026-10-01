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

  fun findToolchainKey(snapshot: WorkspaceSnapshot, target: BuildTarget): WorkspaceTargetKey? {
    val toolchains = snapshot.allTargets
      .filter { it.hasBuildData<CcToolchainBuildTarget>() }
      .map { it.key }
      .toSet()

    return findTargetToolchain(target, toolchains).singleOrNull()
  }

  /** Returns the toolchain of a CC target, or null if the toolchain is ambiguous or not found. */
  fun findToolchain(snapshot: WorkspaceSnapshot, target: BuildTarget): BuildTarget? {
    return findToolchainKey(snapshot, target)?.let { snapshot.targets.findTargetByKey(it) }
  }

  /** Returns the compiler of a CC target, or null if the toolchain or its compiler is unknown. */
  fun findCompiler(project: Project, snapshot: WorkspaceSnapshot, target: BuildTarget): CcCompilerInfo? {
    return findToolchainKey(snapshot, target)?.let { CcCompilerInfoService.getInstance(project).get(it) }
  }
}
