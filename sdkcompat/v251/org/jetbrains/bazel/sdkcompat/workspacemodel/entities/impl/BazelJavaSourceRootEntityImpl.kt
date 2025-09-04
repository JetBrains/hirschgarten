package org.jetbrains.bazel.sdkcompat.workspacemodel.entities.impl

import com.intellij.platform.workspace.storage.ConnectionId
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.EntityType
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.GeneratedCodeImplVersion
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.SymbolicEntityId
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.WorkspaceEntityInternalApi
import com.intellij.platform.workspace.storage.WorkspaceEntityWithSymbolicId
import com.intellij.platform.workspace.storage.impl.ModifiableWorkspaceEntityBase
import com.intellij.platform.workspace.storage.impl.SoftLinkable
import com.intellij.platform.workspace.storage.impl.WorkspaceEntityBase
import com.intellij.platform.workspace.storage.impl.WorkspaceEntityData
import com.intellij.platform.workspace.storage.impl.containers.MutableWorkspaceList
import com.intellij.platform.workspace.storage.impl.containers.toMutableWorkspaceList
import com.intellij.platform.workspace.storage.impl.indices.WorkspaceMutableIndex
import com.intellij.platform.workspace.storage.instrumentation.EntityStorageInstrumentation
import com.intellij.platform.workspace.storage.instrumentation.EntityStorageInstrumentationApi
import com.intellij.platform.workspace.storage.metadata.model.EntityMetadata
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.BazelJavaSourceRootEntity
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.PackageNameId

@GeneratedCodeApiVersion(3)
@GeneratedCodeImplVersion(6)
@OptIn(WorkspaceEntityInternalApi::class)
internal class BazelJavaSourceRootEntityImpl(private val dataSource: BazelJavaSourceRootEntityData) : BazelJavaSourceRootEntity, WorkspaceEntityBase(
  dataSource) {

  private companion object {


    private val connections = listOf<ConnectionId>(
    )

  }

  override val packageNameId: PackageNameId
    get() {
      readField("packageNameId")
      return dataSource.packageNameId
    }

  override val sourceRoots: List<VirtualFileUrl>
    get() {
      readField("sourceRoots")
      return dataSource.sourceRoots
    }

  override val entitySource: EntitySource
    get() {
      readField("entitySource")
      return dataSource.entitySource
    }

  override fun connectionIdList(): List<ConnectionId> {
    return connections
  }


  internal class Builder(result: BazelJavaSourceRootEntityData?) : ModifiableWorkspaceEntityBase<BazelJavaSourceRootEntity, BazelJavaSourceRootEntityData>(
    result), BazelJavaSourceRootEntity.Builder {
    internal constructor() : this(BazelJavaSourceRootEntityData())

    override fun applyToBuilder(builder: MutableEntityStorage) {
      if (this.diff != null) {
        if (existsInBuilder(builder)) {
          this.diff = builder
          return
        }
        else {
          error("Entity BazelJavaSourceRootEntity is already created in a different builder")
        }
      }

      this.diff = builder
      addToBuilder()
      this.id = getEntityData().createEntityId()
      // After adding entity data to the builder, we need to unbind it and move the control over entity data to builder
      // Builder may switch to snapshot at any moment and lock entity data to modification
      this.currentEntityData = null

      index(this, "sourceRoots", this.sourceRoots)
      // Process linked entities that are connected without a builder
      processLinkedEntities(builder)
      checkInitialization() // TODO uncomment and check failed tests
    }

    private fun checkInitialization() {
      val _diff = diff
      if (!getEntityData().isEntitySourceInitialized()) {
        error("Field WorkspaceEntity#entitySource should be initialized")
      }
      if (!getEntityData().isPackageNameIdInitialized()) {
        error("Field BazelJavaSourceRootEntity#packageNameId should be initialized")
      }
      if (!getEntityData().isSourceRootsInitialized()) {
        error("Field BazelJavaSourceRootEntity#sourceRoots should be initialized")
      }
    }

    override fun connectionIdList(): List<ConnectionId> {
      return connections
    }

    override fun afterModification() {
      val collection_sourceRoots = getEntityData().sourceRoots
      if (collection_sourceRoots is MutableWorkspaceList<*>) {
        collection_sourceRoots.cleanModificationUpdateAction()
      }
    }

    // Relabeling code, move information from dataSource to this builder
    override fun relabel(dataSource: WorkspaceEntity, parents: Set<WorkspaceEntity>?) {
      dataSource as BazelJavaSourceRootEntity
      if (this.entitySource != dataSource.entitySource) this.entitySource = dataSource.entitySource
      if (this.packageNameId != dataSource.packageNameId) this.packageNameId = dataSource.packageNameId
      if (this.sourceRoots != dataSource.sourceRoots) this.sourceRoots = dataSource.sourceRoots.toMutableList()
      updateChildToParentReferences(parents)
    }


    override var entitySource: EntitySource
      get() = getEntityData().entitySource
      set(value) {
        checkModificationAllowed()
        getEntityData(true).entitySource = value
        changedProperty.add("entitySource")

      }

    override var packageNameId: PackageNameId
      get() = getEntityData().packageNameId
      set(value) {
        checkModificationAllowed()
        getEntityData(true).packageNameId = value
        changedProperty.add("packageNameId")

      }

    private val sourceRootsUpdater: (value: List<VirtualFileUrl>) -> Unit = { value ->
      val _diff = diff
      if (_diff != null) index(this, "sourceRoots", value)
      changedProperty.add("sourceRoots")
    }
    override var sourceRoots: MutableList<VirtualFileUrl>
      get() {
        val collection_sourceRoots = getEntityData().sourceRoots
        if (collection_sourceRoots !is MutableWorkspaceList) return collection_sourceRoots
        if (diff == null || modifiable.get()) {
          collection_sourceRoots.setModificationUpdateAction(sourceRootsUpdater)
        }
        else {
          collection_sourceRoots.cleanModificationUpdateAction()
        }
        return collection_sourceRoots
      }
      set(value) {
        checkModificationAllowed()
        getEntityData(true).sourceRoots = value
        sourceRootsUpdater.invoke(value)
      }

    override fun getEntityClass(): Class<BazelJavaSourceRootEntity> = BazelJavaSourceRootEntity::class.java
  }
}

@OptIn(WorkspaceEntityInternalApi::class)
internal class BazelJavaSourceRootEntityData : WorkspaceEntityData<BazelJavaSourceRootEntity>(), SoftLinkable {
  lateinit var packageNameId: PackageNameId
  lateinit var sourceRoots: MutableList<VirtualFileUrl>

  internal fun isPackageNameIdInitialized(): Boolean = ::packageNameId.isInitialized
  internal fun isSourceRootsInitialized(): Boolean = ::sourceRoots.isInitialized

  override fun getLinks(): Set<SymbolicEntityId<*>> {
    val result = HashSet<SymbolicEntityId<*>>()
    result.add(packageNameId)
    for (item in sourceRoots) {
    }
    return result
  }

  override fun index(index: WorkspaceMutableIndex<SymbolicEntityId<*>>) {
    index.index(this, packageNameId)
    for (item in sourceRoots) {
    }
  }

  override fun updateLinksIndex(prev: Set<SymbolicEntityId<*>>, index: WorkspaceMutableIndex<SymbolicEntityId<*>>) {
    // TODO verify logic
    val mutablePreviousSet = HashSet(prev)
    val removedItem_packageNameId = mutablePreviousSet.remove(packageNameId)
    if (!removedItem_packageNameId) {
      index.index(this, packageNameId)
    }
    for (item in sourceRoots) {
    }
    for (removed in mutablePreviousSet) {
      index.remove(this, removed)
    }
  }

  override fun updateLink(oldLink: SymbolicEntityId<*>, newLink: SymbolicEntityId<*>): Boolean {
    var changed = false
    val packageNameId_data = if (packageNameId == oldLink) {
      changed = true
      newLink as PackageNameId
    }
    else {
      null
    }
    if (packageNameId_data != null) {
      packageNameId = packageNameId_data
    }
    return changed
  }

  override fun wrapAsModifiable(diff: MutableEntityStorage): WorkspaceEntity.Builder<BazelJavaSourceRootEntity> {
    val modifiable = BazelJavaSourceRootEntityImpl.Builder(null)
    modifiable.diff = diff
    modifiable.id = createEntityId()
    return modifiable
  }

  @OptIn(EntityStorageInstrumentationApi::class)
  override fun createEntity(snapshot: EntityStorageInstrumentation): BazelJavaSourceRootEntity {
    val entityId = createEntityId()
    return snapshot.initializeEntity(entityId) {
      val entity = BazelJavaSourceRootEntityImpl(this)
      entity.snapshot = snapshot
      entity.id = entityId
      entity
    }
  }

  override fun getMetadata(): EntityMetadata {
    return MetadataStorageImpl.getMetadataByTypeFqn(
      "org.jetbrains.bazel.sdkcompat.workspacemodel.entities.BazelJavaSourceRootEntity") as EntityMetadata
  }

  override fun clone(): BazelJavaSourceRootEntityData {
    val clonedEntity = super.clone()
    clonedEntity as BazelJavaSourceRootEntityData
    clonedEntity.sourceRoots = clonedEntity.sourceRoots.toMutableWorkspaceList()
    return clonedEntity
  }

  override fun getEntityInterface(): Class<out WorkspaceEntity> {
    return BazelJavaSourceRootEntity::class.java
  }

  override fun createDetachedEntity(parents: List<WorkspaceEntity.Builder<*>>): WorkspaceEntity.Builder<*> {
    return BazelJavaSourceRootEntity(packageNameId, sourceRoots, entitySource) {
    }
  }

  override fun getRequiredParents(): List<Class<out WorkspaceEntity>> {
    val res = mutableListOf<Class<out WorkspaceEntity>>()
    return res
  }

  override fun equals(other: Any?): Boolean {
    if (other == null) return false
    if (this.javaClass != other.javaClass) return false

    other as BazelJavaSourceRootEntityData

    if (this.entitySource != other.entitySource) return false
    if (this.packageNameId != other.packageNameId) return false
    if (this.sourceRoots != other.sourceRoots) return false
    return true
  }

  override fun equalsIgnoringEntitySource(other: Any?): Boolean {
    if (other == null) return false
    if (this.javaClass != other.javaClass) return false

    other as BazelJavaSourceRootEntityData

    if (this.packageNameId != other.packageNameId) return false
    if (this.sourceRoots != other.sourceRoots) return false
    return true
  }

  override fun hashCode(): Int {
    var result = entitySource.hashCode()
    result = 31 * result + packageNameId.hashCode()
    result = 31 * result + sourceRoots.hashCode()
    return result
  }

  override fun hashCodeIgnoringEntitySource(): Int {
    var result = javaClass.hashCode()
    result = 31 * result + packageNameId.hashCode()
    result = 31 * result + sourceRoots.hashCode()
    return result
  }
}
