package org.jetbrains.bazel.clion.workspace

import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.ModuleSourceDependency
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.MutableEntityStorage
import org.jetbrains.bazel.clion.workspace.entities.CcToolchainCompilerInfoEntity
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.workspacemodel.entities.WorkspaceModelTargetKey

internal const val CC_WORKSPACE_MODULE_NAME: String = ".cc-workspace"

internal fun addCcWorkspaceModule(builder: MutableEntityStorage, entitySource: EntitySource) {
  builder.addEntity(
    ModuleEntity(
      name = CC_WORKSPACE_MODULE_NAME,
      dependencies = listOf(ModuleSourceDependency),
      entitySource = entitySource,
    )
  )
}

/** `null` before the sync applies the project model, and for a project that Bazel did not import. */
internal fun findCcWorkspaceModuleId(project: Project): ModuleId? {
  val id = ModuleId(CC_WORKSPACE_MODULE_NAME)
  return if (project.workspaceModel.currentSnapshot.resolve(id) != null) id else null
}

/** Persists the compiler settings of every resolved toolchain. */
internal fun addCcCompilerInfoEntities(
  builder: MutableEntityStorage,
  entitySource: EntitySource,
  toolchain2Compiler: Map<WorkspaceTargetKey, CcCompilerInfo>,
) {
  for ((toolchainKey, info) in toolchain2Compiler) {
    builder.addEntity(
      CcToolchainCompilerInfoEntity(
        _toolchainKey = WorkspaceModelTargetKey.of(toolchainKey),
        cCompilerPath = info.cCompiler.toString(),
        cCompilerKindId = info.cCompilerKind.id.toString(),
        cppCompilerPath = info.cppCompiler.toString(),
        cppCompilerKindId = info.cppCompilerKind.id.toString(),
        cSwitches = info.cSwitches,
        cppSwitches = info.cppSwitches,
        compilerName = info.name,
        environment = info.environment,
        builtinIncludes = info.builtinIncludes.getOutputLocations().toList(),
        entitySource = entitySource,
      ) {
        sysroot = info.sysroot
      },
    )
  }
}
