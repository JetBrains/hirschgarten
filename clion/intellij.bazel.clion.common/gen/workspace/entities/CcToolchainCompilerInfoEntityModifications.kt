@file:JvmName("CcToolchainCompilerInfoEntityModifications")

package org.jetbrains.bazel.clion.workspace.entities

import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.EntityType
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntityBuilder
import com.intellij.platform.workspace.storage.impl.containers.toMutableWorkspaceList
import org.jetbrains.annotations.ApiStatus.Internal
import org.jetbrains.bazel.clion.workspace.entities.impl.CcToolchainCompilerInfoEntityImpl
import org.jetbrains.bazel.workspacemodel.entities.WorkspaceModelTargetKey
import org.jetbrains.bsp.protocol.OutputLocation

@Internal
@GeneratedCodeApiVersion(3)
interface CcToolchainCompilerInfoEntityBuilder : WorkspaceEntityBuilder<CcToolchainCompilerInfoEntity> {
  override var entitySource: EntitySource
  var _toolchainKey: WorkspaceModelTargetKey
  var cCompilerPath: String
  var cCompilerKindId: String
  var cppCompilerPath: String
  var cppCompilerKindId: String
  var cSwitches: MutableList<String>
  var cppSwitches: MutableList<String>
  var compilerName: String
  var environment: Map<String, String>
  var builtinIncludes: MutableList<OutputLocation>
  var sysroot: OutputLocation?
}

internal object CcToolchainCompilerInfoEntityType : EntityType<CcToolchainCompilerInfoEntity, CcToolchainCompilerInfoEntityBuilder>() {
  override val entityImplClass: Class<*> get() = CcToolchainCompilerInfoEntityImpl::class.java
  override val entityImplBuilderClass: Class<*> get() = CcToolchainCompilerInfoEntityImpl.Builder::class.java
  operator fun invoke(
    _toolchainKey: WorkspaceModelTargetKey,
    cCompilerPath: String,
    cCompilerKindId: String,
    cppCompilerPath: String,
    cppCompilerKindId: String,
    cSwitches: List<String>,
    cppSwitches: List<String>,
    compilerName: String,
    environment: Map<String, String>,
    builtinIncludes: List<OutputLocation>,
    entitySource: EntitySource,
    init: (CcToolchainCompilerInfoEntityBuilder.() -> Unit)? = null,
  ): CcToolchainCompilerInfoEntityBuilder {
    val builder = builder()
    builder._toolchainKey = _toolchainKey
    builder.cCompilerPath = cCompilerPath
    builder.cCompilerKindId = cCompilerKindId
    builder.cppCompilerPath = cppCompilerPath
    builder.cppCompilerKindId = cppCompilerKindId
    builder.cSwitches = cSwitches.toMutableWorkspaceList()
    builder.cppSwitches = cppSwitches.toMutableWorkspaceList()
    builder.compilerName = compilerName
    builder.environment = environment
    builder.builtinIncludes = builtinIncludes.toMutableWorkspaceList()
    builder.entitySource = entitySource
    init?.invoke(builder)
    return builder
  }
}

@Internal
fun MutableEntityStorage.modifyCcToolchainCompilerInfoEntity(
  entity: CcToolchainCompilerInfoEntity,
  modification: CcToolchainCompilerInfoEntityBuilder.() -> Unit,
): CcToolchainCompilerInfoEntity = modifyEntity(CcToolchainCompilerInfoEntityBuilder::class.java, entity, modification)

@Internal
@JvmOverloads
@JvmName("createCcToolchainCompilerInfoEntity")
fun CcToolchainCompilerInfoEntity(
  _toolchainKey: WorkspaceModelTargetKey,
  cCompilerPath: String,
  cCompilerKindId: String,
  cppCompilerPath: String,
  cppCompilerKindId: String,
  cSwitches: List<String>,
  cppSwitches: List<String>,
  compilerName: String,
  environment: Map<String, String>,
  builtinIncludes: List<OutputLocation>,
  entitySource: EntitySource,
  init: (CcToolchainCompilerInfoEntityBuilder.() -> Unit)? = null,
): CcToolchainCompilerInfoEntityBuilder = CcToolchainCompilerInfoEntityType(
  _toolchainKey,
  cCompilerPath,
  cCompilerKindId,
  cppCompilerPath,
  cppCompilerKindId,
  cSwitches,
  cppSwitches,
  compilerName,
  environment,
  builtinIncludes,
  entitySource,
  init,
)
