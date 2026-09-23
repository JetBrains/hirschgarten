package org.jetbrains.bazel.clion.workspace

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.workspace.storage.EntityStorage
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerId
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerKind
import com.jetbrains.cidr.lang.workspace.compiler.UnknownCompilerKind
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.clion.workspace.entities.CcToolchainCompilerInfoEntity
import org.jetbrains.bazel.clion.workspace.entities.toolchainKey
import org.jetbrains.bazel.sync.workspace.snapshot.OutputLocationCollectionBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import java.nio.file.Path

@ApiStatus.Internal
@Service(Service.Level.PROJECT)
class CcCompilerInfoService(private val project: Project) {

  private class Cached(val storage: EntityStorage, val target2Compiler: Map<WorkspaceTargetKey, CcCompilerInfo>)

  @Volatile
  private var cached: Cached? = null

  fun get(key: WorkspaceTargetKey): CcCompilerInfo? = all()[key]

  fun all(): Map<WorkspaceTargetKey, CcCompilerInfo> {
    val storage = project.workspaceModel.currentSnapshot

    cached?.let { if (it.storage === storage) return it.target2Compiler }

    val target2Compiler = loadCompilerSettings(storage)
    cached = Cached(storage, target2Compiler)

    return target2Compiler
  }

  companion object {

    fun getInstance(project: Project): CcCompilerInfoService = project.service()
  }
}

private fun loadCompilerSettings(storage: EntityStorage): Map<WorkspaceTargetKey, CcCompilerInfo> {
  return storage
    .entities(CcToolchainCompilerInfoEntity::class.java)
    .associate { entity -> entity.toolchainKey to entity.toCompilerInfo() }
}

private fun compilerKindById(id: String): OCCompilerKind = when (id) {
  OCCompilerId.CLANG.toString() -> CcCompilerKind.CLANG
  OCCompilerId.GCC.toString() -> CcCompilerKind.GCC
  else -> UnknownCompilerKind
}

private fun CcToolchainCompilerInfoEntity.toCompilerInfo(): CcCompilerInfo = CcCompilerInfo(
  cCompiler = Path.of(cCompilerPath),
  cCompilerKind = compilerKindById(cCompilerKindId),
  cppCompiler = Path.of(cppCompilerPath),
  cppCompilerKind = compilerKindById(cppCompilerKindId),
  cSwitches = cSwitches.toList(),
  cppSwitches = cppSwitches.toList(),
  name = compilerName,
  environment = environment.toMap(),
  builtinIncludes = OutputLocationCollectionBuilder.ofLocations(builtinIncludes),
  sysroot = sysroot,
)
