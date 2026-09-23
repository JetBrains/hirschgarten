package org.jetbrains.bazel.clion.workspace.entities

import com.intellij.platform.workspace.storage.WorkspaceEntity
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.workspacemodel.entities.WorkspaceModelTargetKey
import org.jetbrains.bsp.protocol.OutputLocation

@ApiStatus.Internal
interface CcToolchainCompilerInfoEntity : WorkspaceEntity {
  val _toolchainKey: WorkspaceModelTargetKey
  val cCompilerPath: String
  val cCompilerKindId: String
  val cppCompilerPath: String
  val cppCompilerKindId: String
  val cSwitches: List<String>
  val cppSwitches: List<String>
  val compilerName: String
  val environment: Map<String, String>
  val builtinIncludes: List<OutputLocation>
  val sysroot: OutputLocation?
}

@get:ApiStatus.Internal
val CcToolchainCompilerInfoEntity.toolchainKey: WorkspaceTargetKey
  get() = _toolchainKey.toWorkspaceTarget()
