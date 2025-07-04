package org.jetbrains.bazel.sdkcompat.workspacemodel.entities.impl

import com.intellij.platform.workspace.jps.entities.LibraryId
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
import com.intellij.platform.workspace.storage.impl.containers.MutableWorkspaceSet
import com.intellij.platform.workspace.storage.impl.containers.toMutableWorkspaceSet
import com.intellij.platform.workspace.storage.impl.indices.WorkspaceMutableIndex
import com.intellij.platform.workspace.storage.instrumentation.EntityStorageInstrumentation
import com.intellij.platform.workspace.storage.instrumentation.EntityStorageInstrumentationApi
import com.intellij.platform.workspace.storage.metadata.model.EntityMetadata
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeEntity
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeId

@GeneratedCodeApiVersion(3)
@GeneratedCodeImplVersion(7)
@OptIn(WorkspaceEntityInternalApi::class)
internal class CompiledSourceCodeInsideJarExcludeEntityImpl(private val dataSource: CompiledSourceCodeInsideJarExcludeEntityData) :
  CompiledSourceCodeInsideJarExcludeEntity, WorkspaceEntityBase(dataSource) {

  private companion object {


    private val connections = listOf<ConnectionId>(
    )

  }

  override val symbolicId: CompiledSourceCodeInsideJarExcludeId = super.symbolicId

  override val relativePathsInsideJarToExclude: Set<String>
    get() {
      readField("relativePathsInsideJarToExclude")
      return dataSource.relativePathsInsideJarToExclude
    }

  override val librariesFromInternalTargetsUrls: Set<String>
    get() {
      readField("librariesFromInternalTargetsUrls")
      return dataSource.librariesFromInternalTargetsUrls
    }

  override val excludeId: CompiledSourceCodeInsideJarExcludeId
    get() {
      readField("excludeId")
      return dataSource.excludeId
    }

  override val entitySource: EntitySource
    get() {
      readField("entitySource")
      return dataSource.entitySource
    }

  override fun connectionIdList(): List<ConnectionId> {
    return connections
  }


  internal class Builder(result: CompiledSourceCodeInsideJarExcludeEntityData?) :
    ModifiableWorkspaceEntityBase<CompiledSourceCodeInsideJarExcludeEntity, CompiledSourceCodeInsideJarExcludeEntityData>(result),
    CompiledSourceCodeInsideJarExcludeEntity.Builder {
    internal constructor() : this(CompiledSourceCodeInsideJarExcludeEntityData())

    override fun applyToBuilder(builder: MutableEntityStorage) {
      if (this.diff != null) {
        if (existsInBuilder(builder)) {
          this.diff = builder
          return
        }
        else {
          error("Entity CompiledSourceCodeInsideJarExcludeEntity is already created in a different builder")
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
      if (!getEntityData().isRelativePathsInsideJarToExcludeInitialized()) {
        error("Field CompiledSourceCodeInsideJarExcludeEntity#relativePathsInsideJarToExclude should be initialized")
      }
      if (!getEntityData().isLibrariesFromInternalTargetsUrlsInitialized()) {
        error("Field CompiledSourceCodeInsideJarExcludeEntity#librariesFromInternalTargetsUrls should be initialized")
      }
      if (!getEntityData().isExcludeIdInitialized()) {
        error("Field CompiledSourceCodeInsideJarExcludeEntity#excludeId should be initialized")
      }
    }

    override fun connectionIdList(): List<ConnectionId> {
      return connections
    }

    override fun afterModification() {
      val collection_relativePathsInsideJarToExclude = getEntityData().relativePathsInsideJarToExclude
      if (collection_relativePathsInsideJarToExclude is MutableWorkspaceSet<*>) {
        collection_relativePathsInsideJarToExclude.cleanModificationUpdateAction()
      }
      val collection_librariesFromInternalTargetsUrls = getEntityData().librariesFromInternalTargetsUrls
      if (collection_librariesFromInternalTargetsUrls is MutableWorkspaceSet<*>) {
        collection_librariesFromInternalTargetsUrls.cleanModificationUpdateAction()
      }
    }

    // Relabeling code, move information from dataSource to this builder
    override fun relabel(dataSource: WorkspaceEntity, parents: Set<WorkspaceEntity>?) {
      dataSource as CompiledSourceCodeInsideJarExcludeEntity
      if (this.entitySource != dataSource.entitySource) this.entitySource = dataSource.entitySource
      if (this.relativePathsInsideJarToExclude != dataSource.relativePathsInsideJarToExclude) this.relativePathsInsideJarToExclude =
        dataSource.relativePathsInsideJarToExclude.toMutableSet()
      if (this.librariesFromInternalTargetsUrls != dataSource.librariesFromInternalTargetsUrls) this.librariesFromInternalTargetsUrls =
        dataSource.librariesFromInternalTargetsUrls.toMutableSet()
      if (this.excludeId != dataSource.excludeId) this.excludeId = dataSource.excludeId
      updateChildToParentReferences(parents)
    }


    override var entitySource: EntitySource
      get() = getEntityData().entitySource
      set(value) {
        checkModificationAllowed()
        getEntityData(true).entitySource = value
        changedProperty.add("entitySource")

      }

    private val relativePathsInsideJarToExcludeUpdater: (value: Set<String>) -> Unit = { value ->

      changedProperty.add("relativePathsInsideJarToExclude")
    }
    override var relativePathsInsideJarToExclude: MutableSet<String>
      get() {
        val collection_relativePathsInsideJarToExclude = getEntityData().relativePathsInsideJarToExclude
        if (collection_relativePathsInsideJarToExclude !is MutableWorkspaceSet) return collection_relativePathsInsideJarToExclude
        if (diff == null || modifiable.get()) {
          collection_relativePathsInsideJarToExclude.setModificationUpdateAction(relativePathsInsideJarToExcludeUpdater)
        }
        else {
          collection_relativePathsInsideJarToExclude.cleanModificationUpdateAction()
        }
        return collection_relativePathsInsideJarToExclude
      }
      set(value) {
        checkModificationAllowed()
        getEntityData(true).relativePathsInsideJarToExclude = value
        relativePathsInsideJarToExcludeUpdater.invoke(value)
      }

    private val librariesFromInternalTargetsUrlsUpdater: (value: Set<String>) -> Unit = { value ->

      changedProperty.add("librariesFromInternalTargetsUrls")
    }
    override var librariesFromInternalTargetsUrls: MutableSet<String>
      get() {
        val collection_librariesFromInternalTargetsUrls = getEntityData().librariesFromInternalTargetsUrls
        if (collection_librariesFromInternalTargetsUrls !is MutableWorkspaceSet) return collection_librariesFromInternalTargetsUrls
        if (diff == null || modifiable.get()) {
          collection_librariesFromInternalTargetsUrls.setModificationUpdateAction(librariesFromInternalTargetsUrlsUpdater)
        }
        else {
          collection_librariesFromInternalTargetsUrls.cleanModificationUpdateAction()
        }
        return collection_librariesFromInternalTargetsUrls
      }
      set(value) {
        checkModificationAllowed()
        getEntityData(true).librariesFromInternalTargetsUrls = value
        librariesFromInternalTargetsUrlsUpdater.invoke(value)
      }

    override var excludeId: CompiledSourceCodeInsideJarExcludeId
      get() = getEntityData().excludeId
      set(value) {
        checkModificationAllowed()
        getEntityData(true).excludeId = value
        changedProperty.add("excludeId")

      }

    override fun getEntityClass(): Class<CompiledSourceCodeInsideJarExcludeEntity> = CompiledSourceCodeInsideJarExcludeEntity::class.java
  }
}

@OptIn(WorkspaceEntityInternalApi::class)
internal class CompiledSourceCodeInsideJarExcludeEntityData : WorkspaceEntityData<CompiledSourceCodeInsideJarExcludeEntity>(),
                                                              SoftLinkable {
  lateinit var relativePathsInsideJarToExclude: MutableSet<String>
  lateinit var librariesFromInternalTargetsUrls: MutableSet<String>
  lateinit var excludeId: CompiledSourceCodeInsideJarExcludeId

  internal fun isRelativePathsInsideJarToExcludeInitialized(): Boolean = ::relativePathsInsideJarToExclude.isInitialized
  internal fun isLibrariesFromInternalTargetsUrlsInitialized(): Boolean = ::librariesFromInternalTargetsUrls.isInitialized
  internal fun isExcludeIdInitialized(): Boolean = ::excludeId.isInitialized

  override fun getLinks(): Set<SymbolicEntityId<*>> {
    val result = HashSet<SymbolicEntityId<*>>()
    for (item in relativePathsInsideJarToExclude) {
    }
    for (item in librariesFromInternalTargetsUrls) {
    }
    result.add(excludeId)
    return result
  }

  override fun index(index: WorkspaceMutableIndex<SymbolicEntityId<*>>) {
    for (item in relativePathsInsideJarToExclude) {
    }
    for (item in librariesFromInternalTargetsUrls) {
    }
    index.index(this, excludeId)
  }

  override fun updateLinksIndex(prev: Set<SymbolicEntityId<*>>, index: WorkspaceMutableIndex<SymbolicEntityId<*>>) {
    // TODO verify logic
    val mutablePreviousSet = HashSet(prev)
    for (item in relativePathsInsideJarToExclude) {
    }
    for (item in librariesFromInternalTargetsUrls) {
    }
    val removedItem_excludeId = mutablePreviousSet.remove(excludeId)
    if (!removedItem_excludeId) {
      index.index(this, excludeId)
    }
    for (removed in mutablePreviousSet) {
      index.remove(this, removed)
    }
  }

  override fun updateLink(oldLink: SymbolicEntityId<*>, newLink: SymbolicEntityId<*>): Boolean {
    var changed = false
    val excludeId_data = if (excludeId == oldLink) {
      changed = true
      newLink as CompiledSourceCodeInsideJarExcludeId
    }
    else {
      null
    }
    if (excludeId_data != null) {
      excludeId = excludeId_data
    }
    return changed
  }

  override fun wrapAsModifiable(diff: MutableEntityStorage): WorkspaceEntity.Builder<CompiledSourceCodeInsideJarExcludeEntity> {
    val modifiable = CompiledSourceCodeInsideJarExcludeEntityImpl.Builder(null)
    modifiable.diff = diff
    modifiable.id = createEntityId()
    return modifiable
  }

  @OptIn(EntityStorageInstrumentationApi::class)
  override fun createEntity(snapshot: EntityStorageInstrumentation): CompiledSourceCodeInsideJarExcludeEntity {
    val entityId = createEntityId()
    return snapshot.initializeEntity(entityId) {
      val entity = CompiledSourceCodeInsideJarExcludeEntityImpl(this)
      entity.snapshot = snapshot
      entity.id = entityId
      entity
    }
  }

  override fun getMetadata(): EntityMetadata {
    return MetadataStorageImpl.getMetadataByTypeFqn(
      "org.jetbrains.bazel.sdkcompat.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeEntity"
    ) as EntityMetadata
  }

  override fun clone(): CompiledSourceCodeInsideJarExcludeEntityData {
    val clonedEntity = super.clone()
    clonedEntity as CompiledSourceCodeInsideJarExcludeEntityData
    clonedEntity.relativePathsInsideJarToExclude = clonedEntity.relativePathsInsideJarToExclude.toMutableWorkspaceSet()
    clonedEntity.librariesFromInternalTargetsUrls = clonedEntity.librariesFromInternalTargetsUrls.toMutableWorkspaceSet()
    return clonedEntity
  }

  override fun getEntityInterface(): Class<out WorkspaceEntity> {
    return CompiledSourceCodeInsideJarExcludeEntity::class.java
  }

  override fun createDetachedEntity(parents: List<WorkspaceEntity.Builder<*>>): WorkspaceEntity.Builder<*> {
    return CompiledSourceCodeInsideJarExcludeEntity(
      relativePathsInsideJarToExclude, librariesFromInternalTargetsUrls, excludeId, entitySource
    ) {
    }
  }

  override fun getRequiredParents(): List<Class<out WorkspaceEntity>> {
    val res = mutableListOf<Class<out WorkspaceEntity>>()
    return res
  }

  override fun equals(other: Any?): Boolean {
    if (other == null) return false
    if (this.javaClass != other.javaClass) return false

    other as CompiledSourceCodeInsideJarExcludeEntityData

    if (this.entitySource != other.entitySource) return false
    if (this.relativePathsInsideJarToExclude != other.relativePathsInsideJarToExclude) return false
    if (this.librariesFromInternalTargetsUrls != other.librariesFromInternalTargetsUrls) return false
    if (this.excludeId != other.excludeId) return false
    return true
  }

  override fun equalsIgnoringEntitySource(other: Any?): Boolean {
    if (other == null) return false
    if (this.javaClass != other.javaClass) return false

    other as CompiledSourceCodeInsideJarExcludeEntityData

    if (this.relativePathsInsideJarToExclude != other.relativePathsInsideJarToExclude) return false
    if (this.librariesFromInternalTargetsUrls != other.librariesFromInternalTargetsUrls) return false
    if (this.excludeId != other.excludeId) return false
    return true
  }

  override fun hashCode(): Int {
    var result = entitySource.hashCode()
    result = 31 * result + relativePathsInsideJarToExclude.hashCode()
    result = 31 * result + librariesFromInternalTargetsUrls.hashCode()
    result = 31 * result + excludeId.hashCode()
    return result
  }

  override fun hashCodeIgnoringEntitySource(): Int {
    var result = javaClass.hashCode()
    result = 31 * result + relativePathsInsideJarToExclude.hashCode()
    result = 31 * result + librariesFromInternalTargetsUrls.hashCode()
    result = 31 * result + excludeId.hashCode()
    return result
  }
}
