@file:OptIn(EntityStorageInstrumentationApi::class)

package org.jetbrains.bazel.workspacemodel.entities.impl

import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntityBuilder
import com.intellij.platform.workspace.storage.ConnectionId
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.GeneratedCodeImplVersion
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.WorkspaceEntityBuilder
import com.intellij.platform.workspace.storage.WorkspaceEntityInternalApi
import com.intellij.platform.workspace.storage.impl.EntityLink
import com.intellij.platform.workspace.storage.impl.ModifiableWorkspaceEntityBase
import com.intellij.platform.workspace.storage.impl.WorkspaceEntityBase
import com.intellij.platform.workspace.storage.impl.WorkspaceEntityData
import com.intellij.platform.workspace.storage.instrumentation.EntityStorageInstrumentation
import com.intellij.platform.workspace.storage.instrumentation.EntityStorageInstrumentationApi
import com.intellij.platform.workspace.storage.instrumentation.MutableEntityStorageInstrumentation
import com.intellij.platform.workspace.storage.instrumentation.instrumentation
import com.intellij.platform.workspace.storage.metadata.model.EntityMetadata
import org.jetbrains.annotations.ApiStatus.Internal
import org.jetbrains.bazel.workspacemodel.entities.BazelModuleExtensionEntity
import org.jetbrains.bazel.workspacemodel.entities.BazelModuleExtensionEntityBuilder
import org.jetbrains.bazel.workspacemodel.entities.WorkspaceModelTargetLabel
import org.jetbrains.bazel.workspacemodel.entities.WorkspaceModelTargetLabelList

@Internal
@GeneratedCodeApiVersion(3)
@GeneratedCodeImplVersion(7)
@OptIn(WorkspaceEntityInternalApi::class)
internal class BazelModuleExtensionEntityImpl(private val dataSource: BazelModuleExtensionEntityData) : BazelModuleExtensionEntity,
                                                                                                        WorkspaceEntityBase(dataSource) {

  private companion object {
    internal val MODULE_CONNECTION_ID: ConnectionId =
      ConnectionId.create(ModuleEntity::class.java, BazelModuleExtensionEntity::class.java, ConnectionId.ConnectionType.ONE_TO_ONE, false)
    private val connections = listOf<ConnectionId>(MODULE_CONNECTION_ID)

  }

  override val module: ModuleEntity
    get() = snapshot.instrumentation.getParent(MODULE_CONNECTION_ID, this) as? ModuleEntity
            ?: error("Parent module not found for BazelModuleExtensionEntity")
  override val label: WorkspaceModelTargetLabel
    get() {
      readField("label")
      return dataSource.label
    }
  override val strictDependencies: WorkspaceModelTargetLabelList
    get() {
      readField("strictDependencies")
      return dataSource.strictDependencies
    }

  override val entitySource: EntitySource
    get() {
      readField("entitySource")
      return dataSource.entitySource
    }

  override fun connectionIdList(): List<ConnectionId> {
    return connections
  }


  internal class Builder(result: BazelModuleExtensionEntityData?) :
    ModifiableWorkspaceEntityBase<BazelModuleExtensionEntity, BazelModuleExtensionEntityData>(result), BazelModuleExtensionEntityBuilder {
    internal constructor() : this(BazelModuleExtensionEntityData())

    override fun applyToBuilder(builder: MutableEntityStorage) {
      if (this.diff != null) {
        if (existsInBuilder(builder)) {
          this.diff = builder
          return
        }
        else {
          error("Entity BazelModuleExtensionEntity is already created in a different builder")
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
      if (_diff != null) {
        if (_diff.instrumentation.getParentBuilder(MODULE_CONNECTION_ID, this) == null) {
          error("Field BazelModuleExtensionEntity#module should be initialized")
        }
      }
      else {
        if (this.entityLinks[EntityLink(false, MODULE_CONNECTION_ID)] == null) {
          error("Field BazelModuleExtensionEntity#module should be initialized")
        }
      }
      if (!getEntityData().isLabelInitialized()) {
        error("Field BazelModuleExtensionEntity#label should be initialized")
      }
      if (!getEntityData().isStrictDependenciesInitialized()) {
        error("Field BazelModuleExtensionEntity#strictDependencies should be initialized")
      }
    }

    override fun connectionIdList(): List<ConnectionId> {
      return connections
    }

    // Relabeling code, move information from dataSource to this builder
    override fun relabel(dataSource: WorkspaceEntity, parents: Set<WorkspaceEntity>?) {
      dataSource as BazelModuleExtensionEntity
      if (this.entitySource != dataSource.entitySource) this.entitySource = dataSource.entitySource
      if (this.label != dataSource.label) this.label = dataSource.label
      if (this.strictDependencies != dataSource.strictDependencies) this.strictDependencies = dataSource.strictDependencies
      updateChildToParentReferences(parents)
    }


    override var entitySource: EntitySource
      get() = getEntityData().entitySource
      set(value) {
        checkModificationAllowed()
        getEntityData(true).entitySource = value
        changedProperty.add("entitySource")

      }
    override var module: ModuleEntityBuilder
      get() {
        val _diff = diff
        return if (_diff != null) {
          ((_diff as MutableEntityStorageInstrumentation).getParentBuilder(MODULE_CONNECTION_ID, this) as? ModuleEntityBuilder)
          ?: (this.entityLinks[EntityLink(false, MODULE_CONNECTION_ID)] as? ModuleEntityBuilder)
          ?: error("module is null for BazelModuleExtensionEntity")
        }
        else {
          (this.entityLinks[EntityLink(false, MODULE_CONNECTION_ID)] as? ModuleEntityBuilder)
          ?: error("module is null for BazelModuleExtensionEntity")
        }
      }
      set(value) {
        checkModificationAllowed()
        val _diff = diff
        if (_diff != null && value is ModifiableWorkspaceEntityBase<*, *> && value.diff == null) {
          if (value is ModifiableWorkspaceEntityBase<*, *>) {
            value.entityLinks[EntityLink(true, MODULE_CONNECTION_ID)] = this
          }
// else you're attaching a new entity to an existing entity that is not modifiable
          _diff.addEntity(value as ModifiableWorkspaceEntityBase<WorkspaceEntity, *>)
        }
        if (_diff != null && (value !is ModifiableWorkspaceEntityBase<*, *> || value.diff != null)) {
          _diff.instrumentation.addChild(MODULE_CONNECTION_ID, value, this)
        }
        else {
          if (value is ModifiableWorkspaceEntityBase<*, *>) {
            value.entityLinks[EntityLink(true, MODULE_CONNECTION_ID)] = this
          }
// else you're attaching a new entity to an existing entity that is not modifiable
          this.entityLinks[EntityLink(false, MODULE_CONNECTION_ID)] = value
        }
        changedProperty.add("module")
      }

    override var label: WorkspaceModelTargetLabel
      get() = getEntityData().label
      set(value) {
        checkModificationAllowed()
        getEntityData(true).label = value
        changedProperty.add("label")

      }
    override var strictDependencies: WorkspaceModelTargetLabelList
      get() = getEntityData().strictDependencies
      set(value) {
        checkModificationAllowed()
        getEntityData(true).strictDependencies = value
        changedProperty.add("strictDependencies")

      }

    override fun getEntityClass(): Class<BazelModuleExtensionEntity> = BazelModuleExtensionEntity::class.java
  }

}

@OptIn(WorkspaceEntityInternalApi::class)
internal class BazelModuleExtensionEntityData : WorkspaceEntityData<BazelModuleExtensionEntity>() {
  lateinit var label: WorkspaceModelTargetLabel
  lateinit var strictDependencies: WorkspaceModelTargetLabelList

  internal fun isLabelInitialized(): Boolean = ::label.isInitialized
  internal fun isStrictDependenciesInitialized(): Boolean = ::strictDependencies.isInitialized

  override fun wrapAsModifiable(diff: MutableEntityStorage): WorkspaceEntityBuilder<BazelModuleExtensionEntity> {
    val modifiable = BazelModuleExtensionEntityImpl.Builder(null)
    modifiable.diff = diff
    modifiable.id = createEntityId()
    return modifiable
  }

  override fun createEntity(snapshot: EntityStorageInstrumentation): BazelModuleExtensionEntity {
    val entityId = createEntityId()
    return snapshot.initializeEntity(entityId) {
      val entity = BazelModuleExtensionEntityImpl(this)
      entity.snapshot = snapshot
      entity.id = entityId
      entity
    }
  }

  override fun getMetadata(): EntityMetadata {
    return MetadataStorageImpl.getMetadataByTypeFqn("org.jetbrains.bazel.workspacemodel.entities.BazelModuleExtensionEntity") as EntityMetadata
  }

  override fun getEntityInterface(): Class<out WorkspaceEntity> {
    return BazelModuleExtensionEntity::class.java
  }

  override fun createDetachedEntity(parents: List<WorkspaceEntityBuilder<*>>): WorkspaceEntityBuilder<*> {
    return BazelModuleExtensionEntity(label, strictDependencies, entitySource) {
      parents.filterIsInstance<ModuleEntityBuilder>().singleOrNull()?.let { this.module = it }
    }
  }

  override fun getRequiredParents(): List<Class<out WorkspaceEntity>> {
    val res = mutableListOf<Class<out WorkspaceEntity>>()
    res.add(ModuleEntity::class.java)
    return res
  }

  override fun equals(other: Any?): Boolean {
    if (other == null) return false
    if (this.javaClass != other.javaClass) return false
    other as BazelModuleExtensionEntityData
    if (this.entitySource != other.entitySource) return false
    if (this.label != other.label) return false
    if (this.strictDependencies != other.strictDependencies) return false
    return true
  }

  override fun equalsIgnoringEntitySource(other: Any?): Boolean {
    if (other == null) return false
    if (this.javaClass != other.javaClass) return false
    other as BazelModuleExtensionEntityData
    if (this.label != other.label) return false
    if (this.strictDependencies != other.strictDependencies) return false
    return true
  }

  override fun hashCode(): Int {
    var result = entitySource.hashCode()
    result = 31 * result + label.hashCode()
    result = 31 * result + strictDependencies.hashCode()
    return result
  }

  override fun hashCodeIgnoringEntitySource(): Int {
    var result = javaClass.hashCode()
    result = 31 * result + label.hashCode()
    result = 31 * result + strictDependencies.hashCode()
    return result
  }
}
