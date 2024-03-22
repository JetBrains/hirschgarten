package org.jetbrains.workspacemodel.entities

import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.EntityInformation
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.GeneratedCodeImplVersion
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.impl.ConnectionId
import com.intellij.platform.workspace.storage.impl.EntityLink
import com.intellij.platform.workspace.storage.impl.ModifiableWorkspaceEntityBase
import com.intellij.platform.workspace.storage.impl.WorkspaceEntityBase
import com.intellij.platform.workspace.storage.impl.WorkspaceEntityData
import com.intellij.platform.workspace.storage.impl.containers.MutableWorkspaceList
import com.intellij.platform.workspace.storage.impl.containers.toMutableWorkspaceList
import com.intellij.platform.workspace.storage.impl.extractOneToOneParent
import com.intellij.platform.workspace.storage.impl.updateOneToOneParentOfChild
import com.intellij.platform.workspace.storage.instrumentation.EntityStorageInstrumentation
import com.intellij.platform.workspace.storage.instrumentation.EntityStorageInstrumentationApi
import com.intellij.platform.workspace.storage.metadata.model.EntityMetadata
import com.intellij.platform.workspace.storage.url.VirtualFileUrl

@GeneratedCodeApiVersion(2)
@GeneratedCodeImplVersion(3)
public open class JvmBinaryJarsEntityImpl(private val dataSource: JvmBinaryJarsEntityData) : JvmBinaryJarsEntity,
  WorkspaceEntityBase(dataSource) {

  private companion object {
    internal val MODULE_CONNECTION_ID: ConnectionId = ConnectionId.create(
      ModuleEntity::class.java,
      JvmBinaryJarsEntity::class.java,
      ConnectionId.ConnectionType.ONE_TO_ONE,
      false
    )

    private val connections = listOf<ConnectionId>(
      MODULE_CONNECTION_ID,
    )

  }

  override val jars: List<VirtualFileUrl>
    get() {
      readField("jars")
      return dataSource.jars
    }

  override val module: ModuleEntity
    get() = snapshot.extractOneToOneParent(MODULE_CONNECTION_ID, this)!!

  override val entitySource: EntitySource
    get() {
      readField("entitySource")
      return dataSource.entitySource
    }

  override fun connectionIdList(): List<ConnectionId> {
    return connections
  }


  public class Builder(result: JvmBinaryJarsEntityData?) :
    ModifiableWorkspaceEntityBase<JvmBinaryJarsEntity, JvmBinaryJarsEntityData>(result), JvmBinaryJarsEntity.Builder {
    public constructor() : this(JvmBinaryJarsEntityData())

    override fun applyToBuilder(builder: MutableEntityStorage) {
      if (this.diff != null) {
        if (existsInBuilder(builder)) {
          this.diff = builder
          return
        } else {
          error("Entity JvmBinaryJarsEntity is already created in a different builder")
        }
      }

      this.diff = builder
      this.snapshot = builder
      addToBuilder()
      this.id = getEntityData().createEntityId()
      // After adding entity data to the builder, we need to unbind it and move the control over entity data to builder
      // Builder may switch to snapshot at any moment and lock entity data to modification
      this.currentEntityData = null

      index(this, "jars", this.jars)
      // Process linked entities that are connected without a builder
      processLinkedEntities(builder)
      checkInitialization() // TODO uncomment and check failed tests
    }

    private fun checkInitialization() {
      val _diff = diff
      if (!getEntityData().isEntitySourceInitialized()) {
        error("Field WorkspaceEntity#entitySource should be initialized")
      }
      if (!getEntityData().isJarsInitialized()) {
        error("Field JvmBinaryJarsEntity#jars should be initialized")
      }
      if (_diff != null) {
        if (_diff.extractOneToOneParent<WorkspaceEntityBase>(MODULE_CONNECTION_ID, this) == null) {
          error("Field JvmBinaryJarsEntity#module should be initialized")
        }
      } else {
        if (this.entityLinks[EntityLink(false, MODULE_CONNECTION_ID)] == null) {
          error("Field JvmBinaryJarsEntity#module should be initialized")
        }
      }
    }

    override fun connectionIdList(): List<ConnectionId> {
      return connections
    }

    override fun afterModification() {
      val collection_jars = getEntityData().jars
      if (collection_jars is MutableWorkspaceList<*>) {
        collection_jars.cleanModificationUpdateAction()
      }
    }

    // Relabeling code, move information from dataSource to this builder
    override fun relabel(dataSource: WorkspaceEntity, parents: Set<WorkspaceEntity>?) {
      dataSource as JvmBinaryJarsEntity
      if (this.entitySource != dataSource.entitySource) this.entitySource = dataSource.entitySource
      if (this.jars != dataSource.jars) this.jars = dataSource.jars.toMutableList()
      updateChildToParentReferences(parents)
    }


    override var entitySource: EntitySource
      get() = getEntityData().entitySource
      set(value) {
        checkModificationAllowed()
        getEntityData(true).entitySource = value
        changedProperty.add("entitySource")

      }

    private val jarsUpdater: (value: List<VirtualFileUrl>) -> Unit = { value ->
      val _diff = diff
      if (_diff != null) index(this, "jars", value)
      changedProperty.add("jars")
    }
    override var jars: MutableList<VirtualFileUrl>
      get() {
        val collection_jars = getEntityData().jars
        if (collection_jars !is MutableWorkspaceList) return collection_jars
        if (diff == null || modifiable.get()) {
          collection_jars.setModificationUpdateAction(jarsUpdater)
        } else {
          collection_jars.cleanModificationUpdateAction()
        }
        return collection_jars
      }
      set(value) {
        checkModificationAllowed()
        getEntityData(true).jars = value
        jarsUpdater.invoke(value)
      }

    override var module: ModuleEntity
      get() {
        val _diff = diff
        return if (_diff != null) {
          _diff.extractOneToOneParent(MODULE_CONNECTION_ID, this) ?: this.entityLinks[EntityLink(
            false,
            MODULE_CONNECTION_ID
          )]!! as ModuleEntity
        } else {
          this.entityLinks[EntityLink(false, MODULE_CONNECTION_ID)]!! as ModuleEntity
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
          _diff.addEntity(value)
        }
        if (_diff != null && (value !is ModifiableWorkspaceEntityBase<*, *> || value.diff != null)) {
          _diff.updateOneToOneParentOfChild(MODULE_CONNECTION_ID, this, value)
        } else {
          if (value is ModifiableWorkspaceEntityBase<*, *>) {
            value.entityLinks[EntityLink(true, MODULE_CONNECTION_ID)] = this
          }
          // else you're attaching a new entity to an existing entity that is not modifiable

          this.entityLinks[EntityLink(false, MODULE_CONNECTION_ID)] = value
        }
        changedProperty.add("module")
      }

    override fun getEntityClass(): Class<JvmBinaryJarsEntity> = JvmBinaryJarsEntity::class.java
  }
}

public class JvmBinaryJarsEntityData : WorkspaceEntityData<JvmBinaryJarsEntity>() {
  public lateinit var jars: MutableList<VirtualFileUrl>

  internal fun isJarsInitialized(): Boolean = ::jars.isInitialized

  override fun wrapAsModifiable(diff: MutableEntityStorage): WorkspaceEntity.Builder<JvmBinaryJarsEntity> {
    val modifiable = JvmBinaryJarsEntityImpl.Builder(null)
    modifiable.diff = diff
    modifiable.snapshot = diff
    modifiable.id = createEntityId()
    return modifiable
  }

  @OptIn(EntityStorageInstrumentationApi::class)
  override fun createEntity(snapshot: EntityStorageInstrumentation): JvmBinaryJarsEntity {
    val entityId = createEntityId()
    return snapshot.initializeEntity(entityId) {
      val entity = JvmBinaryJarsEntityImpl(this)
      entity.snapshot = snapshot
      entity.id = entityId
      entity
    }
  }

  override fun getMetadata(): EntityMetadata {
    return MetadataStorageImpl.getMetadataByTypeFqn("org.jetbrains.workspacemodel.entities.JvmBinaryJarsEntity") as EntityMetadata
  }

  override fun clone(): JvmBinaryJarsEntityData {
    val clonedEntity = super.clone()
    clonedEntity as JvmBinaryJarsEntityData
    clonedEntity.jars = clonedEntity.jars.toMutableWorkspaceList()
    return clonedEntity
  }

  override fun getEntityInterface(): Class<out WorkspaceEntity> {
    return JvmBinaryJarsEntity::class.java
  }

  override fun serialize(ser: EntityInformation.Serializer) {
  }

  override fun deserialize(de: EntityInformation.Deserializer) {
  }

  override fun createDetachedEntity(parents: List<WorkspaceEntity>): WorkspaceEntity {
    return JvmBinaryJarsEntity(jars, entitySource) {
      parents.filterIsInstance<ModuleEntity>().singleOrNull()?.let { this.module = it }
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

    other as JvmBinaryJarsEntityData

    if (this.entitySource != other.entitySource) return false
    if (this.jars != other.jars) return false
    return true
  }

  override fun equalsIgnoringEntitySource(other: Any?): Boolean {
    if (other == null) return false
    if (this.javaClass != other.javaClass) return false

    other as JvmBinaryJarsEntityData

    if (this.jars != other.jars) return false
    return true
  }

  override fun hashCode(): Int {
    var result = entitySource.hashCode()
    result = 31 * result + jars.hashCode()
    return result
  }

  override fun hashCodeIgnoringEntitySource(): Int {
    var result = javaClass.hashCode()
    result = 31 * result + jars.hashCode()
    return result
  }
}
