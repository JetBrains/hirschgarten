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
public open class AndroidAddendumEntityImpl(private val dataSource: AndroidAddendumEntityData) : AndroidAddendumEntity,
  WorkspaceEntityBase(dataSource) {

  private companion object {
    internal val MODULE_CONNECTION_ID: ConnectionId = ConnectionId.create(
      ModuleEntity::class.java,
      AndroidAddendumEntity::class.java,
      ConnectionId.ConnectionType.ONE_TO_ONE,
      false
    )

    private val connections = listOf<ConnectionId>(
      MODULE_CONNECTION_ID,
    )

  }

  override val androidSdkName: String
    get() {
      readField("androidSdkName")
      return dataSource.androidSdkName
    }

  override val androidTargetType: AndroidTargetType
    get() {
      readField("androidTargetType")
      return dataSource.androidTargetType
    }

  override val manifest: VirtualFileUrl?
    get() {
      readField("manifest")
      return dataSource.manifest
    }

  override val resourceFolders: List<VirtualFileUrl>
    get() {
      readField("resourceFolders")
      return dataSource.resourceFolders
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


  public class Builder(result: AndroidAddendumEntityData?) :
    ModifiableWorkspaceEntityBase<AndroidAddendumEntity, AndroidAddendumEntityData>(result),
    AndroidAddendumEntity.Builder {
    public constructor() : this(AndroidAddendumEntityData())

    override fun applyToBuilder(builder: MutableEntityStorage) {
      if (this.diff != null) {
        if (existsInBuilder(builder)) {
          this.diff = builder
          return
        } else {
          error("Entity AndroidAddendumEntity is already created in a different builder")
        }
      }

      this.diff = builder
      this.snapshot = builder
      addToBuilder()
      this.id = getEntityData().createEntityId()
      // After adding entity data to the builder, we need to unbind it and move the control over entity data to builder
      // Builder may switch to snapshot at any moment and lock entity data to modification
      this.currentEntityData = null

      index(this, "manifest", this.manifest)
      index(this, "resourceFolders", this.resourceFolders)
      // Process linked entities that are connected without a builder
      processLinkedEntities(builder)
      checkInitialization() // TODO uncomment and check failed tests
    }

    private fun checkInitialization() {
      val _diff = diff
      if (!getEntityData().isEntitySourceInitialized()) {
        error("Field WorkspaceEntity#entitySource should be initialized")
      }
      if (!getEntityData().isAndroidSdkNameInitialized()) {
        error("Field AndroidAddendumEntity#androidSdkName should be initialized")
      }
      if (!getEntityData().isAndroidTargetTypeInitialized()) {
        error("Field AndroidAddendumEntity#androidTargetType should be initialized")
      }
      if (!getEntityData().isResourceFoldersInitialized()) {
        error("Field AndroidAddendumEntity#resourceFolders should be initialized")
      }
      if (_diff != null) {
        if (_diff.extractOneToOneParent<WorkspaceEntityBase>(MODULE_CONNECTION_ID, this) == null) {
          error("Field AndroidAddendumEntity#module should be initialized")
        }
      } else {
        if (this.entityLinks[EntityLink(false, MODULE_CONNECTION_ID)] == null) {
          error("Field AndroidAddendumEntity#module should be initialized")
        }
      }
    }

    override fun connectionIdList(): List<ConnectionId> {
      return connections
    }

    override fun afterModification() {
      val collection_resourceFolders = getEntityData().resourceFolders
      if (collection_resourceFolders is MutableWorkspaceList<*>) {
        collection_resourceFolders.cleanModificationUpdateAction()
      }
    }

    // Relabeling code, move information from dataSource to this builder
    override fun relabel(dataSource: WorkspaceEntity, parents: Set<WorkspaceEntity>?) {
      dataSource as AndroidAddendumEntity
      if (this.entitySource != dataSource.entitySource) this.entitySource = dataSource.entitySource
      if (this.androidSdkName != dataSource.androidSdkName) this.androidSdkName = dataSource.androidSdkName
      if (this.androidTargetType != dataSource.androidTargetType) this.androidTargetType = dataSource.androidTargetType
      if (this.manifest != dataSource?.manifest) this.manifest = dataSource.manifest
      if (this.resourceFolders != dataSource.resourceFolders) this.resourceFolders =
        dataSource.resourceFolders.toMutableList()
      updateChildToParentReferences(parents)
    }


    override var entitySource: EntitySource
      get() = getEntityData().entitySource
      set(value) {
        checkModificationAllowed()
        getEntityData(true).entitySource = value
        changedProperty.add("entitySource")

      }

    override var androidSdkName: String
      get() = getEntityData().androidSdkName
      set(value) {
        checkModificationAllowed()
        getEntityData(true).androidSdkName = value
        changedProperty.add("androidSdkName")
      }

    override var androidTargetType: AndroidTargetType
      get() = getEntityData().androidTargetType
      set(value) {
        checkModificationAllowed()
        getEntityData(true).androidTargetType = value
        changedProperty.add("androidTargetType")

      }

    override var manifest: VirtualFileUrl?
      get() = getEntityData().manifest
      set(value) {
        checkModificationAllowed()
        getEntityData(true).manifest = value
        changedProperty.add("manifest")
        val _diff = diff
        if (_diff != null) index(this, "manifest", value)
      }

    private val resourceFoldersUpdater: (value: List<VirtualFileUrl>) -> Unit = { value ->
      val _diff = diff
      if (_diff != null) index(this, "resourceFolders", value)
      changedProperty.add("resourceFolders")
    }
    override var resourceFolders: MutableList<VirtualFileUrl>
      get() {
        val collection_resourceFolders = getEntityData().resourceFolders
        if (collection_resourceFolders !is MutableWorkspaceList) return collection_resourceFolders
        if (diff == null || modifiable.get()) {
          collection_resourceFolders.setModificationUpdateAction(resourceFoldersUpdater)
        } else {
          collection_resourceFolders.cleanModificationUpdateAction()
        }
        return collection_resourceFolders
      }
      set(value) {
        checkModificationAllowed()
        getEntityData(true).resourceFolders = value
        resourceFoldersUpdater.invoke(value)
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

    override fun getEntityClass(): Class<AndroidAddendumEntity> = AndroidAddendumEntity::class.java
  }
}

public class AndroidAddendumEntityData : WorkspaceEntityData<AndroidAddendumEntity>() {
  public lateinit var androidSdkName: String
  public lateinit var androidTargetType: AndroidTargetType
  public var manifest: VirtualFileUrl? = null
  public lateinit var resourceFolders: MutableList<VirtualFileUrl>

  internal fun isAndroidSdkNameInitialized(): Boolean = ::androidSdkName.isInitialized
  internal fun isAndroidTargetTypeInitialized(): Boolean = ::androidTargetType.isInitialized
  internal fun isResourceFoldersInitialized(): Boolean = ::resourceFolders.isInitialized

  override fun wrapAsModifiable(diff: MutableEntityStorage): WorkspaceEntity.Builder<AndroidAddendumEntity> {
    val modifiable = AndroidAddendumEntityImpl.Builder(null)
    modifiable.diff = diff
    modifiable.snapshot = diff
    modifiable.id = createEntityId()
    return modifiable
  }

  @OptIn(EntityStorageInstrumentationApi::class)
  override fun createEntity(snapshot: EntityStorageInstrumentation): AndroidAddendumEntity {
    val entityId = createEntityId()
    return snapshot.initializeEntity(entityId) {
      val entity = AndroidAddendumEntityImpl(this)
      entity.snapshot = snapshot
      entity.id = entityId
      entity
    }
  }

  override fun getMetadata(): EntityMetadata {
    return MetadataStorageImpl.getMetadataByTypeFqn("org.jetbrains.workspacemodel.entities.AndroidAddendumEntity") as EntityMetadata
  }

  override fun clone(): AndroidAddendumEntityData {
    val clonedEntity = super.clone()
    clonedEntity as AndroidAddendumEntityData
    clonedEntity.resourceFolders = clonedEntity.resourceFolders.toMutableWorkspaceList()
    return clonedEntity
  }

  override fun getEntityInterface(): Class<out WorkspaceEntity> {
    return AndroidAddendumEntity::class.java
  }

  override fun serialize(ser: EntityInformation.Serializer) {
  }

  override fun deserialize(de: EntityInformation.Deserializer) {
  }

  override fun createDetachedEntity(parents: List<WorkspaceEntity>): WorkspaceEntity {
    return AndroidAddendumEntity(androidSdkName, androidTargetType, resourceFolders, entitySource) {
      this.manifest = this@AndroidAddendumEntityData.manifest
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

    other as AndroidAddendumEntityData

    if (this.entitySource != other.entitySource) return false
    if (this.androidSdkName != other.androidSdkName) return false
    if (this.androidTargetType != other.androidTargetType) return false
    if (this.manifest != other.manifest) return false
    if (this.resourceFolders != other.resourceFolders) return false
    return true
  }

  override fun equalsIgnoringEntitySource(other: Any?): Boolean {
    if (other == null) return false
    if (this.javaClass != other.javaClass) return false

    other as AndroidAddendumEntityData

    if (this.androidSdkName != other.androidSdkName) return false
    if (this.androidTargetType != other.androidTargetType) return false
    if (this.manifest != other.manifest) return false
    if (this.resourceFolders != other.resourceFolders) return false
    return true
  }

  override fun hashCode(): Int {
    var result = entitySource.hashCode()
    result = 31 * result + androidSdkName.hashCode()
    result = 31 * result + androidTargetType.hashCode()
    result = 31 * result + manifest.hashCode()
    result = 31 * result + resourceFolders.hashCode()
    return result
  }

  override fun hashCodeIgnoringEntitySource(): Int {
    var result = javaClass.hashCode()
    result = 31 * result + androidSdkName.hashCode()
    result = 31 * result + androidTargetType.hashCode()
    result = 31 * result + manifest.hashCode()
    result = 31 * result + resourceFolders.hashCode()
    return result
  }
}
