@file:OptIn(EntityStorageInstrumentationApi::class)

package org.jetbrains.bazel.clion.workspace.entities.impl

import com.intellij.platform.workspace.storage.ConnectionId
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.GeneratedCodeImplVersion
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.WorkspaceEntityBuilder
import com.intellij.platform.workspace.storage.WorkspaceEntityInternalApi
import com.intellij.platform.workspace.storage.impl.ModifiableWorkspaceEntityBase
import com.intellij.platform.workspace.storage.impl.WorkspaceEntityBase
import com.intellij.platform.workspace.storage.impl.WorkspaceEntityData
import com.intellij.platform.workspace.storage.impl.containers.MutableWorkspaceList
import com.intellij.platform.workspace.storage.impl.containers.toMutableWorkspaceList
import com.intellij.platform.workspace.storage.instrumentation.EntityStorageInstrumentationApi
import com.intellij.platform.workspace.storage.metadata.model.EntityMetadata
import org.jetbrains.annotations.ApiStatus.Internal
import org.jetbrains.bazel.clion.workspace.entities.CcToolchainCompilerInfoEntity
import org.jetbrains.bazel.clion.workspace.entities.CcToolchainCompilerInfoEntityBuilder
import org.jetbrains.bazel.workspacemodel.entities.WorkspaceModelTargetKey
import org.jetbrains.bsp.protocol.OutputLocation

@Internal
@GeneratedCodeApiVersion(3)
@GeneratedCodeImplVersion(7)
@OptIn(WorkspaceEntityInternalApi::class)
internal class CcToolchainCompilerInfoEntityImpl(private val dataSource: CcToolchainCompilerInfoEntityData) : CcToolchainCompilerInfoEntity,
                                                                                                              WorkspaceEntityBase(dataSource) {

  override val _toolchainKey: WorkspaceModelTargetKey
    get() {
      readField("_toolchainKey")
      return dataSource._toolchainKey
    }
  override val cCompilerPath: String
    get() {
      readField("cCompilerPath")
      return dataSource.cCompilerPath
    }
  override val cCompilerKindId: String
    get() {
      readField("cCompilerKindId")
      return dataSource.cCompilerKindId
    }
  override val cppCompilerPath: String
    get() {
      readField("cppCompilerPath")
      return dataSource.cppCompilerPath
    }
  override val cppCompilerKindId: String
    get() {
      readField("cppCompilerKindId")
      return dataSource.cppCompilerKindId
    }
  override val cSwitches: List<String>
    get() {
      readField("cSwitches")
      return dataSource.cSwitches
    }
  override val cppSwitches: List<String>
    get() {
      readField("cppSwitches")
      return dataSource.cppSwitches
    }
  override val compilerName: String
    get() {
      readField("compilerName")
      return dataSource.compilerName
    }
  override val environment: Map<String, String>
    get() {
      readField("environment")
      return dataSource.environment
    }
  override val builtinIncludes: List<OutputLocation>
    get() {
      readField("builtinIncludes")
      return dataSource.builtinIncludes
    }
  override val sysroot: OutputLocation?
    get() {
      readField("sysroot")
      return dataSource.sysroot
    }
  override val entitySource: EntitySource
    get() {
      readField("entitySource")
      return dataSource.entitySource
    }

  override fun connectionIdList(): List<ConnectionId> {
    return emptyList()
  }

  internal class Builder(result: CcToolchainCompilerInfoEntityData?) :
    ModifiableWorkspaceEntityBase<CcToolchainCompilerInfoEntity, CcToolchainCompilerInfoEntityData>(result),
    CcToolchainCompilerInfoEntityBuilder {
    internal constructor() : this(CcToolchainCompilerInfoEntityData())

    override fun checkInitialization() {
      val _diff = diff
      if (!getEntityData().isEntitySourceInitialized()) {
        error("Field WorkspaceEntity#entitySource should be initialized")
      }
      if (!getEntityData().is_toolchainKeyInitialized()) {
        error("Field CcToolchainCompilerInfoEntity#_toolchainKey should be initialized")
      }
      if (!getEntityData().isCCompilerPathInitialized()) {
        error("Field CcToolchainCompilerInfoEntity#cCompilerPath should be initialized")
      }
      if (!getEntityData().isCCompilerKindIdInitialized()) {
        error("Field CcToolchainCompilerInfoEntity#cCompilerKindId should be initialized")
      }
      if (!getEntityData().isCppCompilerPathInitialized()) {
        error("Field CcToolchainCompilerInfoEntity#cppCompilerPath should be initialized")
      }
      if (!getEntityData().isCppCompilerKindIdInitialized()) {
        error("Field CcToolchainCompilerInfoEntity#cppCompilerKindId should be initialized")
      }
      if (!getEntityData().isCSwitchesInitialized()) {
        error("Field CcToolchainCompilerInfoEntity#cSwitches should be initialized")
      }
      if (!getEntityData().isCppSwitchesInitialized()) {
        error("Field CcToolchainCompilerInfoEntity#cppSwitches should be initialized")
      }
      if (!getEntityData().isCompilerNameInitialized()) {
        error("Field CcToolchainCompilerInfoEntity#compilerName should be initialized")
      }
      if (!getEntityData().isEnvironmentInitialized()) {
        error("Field CcToolchainCompilerInfoEntity#environment should be initialized")
      }
      if (!getEntityData().isBuiltinIncludesInitialized()) {
        error("Field CcToolchainCompilerInfoEntity#builtinIncludes should be initialized")
      }
    }

    override fun connectionIdList(): List<ConnectionId> {
      return emptyList()
    }

    override fun afterModification() {
      val collection_cSwitches = getEntityData().cSwitches
      if (collection_cSwitches is MutableWorkspaceList<*>) {
        collection_cSwitches.cleanModificationUpdateAction()
      }
      val collection_cppSwitches = getEntityData().cppSwitches
      if (collection_cppSwitches is MutableWorkspaceList<*>) {
        collection_cppSwitches.cleanModificationUpdateAction()
      }
      val collection_builtinIncludes = getEntityData().builtinIncludes
      if (collection_builtinIncludes is MutableWorkspaceList<*>) {
        collection_builtinIncludes.cleanModificationUpdateAction()
      }
    }

    // Relabeling code, move information from dataSource to this builder
    override fun relabel(dataSource: WorkspaceEntity, parents: Set<WorkspaceEntity>?) {
      dataSource as CcToolchainCompilerInfoEntity
      if (this.entitySource != dataSource.entitySource) this.entitySource = dataSource.entitySource
      if (this._toolchainKey != dataSource._toolchainKey) this._toolchainKey = dataSource._toolchainKey
      if (this.cCompilerPath != dataSource.cCompilerPath) this.cCompilerPath = dataSource.cCompilerPath
      if (this.cCompilerKindId != dataSource.cCompilerKindId) this.cCompilerKindId = dataSource.cCompilerKindId
      if (this.cppCompilerPath != dataSource.cppCompilerPath) this.cppCompilerPath = dataSource.cppCompilerPath
      if (this.cppCompilerKindId != dataSource.cppCompilerKindId) this.cppCompilerKindId = dataSource.cppCompilerKindId
      if (this.cSwitches != dataSource.cSwitches) this.cSwitches = dataSource.cSwitches.toMutableList()
      if (this.cppSwitches != dataSource.cppSwitches) this.cppSwitches = dataSource.cppSwitches.toMutableList()
      if (this.compilerName != dataSource.compilerName) this.compilerName = dataSource.compilerName
      if (this.environment != dataSource.environment) this.environment = dataSource.environment.toMutableMap()
      if (this.builtinIncludes != dataSource.builtinIncludes) this.builtinIncludes = dataSource.builtinIncludes.toMutableList()
      if (this.sysroot != dataSource.sysroot) this.sysroot = dataSource.sysroot
      updateChildToParentReferences(parents)
    }

    override var entitySource: EntitySource
      get() = getEntityData().entitySource
      set(value) {
        checkModificationAllowed()
        getEntityData(true).entitySource = value
        changedProperty.add("entitySource")
      }
    override var _toolchainKey: WorkspaceModelTargetKey
      get() = getEntityData()._toolchainKey
      set(value) {
        checkModificationAllowed()
        getEntityData(true)._toolchainKey = value
        changedProperty.add("_toolchainKey")
      }
    override var cCompilerPath: String
      get() = getEntityData().cCompilerPath
      set(value) {
        checkModificationAllowed()
        getEntityData(true).cCompilerPath = value
        changedProperty.add("cCompilerPath")
      }
    override var cCompilerKindId: String
      get() = getEntityData().cCompilerKindId
      set(value) {
        checkModificationAllowed()
        getEntityData(true).cCompilerKindId = value
        changedProperty.add("cCompilerKindId")
      }
    override var cppCompilerPath: String
      get() = getEntityData().cppCompilerPath
      set(value) {
        checkModificationAllowed()
        getEntityData(true).cppCompilerPath = value
        changedProperty.add("cppCompilerPath")
      }
    override var cppCompilerKindId: String
      get() = getEntityData().cppCompilerKindId
      set(value) {
        checkModificationAllowed()
        getEntityData(true).cppCompilerKindId = value
        changedProperty.add("cppCompilerKindId")
      }
    private val cSwitchesUpdater: (value: List<String>) -> Unit = { value ->
      changedProperty.add("cSwitches")
    }
    override var cSwitches: MutableList<String>
      get() {
        val collection_cSwitches = getEntityData().cSwitches
        if (collection_cSwitches !is MutableWorkspaceList) return collection_cSwitches
        if (diff == null || modifiable.get()) {
          collection_cSwitches.setModificationUpdateAction(cSwitchesUpdater)
        }
        else {
          collection_cSwitches.cleanModificationUpdateAction()
        }
        return collection_cSwitches
      }
      set(value) {
        checkModificationAllowed()
        getEntityData(true).cSwitches = value
        cSwitchesUpdater.invoke(value)
      }
    private val cppSwitchesUpdater: (value: List<String>) -> Unit = { value ->
      changedProperty.add("cppSwitches")
    }
    override var cppSwitches: MutableList<String>
      get() {
        val collection_cppSwitches = getEntityData().cppSwitches
        if (collection_cppSwitches !is MutableWorkspaceList) return collection_cppSwitches
        if (diff == null || modifiable.get()) {
          collection_cppSwitches.setModificationUpdateAction(cppSwitchesUpdater)
        }
        else {
          collection_cppSwitches.cleanModificationUpdateAction()
        }
        return collection_cppSwitches
      }
      set(value) {
        checkModificationAllowed()
        getEntityData(true).cppSwitches = value
        cppSwitchesUpdater.invoke(value)
      }
    override var compilerName: String
      get() = getEntityData().compilerName
      set(value) {
        checkModificationAllowed()
        getEntityData(true).compilerName = value
        changedProperty.add("compilerName")
      }
    override var environment: Map<String, String>
      get() = getEntityData().environment
      set(value) {
        checkModificationAllowed()
        getEntityData(true).environment = value
        changedProperty.add("environment")
      }
    private val builtinIncludesUpdater: (value: List<OutputLocation>) -> Unit = { value ->
      changedProperty.add("builtinIncludes")
    }
    override var builtinIncludes: MutableList<OutputLocation>
      get() {
        val collection_builtinIncludes = getEntityData().builtinIncludes
        if (collection_builtinIncludes !is MutableWorkspaceList) return collection_builtinIncludes
        if (diff == null || modifiable.get()) {
          collection_builtinIncludes.setModificationUpdateAction(builtinIncludesUpdater)
        }
        else {
          collection_builtinIncludes.cleanModificationUpdateAction()
        }
        return collection_builtinIncludes
      }
      set(value) {
        checkModificationAllowed()
        getEntityData(true).builtinIncludes = value
        builtinIncludesUpdater.invoke(value)
      }
    override var sysroot: OutputLocation?
      get() = getEntityData().sysroot
      set(value) {
        checkModificationAllowed()
        getEntityData(true).sysroot = value
        changedProperty.add("sysroot")
      }

    override fun getEntityClass(): Class<CcToolchainCompilerInfoEntity> = CcToolchainCompilerInfoEntity::class.java
  }
}

@OptIn(WorkspaceEntityInternalApi::class)
internal class CcToolchainCompilerInfoEntityData : WorkspaceEntityData<CcToolchainCompilerInfoEntity>() {
  lateinit var _toolchainKey: WorkspaceModelTargetKey
  lateinit var cCompilerPath: String
  lateinit var cCompilerKindId: String
  lateinit var cppCompilerPath: String
  lateinit var cppCompilerKindId: String
  lateinit var cSwitches: MutableList<String>
  lateinit var cppSwitches: MutableList<String>
  lateinit var compilerName: String
  lateinit var environment: Map<String, String>
  lateinit var builtinIncludes: MutableList<OutputLocation>
  var sysroot: OutputLocation? = null
  internal fun is_toolchainKeyInitialized(): Boolean = ::_toolchainKey.isInitialized
  internal fun isCCompilerPathInitialized(): Boolean = ::cCompilerPath.isInitialized
  internal fun isCCompilerKindIdInitialized(): Boolean = ::cCompilerKindId.isInitialized
  internal fun isCppCompilerPathInitialized(): Boolean = ::cppCompilerPath.isInitialized
  internal fun isCppCompilerKindIdInitialized(): Boolean = ::cppCompilerKindId.isInitialized
  internal fun isCSwitchesInitialized(): Boolean = ::cSwitches.isInitialized
  internal fun isCppSwitchesInitialized(): Boolean = ::cppSwitches.isInitialized
  internal fun isCompilerNameInitialized(): Boolean = ::compilerName.isInitialized
  internal fun isEnvironmentInitialized(): Boolean = ::environment.isInitialized
  internal fun isBuiltinIncludesInitialized(): Boolean = ::builtinIncludes.isInitialized
  override fun newInstance(): CcToolchainCompilerInfoEntity = CcToolchainCompilerInfoEntityImpl(this)
  override fun newBuilderInstance(): ModifiableWorkspaceEntityBase<CcToolchainCompilerInfoEntity, *> =
    CcToolchainCompilerInfoEntityImpl.Builder(null)

  override fun getMetadata(): EntityMetadata {
    return MetadataStorageImpl.getMetadataByTypeFqn("org.jetbrains.bazel.clion.workspace.entities.CcToolchainCompilerInfoEntity") as EntityMetadata
  }

  override fun clone(): CcToolchainCompilerInfoEntityData {
    val clonedEntity = super.clone()
    clonedEntity as CcToolchainCompilerInfoEntityData
    clonedEntity.cSwitches = clonedEntity.cSwitches.toMutableWorkspaceList()
    clonedEntity.cppSwitches = clonedEntity.cppSwitches.toMutableWorkspaceList()
    clonedEntity.builtinIncludes = clonedEntity.builtinIncludes.toMutableWorkspaceList()
    return clonedEntity
  }

  override fun getEntityInterface(): Class<out WorkspaceEntity> {
    return CcToolchainCompilerInfoEntity::class.java
  }

  override fun createDetachedEntity(parents: List<WorkspaceEntityBuilder<*>>): WorkspaceEntityBuilder<*> {
    return CcToolchainCompilerInfoEntity(
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
    ) {
      this.sysroot = this@CcToolchainCompilerInfoEntityData.sysroot
    }
  }

  override fun getRequiredParents(): List<Class<out WorkspaceEntity>> {
    val res = mutableListOf<Class<out WorkspaceEntity>>()
    return res
  }

  override fun equals(other: Any?): Boolean {
    if (other == null) return false
    if (this.javaClass != other.javaClass) return false
    other as CcToolchainCompilerInfoEntityData
    if (this.entitySource != other.entitySource) return false
    if (this._toolchainKey != other._toolchainKey) return false
    if (this.cCompilerPath != other.cCompilerPath) return false
    if (this.cCompilerKindId != other.cCompilerKindId) return false
    if (this.cppCompilerPath != other.cppCompilerPath) return false
    if (this.cppCompilerKindId != other.cppCompilerKindId) return false
    if (this.cSwitches != other.cSwitches) return false
    if (this.cppSwitches != other.cppSwitches) return false
    if (this.compilerName != other.compilerName) return false
    if (this.environment != other.environment) return false
    if (this.builtinIncludes != other.builtinIncludes) return false
    if (this.sysroot != other.sysroot) return false
    return true
  }

  override fun equalsIgnoringEntitySource(other: Any?): Boolean {
    if (other == null) return false
    if (this.javaClass != other.javaClass) return false
    other as CcToolchainCompilerInfoEntityData
    if (this._toolchainKey != other._toolchainKey) return false
    if (this.cCompilerPath != other.cCompilerPath) return false
    if (this.cCompilerKindId != other.cCompilerKindId) return false
    if (this.cppCompilerPath != other.cppCompilerPath) return false
    if (this.cppCompilerKindId != other.cppCompilerKindId) return false
    if (this.cSwitches != other.cSwitches) return false
    if (this.cppSwitches != other.cppSwitches) return false
    if (this.compilerName != other.compilerName) return false
    if (this.environment != other.environment) return false
    if (this.builtinIncludes != other.builtinIncludes) return false
    if (this.sysroot != other.sysroot) return false
    return true
  }

  override fun hashCode(): Int {
    var result = entitySource.hashCode()
    result = 31 * result + _toolchainKey.hashCode()
    result = 31 * result + cCompilerPath.hashCode()
    result = 31 * result + cCompilerKindId.hashCode()
    result = 31 * result + cppCompilerPath.hashCode()
    result = 31 * result + cppCompilerKindId.hashCode()
    result = 31 * result + cSwitches.hashCode()
    result = 31 * result + cppSwitches.hashCode()
    result = 31 * result + compilerName.hashCode()
    result = 31 * result + environment.hashCode()
    result = 31 * result + builtinIncludes.hashCode()
    result = 31 * result + sysroot.hashCode()
    return result
  }

  override fun hashCodeIgnoringEntitySource(): Int {
    var result = javaClass.hashCode()
    result = 31 * result + _toolchainKey.hashCode()
    result = 31 * result + cCompilerPath.hashCode()
    result = 31 * result + cCompilerKindId.hashCode()
    result = 31 * result + cppCompilerPath.hashCode()
    result = 31 * result + cppCompilerKindId.hashCode()
    result = 31 * result + cSwitches.hashCode()
    result = 31 * result + cppSwitches.hashCode()
    result = 31 * result + compilerName.hashCode()
    result = 31 * result + environment.hashCode()
    result = 31 * result + builtinIncludes.hashCode()
    result = 31 * result + sysroot.hashCode()
    return result
  }
}
