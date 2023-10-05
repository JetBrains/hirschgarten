@file:SuppressWarnings("all")
package org.jetbrains.workspacemodel.entities

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.workspace.storage.EntityInformation
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.EntityStorage
import com.intellij.platform.workspace.storage.EntityType
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.GeneratedCodeImplVersion
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.impl.ConnectionId
import com.intellij.platform.workspace.storage.impl.ModifiableWorkspaceEntityBase
import com.intellij.platform.workspace.storage.impl.UsedClassesCollector
import com.intellij.platform.workspace.storage.impl.WorkspaceEntityBase
import com.intellij.platform.workspace.storage.impl.WorkspaceEntityData
import com.intellij.platform.workspace.storage.impl.containers.MutableWorkspaceList
import com.intellij.platform.workspace.storage.impl.containers.toMutableWorkspaceList
import com.intellij.platform.workspace.storage.url.VirtualFileUrl

@GeneratedCodeApiVersion(2)
@GeneratedCodeImplVersion(2)
public open class BspProjectDirectoriesEntityImpl(public val dataSource: BspProjectDirectoriesEntityData) :
  BspProjectDirectoriesEntity, WorkspaceEntityBase() {

  public companion object {


    public val connections: List<ConnectionId> = listOf<ConnectionId>(
    )

  }

  override val projectRoot: VirtualFileUrl
    get() = dataSource.projectRoot

  override val includedRoots: List<VirtualFileUrl>
    get() = dataSource.includedRoots

  override val excludedRoots: List<VirtualFileUrl>
    get() = dataSource.excludedRoots

  override val entitySource: EntitySource
    get() = dataSource.entitySource

  override fun connectionIdList(): List<ConnectionId> {
    return connections
  }

  public class Builder(result: BspProjectDirectoriesEntityData?) :
    ModifiableWorkspaceEntityBase<BspProjectDirectoriesEntity, BspProjectDirectoriesEntityData>(result),
    BspProjectDirectoriesEntity.Builder {
    public constructor() : this(BspProjectDirectoriesEntityData())

    override fun applyToBuilder(builder: MutableEntityStorage) {
      if (this.diff != null) {
        if (existsInBuilder(builder)) {
          this.diff = builder
          return
        } else {
          error("Entity BspProjectDirectoriesEntity is already created in a different builder")
        }
      }

      this.diff = builder
      this.snapshot = builder
      addToBuilder()
      this.id = getEntityData().createEntityId()
      // After adding entity data to the builder, we need to unbind it and move the control over entity data to builder
      // Builder may switch to snapshot at any moment and lock entity data to modification
      this.currentEntityData = null

      index(this, "projectRoot", this.projectRoot)
      index(this, "includedRoots", this.includedRoots)
      index(this, "excludedRoots", this.excludedRoots)
      // Process linked entities that are connected without a builder
      processLinkedEntities(builder)
      checkInitialization() // TODO uncomment and check failed tests
    }

    public fun checkInitialization() {
      val _diff = diff
      if (!getEntityData().isEntitySourceInitialized()) {
        error("Field WorkspaceEntity#entitySource should be initialized")
      }
      if (!getEntityData().isProjectRootInitialized()) {
        error("Field BspProjectDirectoriesEntity#projectRoot should be initialized")
      }
      if (!getEntityData().isIncludedRootsInitialized()) {
        error("Field BspProjectDirectoriesEntity#includedRoots should be initialized")
      }
      if (!getEntityData().isExcludedRootsInitialized()) {
        error("Field BspProjectDirectoriesEntity#excludedRoots should be initialized")
      }
    }

    override fun connectionIdList(): List<ConnectionId> {
      return connections
    }

    override fun afterModification() {
      val collection_includedRoots = getEntityData().includedRoots
      if (collection_includedRoots is MutableWorkspaceList<*>) {
        collection_includedRoots.cleanModificationUpdateAction()
      }
      val collection_excludedRoots = getEntityData().excludedRoots
      if (collection_excludedRoots is MutableWorkspaceList<*>) {
        collection_excludedRoots.cleanModificationUpdateAction()
      }
    }

    // Relabeling code, move information from dataSource to this builder
    override fun relabel(dataSource: WorkspaceEntity, parents: Set<WorkspaceEntity>?) {
      dataSource as BspProjectDirectoriesEntity
      if (this.entitySource != dataSource.entitySource) this.entitySource = dataSource.entitySource
      if (this.projectRoot != dataSource.projectRoot) this.projectRoot = dataSource.projectRoot
      if (this.includedRoots != dataSource.includedRoots) this.includedRoots = dataSource.includedRoots.toMutableList()
      if (this.excludedRoots != dataSource.excludedRoots) this.excludedRoots = dataSource.excludedRoots.toMutableList()
      updateChildToParentReferences(parents)
    }


    override var entitySource: EntitySource
      get() = getEntityData().entitySource
      set(value) {
        checkModificationAllowed()
        getEntityData(true).entitySource = value
        changedProperty.add("entitySource")

      }

    override var projectRoot: VirtualFileUrl
      get() = getEntityData().projectRoot
      set(value) {
        checkModificationAllowed()
        getEntityData(true).projectRoot = value
        changedProperty.add("projectRoot")
        val _diff = diff
        if (_diff != null) index(this, "projectRoot", value)
      }

    private val includedRootsUpdater: (value: List<VirtualFileUrl>) -> Unit = { value ->
      val _diff = diff
      if (_diff != null) index(this, "includedRoots", value)
      changedProperty.add("includedRoots")
    }
    override var includedRoots: MutableList<VirtualFileUrl>
      get() {
        val collection_includedRoots = getEntityData().includedRoots
        if (collection_includedRoots !is MutableWorkspaceList) return collection_includedRoots
        if (diff == null || modifiable.get()) {
          collection_includedRoots.setModificationUpdateAction(includedRootsUpdater)
        } else {
          collection_includedRoots.cleanModificationUpdateAction()
        }
        return collection_includedRoots
      }
      set(value) {
        checkModificationAllowed()
        getEntityData(true).includedRoots = value
        includedRootsUpdater.invoke(value)
      }

    private val excludedRootsUpdater: (value: List<VirtualFileUrl>) -> Unit = { value ->
      val _diff = diff
      if (_diff != null) index(this, "excludedRoots", value)
      changedProperty.add("excludedRoots")
    }
    override var excludedRoots: MutableList<VirtualFileUrl>
      get() {
        val collection_excludedRoots = getEntityData().excludedRoots
        if (collection_excludedRoots !is MutableWorkspaceList) return collection_excludedRoots
        if (diff == null || modifiable.get()) {
          collection_excludedRoots.setModificationUpdateAction(excludedRootsUpdater)
        } else {
          collection_excludedRoots.cleanModificationUpdateAction()
        }
        return collection_excludedRoots
      }
      set(value) {
        checkModificationAllowed()
        getEntityData(true).excludedRoots = value
        excludedRootsUpdater.invoke(value)
      }

    override fun getEntityClass(): Class<BspProjectDirectoriesEntity> = BspProjectDirectoriesEntity::class.java
  }
}

public class BspProjectDirectoriesEntityData : WorkspaceEntityData<BspProjectDirectoriesEntity>() {
  public lateinit var projectRoot: VirtualFileUrl
  public lateinit var includedRoots: MutableList<VirtualFileUrl>
  public lateinit var excludedRoots: MutableList<VirtualFileUrl>

  public fun isProjectRootInitialized(): Boolean = ::projectRoot.isInitialized
  public fun isIncludedRootsInitialized(): Boolean = ::includedRoots.isInitialized
  public fun isExcludedRootsInitialized(): Boolean = ::excludedRoots.isInitialized

  override fun wrapAsModifiable(diff: MutableEntityStorage): WorkspaceEntity.Builder<BspProjectDirectoriesEntity> {
    val modifiable = BspProjectDirectoriesEntityImpl.Builder(null)
    modifiable.diff = diff
    modifiable.snapshot = diff
    modifiable.id = createEntityId()
    return modifiable
  }

  override fun createEntity(snapshot: EntityStorage): BspProjectDirectoriesEntity {
    return getCached(snapshot) {
      val entity = BspProjectDirectoriesEntityImpl(this)
      entity.snapshot = snapshot
      entity.id = createEntityId()
      entity
    }
  }

  override fun clone(): BspProjectDirectoriesEntityData {
    val clonedEntity = super.clone()
    clonedEntity as BspProjectDirectoriesEntityData
    clonedEntity.includedRoots = clonedEntity.includedRoots.toMutableWorkspaceList()
    clonedEntity.excludedRoots = clonedEntity.excludedRoots.toMutableWorkspaceList()
    return clonedEntity
  }

  override fun getEntityInterface(): Class<out WorkspaceEntity> {
    return BspProjectDirectoriesEntity::class.java
  }

  override fun serialize(ser: EntityInformation.Serializer) {
  }

  override fun deserialize(de: EntityInformation.Deserializer) {
  }

  override fun createDetachedEntity(parents: List<WorkspaceEntity>): WorkspaceEntity {
    return BspProjectDirectoriesEntity(projectRoot, includedRoots, excludedRoots, entitySource) {
    }
  }

  override fun getRequiredParents(): List<Class<out WorkspaceEntity>> {
    val res = mutableListOf<Class<out WorkspaceEntity>>()
    return res
  }

  override fun equals(other: Any?): Boolean {
    if (other == null) return false
    if (this.javaClass != other.javaClass) return false

    other as BspProjectDirectoriesEntityData

    if (this.entitySource != other.entitySource) return false
    if (this.projectRoot != other.projectRoot) return false
    if (this.includedRoots != other.includedRoots) return false
    if (this.excludedRoots != other.excludedRoots) return false
    return true
  }

  override fun equalsIgnoringEntitySource(other: Any?): Boolean {
    if (other == null) return false
    if (this.javaClass != other.javaClass) return false

    other as BspProjectDirectoriesEntityData

    if (this.projectRoot != other.projectRoot) return false
    if (this.includedRoots != other.includedRoots) return false
    if (this.excludedRoots != other.excludedRoots) return false
    return true
  }

  override fun hashCode(): Int {
    var result = entitySource.hashCode()
    result = 31 * result + projectRoot.hashCode()
    result = 31 * result + includedRoots.hashCode()
    result = 31 * result + excludedRoots.hashCode()
    return result
  }

  override fun hashCodeIgnoringEntitySource(): Int {
    var result = javaClass.hashCode()
    result = 31 * result + projectRoot.hashCode()
    result = 31 * result + includedRoots.hashCode()
    result = 31 * result + excludedRoots.hashCode()
    return result
  }

  override fun collectClassUsagesData(collector: UsedClassesCollector) {
    this.includedRoots?.let { collector.add(it::class.java) }
    this.excludedRoots?.let { collector.add(it::class.java) }
    this.projectRoot?.let { collector.add(it::class.java) }
    collector.sameForAllEntities = false
  }
}
