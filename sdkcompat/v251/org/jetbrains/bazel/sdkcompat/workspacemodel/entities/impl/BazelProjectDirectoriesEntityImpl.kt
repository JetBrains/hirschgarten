package org.jetbrains.bazel.sdkcompat.workspacemodel.entities.impl

import com.intellij.platform.workspace.storage.ConnectionId
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.EntityType
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.GeneratedCodeImplVersion
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.WorkspaceEntityInternalApi
import com.intellij.platform.workspace.storage.impl.ModifiableWorkspaceEntityBase
import com.intellij.platform.workspace.storage.impl.WorkspaceEntityBase
import com.intellij.platform.workspace.storage.impl.WorkspaceEntityData
import com.intellij.platform.workspace.storage.impl.containers.MutableWorkspaceList
import com.intellij.platform.workspace.storage.impl.containers.toMutableWorkspaceList
import com.intellij.platform.workspace.storage.instrumentation.EntityStorageInstrumentation
import com.intellij.platform.workspace.storage.instrumentation.EntityStorageInstrumentationApi
import com.intellij.platform.workspace.storage.metadata.model.EntityMetadata
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.BazelProjectDirectoriesEntity

@GeneratedCodeApiVersion(3)
@GeneratedCodeImplVersion(6)
@OptIn(WorkspaceEntityInternalApi::class)
internal class BazelProjectDirectoriesEntityImpl(private val dataSource: BazelProjectDirectoriesEntityData) : BazelProjectDirectoriesEntity, WorkspaceEntityBase(
  dataSource) {

  private companion object {


    private val connections = listOf<ConnectionId>(
    )

  }

  override val projectRoot: VirtualFileUrl
    get() {
      readField("projectRoot")
      return dataSource.projectRoot
    }

  override val includedRoots: List<VirtualFileUrl>
    get() {
      readField("includedRoots")
      return dataSource.includedRoots
    }

  override val excludedRoots: List<VirtualFileUrl>
    get() {
      readField("excludedRoots")
      return dataSource.excludedRoots
    }

  override val buildFiles: List<VirtualFileUrl>
    get() {
      readField("buildFiles")
      return dataSource.buildFiles
    }

  override val indexAllFilesInIncludedRoots: Boolean
    get() {
      readField("indexAllFilesInIncludedRoots")
      return dataSource.indexAllFilesInIncludedRoots
    }
  override val projectViewFile: VirtualFileUrl?
    get() {
      readField("projectViewFile")
      return dataSource.projectViewFile
    }

  override val entitySource: EntitySource
    get() {
      readField("entitySource")
      return dataSource.entitySource
    }

  override fun connectionIdList(): List<ConnectionId> {
    return connections
  }


  internal class Builder(result: BazelProjectDirectoriesEntityData?) : ModifiableWorkspaceEntityBase<BazelProjectDirectoriesEntity, BazelProjectDirectoriesEntityData>(
    result), BazelProjectDirectoriesEntity.Builder {
    internal constructor() : this(BazelProjectDirectoriesEntityData())

    override fun applyToBuilder(builder: MutableEntityStorage) {
      if (this.diff != null) {
        if (existsInBuilder(builder)) {
          this.diff = builder
          return
        }
        else {
          error("Entity BazelProjectDirectoriesEntity is already created in a different builder")
        }
      }

      this.diff = builder
      addToBuilder()
      this.id = getEntityData().createEntityId()
      // After adding entity data to the builder, we need to unbind it and move the control over entity data to builder
      // Builder may switch to snapshot at any moment and lock entity data to modification
      this.currentEntityData = null

      index(this, "projectRoot", this.projectRoot)
      index(this, "includedRoots", this.includedRoots)
      index(this, "excludedRoots", this.excludedRoots)
      index(this, "buildFiles", this.buildFiles)
      index(this, "projectViewFile", this.projectViewFile)
      // Process linked entities that are connected without a builder
      processLinkedEntities(builder)
      checkInitialization() // TODO uncomment and check failed tests
    }

    private fun checkInitialization() {
      val _diff = diff
      if (!getEntityData().isEntitySourceInitialized()) {
        error("Field WorkspaceEntity#entitySource should be initialized")
      }
      if (!getEntityData().isProjectRootInitialized()) {
        error("Field BazelProjectDirectoriesEntity#projectRoot should be initialized")
      }
      if (!getEntityData().isIncludedRootsInitialized()) {
        error("Field BazelProjectDirectoriesEntity#includedRoots should be initialized")
      }
      if (!getEntityData().isExcludedRootsInitialized()) {
        error("Field BazelProjectDirectoriesEntity#excludedRoots should be initialized")
      }
      if (!getEntityData().isBuildFilesInitialized()) {
        error("Field BazelProjectDirectoriesEntity#buildFiles should be initialized")
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
      val collection_buildFiles = getEntityData().buildFiles
      if (collection_buildFiles is MutableWorkspaceList<*>) {
        collection_buildFiles.cleanModificationUpdateAction()
      }
    }

    // Relabeling code, move information from dataSource to this builder
    override fun relabel(dataSource: WorkspaceEntity, parents: Set<WorkspaceEntity>?) {
      dataSource as BazelProjectDirectoriesEntity
      if (this.entitySource != dataSource.entitySource) this.entitySource = dataSource.entitySource
      if (this.projectRoot != dataSource.projectRoot) this.projectRoot = dataSource.projectRoot
      if (this.includedRoots != dataSource.includedRoots) this.includedRoots = dataSource.includedRoots.toMutableList()
      if (this.excludedRoots != dataSource.excludedRoots) this.excludedRoots = dataSource.excludedRoots.toMutableList()
      if (this.buildFiles != dataSource.buildFiles) this.buildFiles = dataSource.buildFiles.toMutableList()
      if (this.indexAllFilesInIncludedRoots != dataSource.indexAllFilesInIncludedRoots) this.indexAllFilesInIncludedRoots = dataSource.indexAllFilesInIncludedRoots
      if (this.projectViewFile != dataSource?.projectViewFile) this.projectViewFile = dataSource.projectViewFile
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
        }
        else {
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
        }
        else {
          collection_excludedRoots.cleanModificationUpdateAction()
        }
        return collection_excludedRoots
      }
      set(value) {
        checkModificationAllowed()
        getEntityData(true).excludedRoots = value
        excludedRootsUpdater.invoke(value)
      }

    private val buildFilesUpdater: (value: List<VirtualFileUrl>) -> Unit = { value ->
      val _diff = diff
      if (_diff != null) index(this, "buildFiles", value)
      changedProperty.add("buildFiles")
    }
    override var buildFiles: MutableList<VirtualFileUrl>
      get() {
        val collection_buildFiles = getEntityData().buildFiles
        if (collection_buildFiles !is MutableWorkspaceList) return collection_buildFiles
        if (diff == null || modifiable.get()) {
          collection_buildFiles.setModificationUpdateAction(buildFilesUpdater)
        }
        else {
          collection_buildFiles.cleanModificationUpdateAction()
        }
        return collection_buildFiles
      }
      set(value) {
        checkModificationAllowed()
        getEntityData(true).buildFiles = value
        buildFilesUpdater.invoke(value)
      }

    override var indexAllFilesInIncludedRoots: Boolean
      get() = getEntityData().indexAllFilesInIncludedRoots
      set(value) {
        checkModificationAllowed()
        getEntityData(true).indexAllFilesInIncludedRoots = value
        changedProperty.add("indexAllFilesInIncludedRoots")
      }

    override var projectViewFile: VirtualFileUrl?
      get() = getEntityData().projectViewFile
      set(value) {
        checkModificationAllowed()
        getEntityData(true).projectViewFile = value
        changedProperty.add("projectViewFile")
        val _diff = diff
        if (_diff != null) index(this, "projectViewFile", value)
      }

    override fun getEntityClass(): Class<BazelProjectDirectoriesEntity> = BazelProjectDirectoriesEntity::class.java
  }
}

@OptIn(WorkspaceEntityInternalApi::class)
internal class BazelProjectDirectoriesEntityData : WorkspaceEntityData<BazelProjectDirectoriesEntity>() {
  lateinit var projectRoot: VirtualFileUrl
  lateinit var includedRoots: MutableList<VirtualFileUrl>
  lateinit var excludedRoots: MutableList<VirtualFileUrl>
  lateinit var buildFiles: MutableList<VirtualFileUrl>
  var indexAllFilesInIncludedRoots: Boolean = false
  var projectViewFile: VirtualFileUrl? = null

  internal fun isProjectRootInitialized(): Boolean = ::projectRoot.isInitialized
  internal fun isIncludedRootsInitialized(): Boolean = ::includedRoots.isInitialized
  internal fun isExcludedRootsInitialized(): Boolean = ::excludedRoots.isInitialized
  internal fun isBuildFilesInitialized(): Boolean = ::buildFiles.isInitialized


  override fun wrapAsModifiable(diff: MutableEntityStorage): WorkspaceEntity.Builder<BazelProjectDirectoriesEntity> {
    val modifiable = BazelProjectDirectoriesEntityImpl.Builder(null)
    modifiable.diff = diff
    modifiable.id = createEntityId()
    return modifiable
  }

  @OptIn(EntityStorageInstrumentationApi::class)
  override fun createEntity(snapshot: EntityStorageInstrumentation): BazelProjectDirectoriesEntity {
    val entityId = createEntityId()
    return snapshot.initializeEntity(entityId) {
      val entity = BazelProjectDirectoriesEntityImpl(this)
      entity.snapshot = snapshot
      entity.id = entityId
      entity
    }
  }

  override fun getMetadata(): EntityMetadata {
    return MetadataStorageImpl.getMetadataByTypeFqn(
      "org.jetbrains.bazel.sdkcompat.workspacemodel.entities.BazelProjectDirectoriesEntity") as EntityMetadata
  }

  override fun clone(): BazelProjectDirectoriesEntityData {
    val clonedEntity = super.clone()
    clonedEntity as BazelProjectDirectoriesEntityData
    clonedEntity.includedRoots = clonedEntity.includedRoots.toMutableWorkspaceList()
    clonedEntity.excludedRoots = clonedEntity.excludedRoots.toMutableWorkspaceList()
    clonedEntity.buildFiles = clonedEntity.buildFiles.toMutableWorkspaceList()
    return clonedEntity
  }

  override fun getEntityInterface(): Class<out WorkspaceEntity> {
    return BazelProjectDirectoriesEntity::class.java
  }

  override fun createDetachedEntity(parents: List<WorkspaceEntity.Builder<*>>): WorkspaceEntity.Builder<*> {
    return BazelProjectDirectoriesEntity(projectRoot, includedRoots, excludedRoots, buildFiles, indexAllFilesInIncludedRoots,
                                         entitySource) {
      this.projectViewFile = this@BazelProjectDirectoriesEntityData.projectViewFile
    }
  }

  override fun getRequiredParents(): List<Class<out WorkspaceEntity>> {
    val res = mutableListOf<Class<out WorkspaceEntity>>()
    return res
  }

  override fun equals(other: Any?): Boolean {
    if (other == null) return false
    if (this.javaClass != other.javaClass) return false

    other as BazelProjectDirectoriesEntityData

    if (this.entitySource != other.entitySource) return false
    if (this.projectRoot != other.projectRoot) return false
    if (this.includedRoots != other.includedRoots) return false
    if (this.excludedRoots != other.excludedRoots) return false
    if (this.buildFiles != other.buildFiles) return false
    if (this.indexAllFilesInIncludedRoots != other.indexAllFilesInIncludedRoots) return false
    if (this.projectViewFile != other.projectViewFile) return false
    return true
  }

  override fun equalsIgnoringEntitySource(other: Any?): Boolean {
    if (other == null) return false
    if (this.javaClass != other.javaClass) return false

    other as BazelProjectDirectoriesEntityData

    if (this.projectRoot != other.projectRoot) return false
    if (this.includedRoots != other.includedRoots) return false
    if (this.excludedRoots != other.excludedRoots) return false
    if (this.buildFiles != other.buildFiles) return false
    if (this.indexAllFilesInIncludedRoots != other.indexAllFilesInIncludedRoots) return false
    if (this.projectViewFile != other.projectViewFile) return false
    return true
  }

  override fun hashCode(): Int {
    var result = entitySource.hashCode()
    result = 31 * result + projectRoot.hashCode()
    result = 31 * result + includedRoots.hashCode()
    result = 31 * result + excludedRoots.hashCode()
    result = 31 * result + buildFiles.hashCode()
    result = 31 * result + indexAllFilesInIncludedRoots.hashCode()
    result = 31 * result + projectViewFile.hashCode()
    return result
  }

  override fun hashCodeIgnoringEntitySource(): Int {
    var result = javaClass.hashCode()
    result = 31 * result + projectRoot.hashCode()
    result = 31 * result + includedRoots.hashCode()
    result = 31 * result + excludedRoots.hashCode()
    result = 31 * result + buildFiles.hashCode()
    result = 31 * result + indexAllFilesInIncludedRoots.hashCode()
    result = 31 * result + projectViewFile.hashCode()
    return result
  }
}
