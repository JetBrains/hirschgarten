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
import com.intellij.platform.workspace.storage.impl.containers.toMutableWorkspaceSet
import com.intellij.platform.workspace.storage.impl.indices.WorkspaceMutableIndex
import com.intellij.platform.workspace.storage.instrumentation.EntityStorageInstrumentation
import com.intellij.platform.workspace.storage.instrumentation.EntityStorageInstrumentationApi
import com.intellij.platform.workspace.storage.metadata.model.EntityMetadata
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeId
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.LibraryCompiledSourceCodeInsideJarExcludeEntity

@GeneratedCodeApiVersion(3)
@GeneratedCodeImplVersion(7)
@OptIn(WorkspaceEntityInternalApi::class)
internal class LibraryCompiledSourceCodeInsideJarExcludeEntityImpl(private val dataSource: LibraryCompiledSourceCodeInsideJarExcludeEntityData) : LibraryCompiledSourceCodeInsideJarExcludeEntity, WorkspaceEntityBase(
  dataSource) {

  private companion object {


    private val connections = listOf<ConnectionId>(
    )

  }

  override val libraryId: LibraryId
    get() {
      readField("libraryId")
      return dataSource.libraryId
    }

  override val compiledSourceCodeInsideJarExcludeId: CompiledSourceCodeInsideJarExcludeId
    get() {
      readField("compiledSourceCodeInsideJarExcludeId")
      return dataSource.compiledSourceCodeInsideJarExcludeId
    }

  override val entitySource: EntitySource
    get() {
      readField("entitySource")
      return dataSource.entitySource
    }

  override fun connectionIdList(): List<ConnectionId> {
    return connections
  }


  internal class Builder(result: LibraryCompiledSourceCodeInsideJarExcludeEntityData?) : ModifiableWorkspaceEntityBase<LibraryCompiledSourceCodeInsideJarExcludeEntity, LibraryCompiledSourceCodeInsideJarExcludeEntityData>(
    result), LibraryCompiledSourceCodeInsideJarExcludeEntity.Builder {
    internal constructor() : this(LibraryCompiledSourceCodeInsideJarExcludeEntityData())

    override fun applyToBuilder(builder: MutableEntityStorage) {
      if (this.diff != null) {
        if (existsInBuilder(builder)) {
          this.diff = builder
          return
        }
        else {
          error("Entity LibraryCompiledSourceCodeInsideJarExcludeEntity is already created in a different builder")
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
      if (!getEntityData().isLibraryIdInitialized()) {
        error("Field LibraryCompiledSourceCodeInsideJarExcludeEntity#libraryId should be initialized")
      }
      if (!getEntityData().isCompiledSourceCodeInsideJarExcludeIdInitialized()) {
        error("Field LibraryCompiledSourceCodeInsideJarExcludeEntity#compiledSourceCodeInsideJarExcludeId should be initialized")
      }
    }

    override fun connectionIdList(): List<ConnectionId> {
      return connections
    }

    // Relabeling code, move information from dataSource to this builder
    override fun relabel(dataSource: WorkspaceEntity, parents: Set<WorkspaceEntity>?) {
      dataSource as LibraryCompiledSourceCodeInsideJarExcludeEntity
      if (this.entitySource != dataSource.entitySource) this.entitySource = dataSource.entitySource
      if (this.libraryId != dataSource.libraryId) this.libraryId = dataSource.libraryId
      if (this.compiledSourceCodeInsideJarExcludeId != dataSource.compiledSourceCodeInsideJarExcludeId) this.compiledSourceCodeInsideJarExcludeId = dataSource.compiledSourceCodeInsideJarExcludeId
      updateChildToParentReferences(parents)
    }


    override var entitySource: EntitySource
      get() = getEntityData().entitySource
      set(value) {
        checkModificationAllowed()
        getEntityData(true).entitySource = value
        changedProperty.add("entitySource")

      }

    override var libraryId: LibraryId
      get() = getEntityData().libraryId
      set(value) {
        checkModificationAllowed()
        getEntityData(true).libraryId = value
        changedProperty.add("libraryId")

      }

    override var compiledSourceCodeInsideJarExcludeId: CompiledSourceCodeInsideJarExcludeId
      get() = getEntityData().compiledSourceCodeInsideJarExcludeId
      set(value) {
        checkModificationAllowed()
        getEntityData(true).compiledSourceCodeInsideJarExcludeId = value
        changedProperty.add("compiledSourceCodeInsideJarExcludeId")

      }

    override fun getEntityClass(): Class<LibraryCompiledSourceCodeInsideJarExcludeEntity> =
      LibraryCompiledSourceCodeInsideJarExcludeEntity::class.java
  }
}

@OptIn(WorkspaceEntityInternalApi::class)
internal class LibraryCompiledSourceCodeInsideJarExcludeEntityData : WorkspaceEntityData<LibraryCompiledSourceCodeInsideJarExcludeEntity>(), SoftLinkable {
  lateinit var libraryId: LibraryId
  lateinit var compiledSourceCodeInsideJarExcludeId: CompiledSourceCodeInsideJarExcludeId

  internal fun isLibraryIdInitialized(): Boolean = ::libraryId.isInitialized
  internal fun isCompiledSourceCodeInsideJarExcludeIdInitialized(): Boolean = ::compiledSourceCodeInsideJarExcludeId.isInitialized

  override fun getLinks(): Set<SymbolicEntityId<*>> {
    val result = HashSet<SymbolicEntityId<*>>()
    result.add(libraryId)
    result.add(compiledSourceCodeInsideJarExcludeId)
    return result
  }

  override fun index(index: WorkspaceMutableIndex<SymbolicEntityId<*>>) {
    index.index(this, libraryId)
    index.index(this, compiledSourceCodeInsideJarExcludeId)
  }

  override fun updateLinksIndex(prev: Set<SymbolicEntityId<*>>, index: WorkspaceMutableIndex<SymbolicEntityId<*>>) {
    // TODO verify logic
    val mutablePreviousSet = HashSet(prev)
    val removedItem_libraryId = mutablePreviousSet.remove(libraryId)
    if (!removedItem_libraryId) {
      index.index(this, libraryId)
    }
    val removedItem_compiledSourceCodeInsideJarExcludeId = mutablePreviousSet.remove(compiledSourceCodeInsideJarExcludeId)
    if (!removedItem_compiledSourceCodeInsideJarExcludeId) {
      index.index(this, compiledSourceCodeInsideJarExcludeId)
    }
    for (removed in mutablePreviousSet) {
      index.remove(this, removed)
    }
  }

  override fun updateLink(oldLink: SymbolicEntityId<*>, newLink: SymbolicEntityId<*>): Boolean {
    var changed = false
    val libraryId_data = if (libraryId == oldLink) {
      changed = true
      newLink as LibraryId
    }
    else {
      null
    }
    if (libraryId_data != null) {
      libraryId = libraryId_data
    }
    val compiledSourceCodeInsideJarExcludeId_data = if (compiledSourceCodeInsideJarExcludeId == oldLink) {
      changed = true
      newLink as CompiledSourceCodeInsideJarExcludeId
    }
    else {
      null
    }
    if (compiledSourceCodeInsideJarExcludeId_data != null) {
      compiledSourceCodeInsideJarExcludeId = compiledSourceCodeInsideJarExcludeId_data
    }
    return changed
  }

  override fun wrapAsModifiable(diff: MutableEntityStorage): WorkspaceEntity.Builder<LibraryCompiledSourceCodeInsideJarExcludeEntity> {
    val modifiable = LibraryCompiledSourceCodeInsideJarExcludeEntityImpl.Builder(null)
    modifiable.diff = diff
    modifiable.id = createEntityId()
    return modifiable
  }

  @OptIn(EntityStorageInstrumentationApi::class)
  override fun createEntity(snapshot: EntityStorageInstrumentation): LibraryCompiledSourceCodeInsideJarExcludeEntity {
    val entityId = createEntityId()
    return snapshot.initializeEntity(entityId) {
      val entity = LibraryCompiledSourceCodeInsideJarExcludeEntityImpl(this)
      entity.snapshot = snapshot
      entity.id = entityId
      entity
    }
  }

  override fun getMetadata(): EntityMetadata {
    return MetadataStorageImpl.getMetadataByTypeFqn(
      "org.jetbrains.bazel.sdkcompat.workspacemodel.entities.LibraryCompiledSourceCodeInsideJarExcludeEntity") as EntityMetadata
  }

  override fun getEntityInterface(): Class<out WorkspaceEntity> {
    return LibraryCompiledSourceCodeInsideJarExcludeEntity::class.java
  }

  override fun createDetachedEntity(parents: List<WorkspaceEntity.Builder<*>>): WorkspaceEntity.Builder<*> {
    return LibraryCompiledSourceCodeInsideJarExcludeEntity(libraryId, compiledSourceCodeInsideJarExcludeId, entitySource) {
    }
  }

  override fun getRequiredParents(): List<Class<out WorkspaceEntity>> {
    val res = mutableListOf<Class<out WorkspaceEntity>>()
    return res
  }

  override fun equals(other: Any?): Boolean {
    if (other == null) return false
    if (this.javaClass != other.javaClass) return false

    other as LibraryCompiledSourceCodeInsideJarExcludeEntityData

    if (this.entitySource != other.entitySource) return false
    if (this.libraryId != other.libraryId) return false
    if (this.compiledSourceCodeInsideJarExcludeId != other.compiledSourceCodeInsideJarExcludeId) return false
    return true
  }

  override fun equalsIgnoringEntitySource(other: Any?): Boolean {
    if (other == null) return false
    if (this.javaClass != other.javaClass) return false

    other as LibraryCompiledSourceCodeInsideJarExcludeEntityData

    if (this.libraryId != other.libraryId) return false
    if (this.compiledSourceCodeInsideJarExcludeId != other.compiledSourceCodeInsideJarExcludeId) return false
    return true
  }

  override fun hashCode(): Int {
    var result = entitySource.hashCode()
    result = 31 * result + libraryId.hashCode()
    result = 31 * result + compiledSourceCodeInsideJarExcludeId.hashCode()
    return result
  }

  override fun hashCodeIgnoringEntitySource(): Int {
    var result = javaClass.hashCode()
    result = 31 * result + libraryId.hashCode()
    result = 31 * result + compiledSourceCodeInsideJarExcludeId.hashCode()
    return result
  }
}
