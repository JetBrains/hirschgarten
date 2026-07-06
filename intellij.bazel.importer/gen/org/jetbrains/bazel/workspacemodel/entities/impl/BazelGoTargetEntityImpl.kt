@file:OptIn(EntityStorageInstrumentationApi::class)

package org.jetbrains.bazel.workspacemodel.entities.impl

import com.intellij.platform.workspace.storage.ConnectionId
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.GeneratedCodeImplVersion
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.SymbolicEntityId
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.WorkspaceEntityBuilder
import com.intellij.platform.workspace.storage.WorkspaceEntityInternalApi
import com.intellij.platform.workspace.storage.impl.ModifiableWorkspaceEntityBase
import com.intellij.platform.workspace.storage.impl.SoftLinkable
import com.intellij.platform.workspace.storage.impl.WorkspaceEntityBase
import com.intellij.platform.workspace.storage.impl.WorkspaceEntityData
import com.intellij.platform.workspace.storage.impl.indices.WorkspaceMutableIndex
import com.intellij.platform.workspace.storage.instrumentation.EntityStorageInstrumentation
import com.intellij.platform.workspace.storage.instrumentation.EntityStorageInstrumentationApi
import com.intellij.platform.workspace.storage.metadata.model.EntityMetadata
import org.jetbrains.annotations.ApiStatus.Internal
import org.jetbrains.bazel.workspacemodel.entities.BazelGoTargetEntity
import org.jetbrains.bazel.workspacemodel.entities.BazelGoTargetEntityBuilder
import org.jetbrains.bazel.workspacemodel.entities.BazelGoTargetEntityId
import org.jetbrains.bazel.workspacemodel.entities.ImportPathId
import org.jetbrains.bazel.workspacemodel.entities.WorkspaceModelTargetKey

@Internal
@GeneratedCodeApiVersion(3)
@GeneratedCodeImplVersion(7)
@OptIn(WorkspaceEntityInternalApi::class)
internal class BazelGoTargetEntityImpl(private val dataSource: BazelGoTargetEntityData) : BazelGoTargetEntity,
                                                                                          WorkspaceEntityBase(dataSource) {

  private companion object {

    private val connections = listOf<ConnectionId>()

  }

  override val symbolicId: BazelGoTargetEntityId = super.symbolicId

  override val _targetKey: WorkspaceModelTargetKey
    get() {
      readField("_targetKey")
      return dataSource._targetKey
    }
  override val importPath: ImportPathId
    get() {
      readField("importPath")
      return dataSource.importPath
    }

  override val entitySource: EntitySource
    get() {
      readField("entitySource")
      return dataSource.entitySource
    }

  override fun connectionIdList(): List<ConnectionId> {
    return connections
  }


  internal class Builder(result: BazelGoTargetEntityData?) :
    ModifiableWorkspaceEntityBase<BazelGoTargetEntity, BazelGoTargetEntityData>(result), BazelGoTargetEntityBuilder {
    internal constructor() : this(BazelGoTargetEntityData())

    override fun applyToBuilder(builder: MutableEntityStorage) {
      if (this.diff != null) {
        if (existsInBuilder(builder)) {
          this.diff = builder
          return
        }
        else {
          error("Entity BazelGoTargetEntity is already created in a different builder")
        }
      }
      this.diff = builder
      addToBuilder()
      this.id = getEntityData().createEntityId()
// After adding entity data to the builder, we need to unbind it and move the control over entity data to builder
// Builder may switch to snapshot at any moment and lock entity data to modification
      this.currentEntityData = null
// Process linked entities that are connected without a builder
      processLinkedEntities(builder)
      checkInitialization() // TODO uncomment and check failed tests
    }

    private fun checkInitialization() {
      val _diff = diff
      if (!getEntityData().isEntitySourceInitialized()) {
        error("Field WorkspaceEntity#entitySource should be initialized")
      }
      if (!getEntityData().is_targetKeyInitialized()) {
        error("Field BazelGoTargetEntity#_targetKey should be initialized")
      }
      if (!getEntityData().isImportPathInitialized()) {
        error("Field BazelGoTargetEntity#importPath should be initialized")
      }
    }

    override fun connectionIdList(): List<ConnectionId> {
      return connections
    }

    // Relabeling code, move information from dataSource to this builder
    override fun relabel(dataSource: WorkspaceEntity, parents: Set<WorkspaceEntity>?) {
      dataSource as BazelGoTargetEntity
      if (this.entitySource != dataSource.entitySource) this.entitySource = dataSource.entitySource
      if (this._targetKey != dataSource._targetKey) this._targetKey = dataSource._targetKey
      if (this.importPath != dataSource.importPath) this.importPath = dataSource.importPath
      updateChildToParentReferences(parents)
    }


    override var entitySource: EntitySource
      get() = getEntityData().entitySource
      set(value) {
        checkModificationAllowed()
        getEntityData(true).entitySource = value
        changedProperty.add("entitySource")

      }
    override var _targetKey: WorkspaceModelTargetKey
      get() = getEntityData()._targetKey
      set(value) {
        checkModificationAllowed()
        getEntityData(true)._targetKey = value
        changedProperty.add("_targetKey")

      }
    override var importPath: ImportPathId
      get() = getEntityData().importPath
      set(value) {
        checkModificationAllowed()
        getEntityData(true).importPath = value
        changedProperty.add("importPath")

      }

    override fun getEntityClass(): Class<BazelGoTargetEntity> = BazelGoTargetEntity::class.java
  }

}

@OptIn(WorkspaceEntityInternalApi::class)
internal class BazelGoTargetEntityData : WorkspaceEntityData<BazelGoTargetEntity>(), SoftLinkable {
  lateinit var _targetKey: WorkspaceModelTargetKey
  lateinit var importPath: ImportPathId

  internal fun is_targetKeyInitialized(): Boolean = ::_targetKey.isInitialized
  internal fun isImportPathInitialized(): Boolean = ::importPath.isInitialized

  override fun getLinks(): Set<SymbolicEntityId<*>> {
    val result = HashSet<SymbolicEntityId<*>>()
    for (item in _targetKey.aspectIds) {
    }
    result.add(importPath)
    return result
  }

  override fun index(index: WorkspaceMutableIndex<SymbolicEntityId<*>>) {
    for (item in _targetKey.aspectIds) {
    }
    index.index(this, importPath)
  }

  override fun updateLinksIndex(prev: Set<SymbolicEntityId<*>>, index: WorkspaceMutableIndex<SymbolicEntityId<*>>) {
// TODO verify logic
    val mutablePreviousSet = HashSet(prev)
    for (item in _targetKey.aspectIds) {
    }
    val removedItem_importPath = mutablePreviousSet.remove(importPath)
    if (!removedItem_importPath) {
      index.index(this, importPath)
    }
    for (removed in mutablePreviousSet) {
      index.remove(this, removed)
    }
  }

  override fun updateLink(oldLink: SymbolicEntityId<*>, newLink: SymbolicEntityId<*>): Boolean {
    var changed = false
    val importPath_data = if (importPath == oldLink) {
      changed = true
      newLink as ImportPathId
    }
    else {
      null
    }
    if (importPath_data != null) {
      importPath = importPath_data
    }
    return changed
  }

  override fun wrapAsModifiable(diff: MutableEntityStorage): WorkspaceEntityBuilder<BazelGoTargetEntity> {
    val modifiable = BazelGoTargetEntityImpl.Builder(null)
    modifiable.diff = diff
    modifiable.id = createEntityId()
    return modifiable
  }

  override fun createEntity(snapshot: EntityStorageInstrumentation): BazelGoTargetEntity {
    val entityId = createEntityId()
    return snapshot.initializeEntity(entityId) {
      val entity = BazelGoTargetEntityImpl(this)
      entity.snapshot = snapshot
      entity.id = entityId
      entity
    }
  }

  override fun getMetadata(): EntityMetadata {
    return MetadataStorageImpl.getMetadataByTypeFqn("org.jetbrains.bazel.workspacemodel.entities.BazelGoTargetEntity") as EntityMetadata
  }

  override fun getEntityInterface(): Class<out WorkspaceEntity> {
    return BazelGoTargetEntity::class.java
  }

  override fun createDetachedEntity(parents: List<WorkspaceEntityBuilder<*>>): WorkspaceEntityBuilder<*> {
    return BazelGoTargetEntity(_targetKey, importPath, entitySource)
  }

  override fun getRequiredParents(): List<Class<out WorkspaceEntity>> {
    val res = mutableListOf<Class<out WorkspaceEntity>>()
    return res
  }

  override fun equals(other: Any?): Boolean {
    if (other == null) return false
    if (this.javaClass != other.javaClass) return false
    other as BazelGoTargetEntityData
    if (this.entitySource != other.entitySource) return false
    if (this._targetKey != other._targetKey) return false
    if (this.importPath != other.importPath) return false
    return true
  }

  override fun equalsIgnoringEntitySource(other: Any?): Boolean {
    if (other == null) return false
    if (this.javaClass != other.javaClass) return false
    other as BazelGoTargetEntityData
    if (this._targetKey != other._targetKey) return false
    if (this.importPath != other.importPath) return false
    return true
  }

  override fun hashCode(): Int {
    var result = entitySource.hashCode()
    result = 31 * result + _targetKey.hashCode()
    result = 31 * result + importPath.hashCode()
    return result
  }

  override fun hashCodeIgnoringEntitySource(): Int {
    var result = javaClass.hashCode()
    result = 31 * result + _targetKey.hashCode()
    result = 31 * result + importPath.hashCode()
    return result
  }
}
