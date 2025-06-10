package org.jetbrains.bazel.workspacemodel.entities.impl

import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.ConnectionId
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.GeneratedCodeImplVersion
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.WorkspaceEntityInternalApi
import com.intellij.platform.workspace.storage.annotations.Child
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
import com.intellij.platform.workspace.storage.instrumentation.MutableEntityStorageInstrumentation
import com.intellij.platform.workspace.storage.metadata.model.EntityMetadata
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import org.jetbrains.bazel.workspacemodel.entities.ScalaAddendumEntity

@GeneratedCodeApiVersion(3)
@GeneratedCodeImplVersion(6)
@OptIn(WorkspaceEntityInternalApi::class)
internal class ScalaAddendumEntityImpl(private val dataSource: ScalaAddendumEntityData) : ScalaAddendumEntity, WorkspaceEntityBase(
  dataSource) {

  private companion object {
    internal val MODULE_CONNECTION_ID: ConnectionId = ConnectionId.create(ModuleEntity::class.java, ScalaAddendumEntity::class.java,
                                                                          ConnectionId.ConnectionType.ONE_TO_ONE, false)

    private val connections = listOf<ConnectionId>(
      MODULE_CONNECTION_ID,
    )

  }

  override val compilerVersion: String
    get() {
      readField("compilerVersion")
      return dataSource.compilerVersion
    }

  override val scalacOptions: List<String>
    get() {
      readField("scalacOptions")
      return dataSource.scalacOptions
    }

  override val sdkClasspaths: List<VirtualFileUrl>
    get() {
      readField("sdkClasspaths")
      return dataSource.sdkClasspaths
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


  internal class Builder(result: ScalaAddendumEntityData?) : ModifiableWorkspaceEntityBase<ScalaAddendumEntity, ScalaAddendumEntityData>(
    result), ScalaAddendumEntity.Builder {
    internal constructor() : this(ScalaAddendumEntityData())

    override fun applyToBuilder(builder: MutableEntityStorage) {
      if (this.diff != null) {
        if (existsInBuilder(builder)) {
          this.diff = builder
          return
        }
        else {
          error("Entity ScalaAddendumEntity is already created in a different builder")
        }
      }

      this.diff = builder
      addToBuilder()
      this.id = getEntityData().createEntityId()
      // After adding entity data to the builder, we need to unbind it and move the control over entity data to builder
      // Builder may switch to snapshot at any moment and lock entity data to modification
      this.currentEntityData = null

      index(this, "sdkClasspaths", this.sdkClasspaths)
      // Process linked entities that are connected without a builder
      processLinkedEntities(builder)
      checkInitialization() // TODO uncomment and check failed tests
    }

    private fun checkInitialization() {
      val _diff = diff
      if (!getEntityData().isEntitySourceInitialized()) {
        error("Field WorkspaceEntity#entitySource should be initialized")
      }
      if (!getEntityData().isCompilerVersionInitialized()) {
        error("Field ScalaAddendumEntity#compilerVersion should be initialized")
      }
      if (!getEntityData().isScalacOptionsInitialized()) {
        error("Field ScalaAddendumEntity#scalacOptions should be initialized")
      }
      if (!getEntityData().isSdkClasspathsInitialized()) {
        error("Field ScalaAddendumEntity#sdkClasspaths should be initialized")
      }
      if (_diff != null) {
        if (_diff.extractOneToOneParent<WorkspaceEntityBase>(MODULE_CONNECTION_ID, this) == null) {
          error("Field ScalaAddendumEntity#module should be initialized")
        }
      }
      else {
        if (this.entityLinks[EntityLink(false, MODULE_CONNECTION_ID)] == null) {
          error("Field ScalaAddendumEntity#module should be initialized")
        }
      }
    }

    override fun connectionIdList(): List<ConnectionId> {
      return connections
    }

    override fun afterModification() {
      val collection_scalacOptions = getEntityData().scalacOptions
      if (collection_scalacOptions is MutableWorkspaceList<*>) {
        collection_scalacOptions.cleanModificationUpdateAction()
      }
      val collection_sdkClasspaths = getEntityData().sdkClasspaths
      if (collection_sdkClasspaths is MutableWorkspaceList<*>) {
        collection_sdkClasspaths.cleanModificationUpdateAction()
      }
    }

    // Relabeling code, move information from dataSource to this builder
    override fun relabel(dataSource: WorkspaceEntity, parents: Set<WorkspaceEntity>?) {
      dataSource as ScalaAddendumEntity
      if (this.entitySource != dataSource.entitySource) this.entitySource = dataSource.entitySource
      if (this.compilerVersion != dataSource.compilerVersion) this.compilerVersion = dataSource.compilerVersion
      if (this.scalacOptions != dataSource.scalacOptions) this.scalacOptions = dataSource.scalacOptions.toMutableList()
      if (this.sdkClasspaths != dataSource.sdkClasspaths) this.sdkClasspaths = dataSource.sdkClasspaths.toMutableList()
      updateChildToParentReferences(parents)
    }


    override var entitySource: EntitySource
      get() = getEntityData().entitySource
      set(value) {
        checkModificationAllowed()
        getEntityData(true).entitySource = value
        changedProperty.add("entitySource")

      }

    override var compilerVersion: String
      get() = getEntityData().compilerVersion
      set(value) {
        checkModificationAllowed()
        getEntityData(true).compilerVersion = value
        changedProperty.add("compilerVersion")
      }

    private val scalacOptionsUpdater: (value: List<String>) -> Unit = { value ->

      changedProperty.add("scalacOptions")
    }
    override var scalacOptions: MutableList<String>
      get() {
        val collection_scalacOptions = getEntityData().scalacOptions
        if (collection_scalacOptions !is MutableWorkspaceList) return collection_scalacOptions
        if (diff == null || modifiable.get()) {
          collection_scalacOptions.setModificationUpdateAction(scalacOptionsUpdater)
        }
        else {
          collection_scalacOptions.cleanModificationUpdateAction()
        }
        return collection_scalacOptions
      }
      set(value) {
        checkModificationAllowed()
        getEntityData(true).scalacOptions = value
        scalacOptionsUpdater.invoke(value)
      }

    private val sdkClasspathsUpdater: (value: List<VirtualFileUrl>) -> Unit = { value ->
      val _diff = diff
      if (_diff != null) index(this, "sdkClasspaths", value)
      changedProperty.add("sdkClasspaths")
    }
    override var sdkClasspaths: MutableList<VirtualFileUrl>
      get() {
        val collection_sdkClasspaths = getEntityData().sdkClasspaths
        if (collection_sdkClasspaths !is MutableWorkspaceList) return collection_sdkClasspaths
        if (diff == null || modifiable.get()) {
          collection_sdkClasspaths.setModificationUpdateAction(sdkClasspathsUpdater)
        }
        else {
          collection_sdkClasspaths.cleanModificationUpdateAction()
        }
        return collection_sdkClasspaths
      }
      set(value) {
        checkModificationAllowed()
        getEntityData(true).sdkClasspaths = value
        sdkClasspathsUpdater.invoke(value)
      }

    override var module: ModuleEntity.Builder
      get() {
        val _diff = diff
        return if (_diff != null) {
          @OptIn(EntityStorageInstrumentationApi::class)
          ((_diff as MutableEntityStorageInstrumentation).getParentBuilder(MODULE_CONNECTION_ID, this) as? ModuleEntity.Builder)
          ?: (this.entityLinks[EntityLink(false, MODULE_CONNECTION_ID)]!! as ModuleEntity.Builder)
        }
        else {
          this.entityLinks[EntityLink(false, MODULE_CONNECTION_ID)]!! as ModuleEntity.Builder
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
          _diff.updateOneToOneParentOfChild(MODULE_CONNECTION_ID, this, value)
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

    override fun getEntityClass(): Class<ScalaAddendumEntity> = ScalaAddendumEntity::class.java
  }
}

@OptIn(WorkspaceEntityInternalApi::class)
internal class ScalaAddendumEntityData : WorkspaceEntityData<ScalaAddendumEntity>() {
  lateinit var compilerVersion: String
  lateinit var scalacOptions: MutableList<String>
  lateinit var sdkClasspaths: MutableList<VirtualFileUrl>

  internal fun isCompilerVersionInitialized(): Boolean = ::compilerVersion.isInitialized
  internal fun isScalacOptionsInitialized(): Boolean = ::scalacOptions.isInitialized
  internal fun isSdkClasspathsInitialized(): Boolean = ::sdkClasspaths.isInitialized

  override fun wrapAsModifiable(diff: MutableEntityStorage): WorkspaceEntity.Builder<ScalaAddendumEntity> {
    val modifiable = ScalaAddendumEntityImpl.Builder(null)
    modifiable.diff = diff
    modifiable.id = createEntityId()
    return modifiable
  }

  @OptIn(EntityStorageInstrumentationApi::class)
  override fun createEntity(snapshot: EntityStorageInstrumentation): ScalaAddendumEntity {
    val entityId = createEntityId()
    return snapshot.initializeEntity(entityId) {
      val entity = ScalaAddendumEntityImpl(this)
      entity.snapshot = snapshot
      entity.id = entityId
      entity
    }
  }

  override fun getMetadata(): EntityMetadata {
    return MetadataStorageImpl.getMetadataByTypeFqn("org.jetbrains.bazel.workspacemodel.entities.ScalaAddendumEntity") as EntityMetadata
  }

  override fun clone(): ScalaAddendumEntityData {
    val clonedEntity = super.clone()
    clonedEntity as ScalaAddendumEntityData
    clonedEntity.scalacOptions = clonedEntity.scalacOptions.toMutableWorkspaceList()
    clonedEntity.sdkClasspaths = clonedEntity.sdkClasspaths.toMutableWorkspaceList()
    return clonedEntity
  }

  override fun getEntityInterface(): Class<out WorkspaceEntity> {
    return ScalaAddendumEntity::class.java
  }

  override fun createDetachedEntity(parents: List<WorkspaceEntity.Builder<*>>): WorkspaceEntity.Builder<*> {
    return ScalaAddendumEntity(compilerVersion, scalacOptions, sdkClasspaths, entitySource) {
      parents.filterIsInstance<ModuleEntity.Builder>().singleOrNull()?.let { this.module = it }
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

    other as ScalaAddendumEntityData

    if (this.entitySource != other.entitySource) return false
    if (this.compilerVersion != other.compilerVersion) return false
    if (this.scalacOptions != other.scalacOptions) return false
    if (this.sdkClasspaths != other.sdkClasspaths) return false
    return true
  }

  override fun equalsIgnoringEntitySource(other: Any?): Boolean {
    if (other == null) return false
    if (this.javaClass != other.javaClass) return false

    other as ScalaAddendumEntityData

    if (this.compilerVersion != other.compilerVersion) return false
    if (this.scalacOptions != other.scalacOptions) return false
    if (this.sdkClasspaths != other.sdkClasspaths) return false
    return true
  }

  override fun hashCode(): Int {
    var result = entitySource.hashCode()
    result = 31 * result + compilerVersion.hashCode()
    result = 31 * result + scalacOptions.hashCode()
    result = 31 * result + sdkClasspaths.hashCode()
    return result
  }

  override fun hashCodeIgnoringEntitySource(): Int {
    var result = javaClass.hashCode()
    result = 31 * result + compilerVersion.hashCode()
    result = 31 * result + scalacOptions.hashCode()
    result = 31 * result + sdkClasspaths.hashCode()
    return result
  }
}
