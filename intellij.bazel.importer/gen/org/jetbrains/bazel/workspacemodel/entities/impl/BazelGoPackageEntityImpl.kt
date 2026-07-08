@file:OptIn(EntityStorageInstrumentationApi::class)

package org.jetbrains.bazel.workspacemodel.entities.impl

import com.intellij.platform.workspace.storage.ConnectionId
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.GeneratedCodeImplVersion
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.WorkspaceEntityBuilder
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
import org.jetbrains.annotations.ApiStatus.Internal
import org.jetbrains.bazel.workspacemodel.entities.BazelGoPackageEntity
import org.jetbrains.bazel.workspacemodel.entities.BazelGoPackageEntityBuilder
import org.jetbrains.bazel.workspacemodel.entities.ImportPathId

@Internal
@GeneratedCodeApiVersion(3)
@GeneratedCodeImplVersion(7)
@OptIn(WorkspaceEntityInternalApi::class)
internal class BazelGoPackageEntityImpl(private val dataSource: BazelGoPackageEntityData) : BazelGoPackageEntity,
                                                                                            WorkspaceEntityBase(dataSource) {

  private companion object {

    private val connections = listOf<ConnectionId>()

  }

  override val symbolicId: ImportPathId = super.symbolicId

  override val importPath: String
    get() {
      readField("importPath")
      return dataSource.importPath
    }
  override val sources: List<VirtualFileUrl>
    get() {
      readField("sources")
      return dataSource.sources
    }

  override val entitySource: EntitySource
    get() {
      readField("entitySource")
      return dataSource.entitySource
    }

  override fun connectionIdList(): List<ConnectionId> {
    return connections
  }


  internal class Builder(result: BazelGoPackageEntityData?) :
    ModifiableWorkspaceEntityBase<BazelGoPackageEntity, BazelGoPackageEntityData>(result), BazelGoPackageEntityBuilder {
    internal constructor() : this(BazelGoPackageEntityData())

    override fun applyToBuilder(builder: MutableEntityStorage) {
      if (this.diff != null) {
        if (existsInBuilder(builder)) {
          this.diff = builder
          return
        }
        else {
          error("Entity BazelGoPackageEntity is already created in a different builder")
        }
      }
      this.diff = builder
      addToBuilder()
      this.id = getEntityData().createEntityId()
// After adding entity data to the builder, we need to unbind it and move the control over entity data to builder
// Builder may switch to snapshot at any moment and lock entity data to modification
      this.currentEntityData = null
      index(this, "sources", this.sources)
// Process linked entities that are connected without a builder
      processLinkedEntities(builder)
      checkInitialization() // TODO uncomment and check failed tests
    }

    private fun checkInitialization() {
      val _diff = diff
      if (!getEntityData().isEntitySourceInitialized()) {
        error("Field WorkspaceEntity#entitySource should be initialized")
      }
      if (!getEntityData().isImportPathInitialized()) {
        error("Field BazelGoPackageEntity#importPath should be initialized")
      }
      if (!getEntityData().isSourcesInitialized()) {
        error("Field BazelGoPackageEntity#sources should be initialized")
      }
    }

    override fun connectionIdList(): List<ConnectionId> {
      return connections
    }

    override fun afterModification() {
      val collection_sources = getEntityData().sources
      if (collection_sources is MutableWorkspaceList<*>) {
        collection_sources.cleanModificationUpdateAction()
      }
    }

    // Relabeling code, move information from dataSource to this builder
    override fun relabel(dataSource: WorkspaceEntity, parents: Set<WorkspaceEntity>?) {
      dataSource as BazelGoPackageEntity
      if (this.entitySource != dataSource.entitySource) this.entitySource = dataSource.entitySource
      if (this.importPath != dataSource.importPath) this.importPath = dataSource.importPath
      if (this.sources != dataSource.sources) this.sources = dataSource.sources.toMutableList()
      updateChildToParentReferences(parents)
    }


    override var entitySource: EntitySource
      get() = getEntityData().entitySource
      set(value) {
        checkModificationAllowed()
        getEntityData(true).entitySource = value
        changedProperty.add("entitySource")

      }
    override var importPath: String
      get() = getEntityData().importPath
      set(value) {
        checkModificationAllowed()
        getEntityData(true).importPath = value
        changedProperty.add("importPath")
      }
    private val sourcesUpdater: (value: List<VirtualFileUrl>) -> Unit = { value ->
      val _diff = diff
      if (_diff != null) index(this, "sources", value)
      changedProperty.add("sources")
    }
    override var sources: MutableList<VirtualFileUrl>
      get() {
        val collection_sources = getEntityData().sources
        if (collection_sources !is MutableWorkspaceList) return collection_sources
        if (diff == null || modifiable.get()) {
          collection_sources.setModificationUpdateAction(sourcesUpdater)
        }
        else {
          collection_sources.cleanModificationUpdateAction()
        }
        return collection_sources
      }
      set(value) {
        checkModificationAllowed()
        getEntityData(true).sources = value
        sourcesUpdater.invoke(value)
      }

    override fun getEntityClass(): Class<BazelGoPackageEntity> = BazelGoPackageEntity::class.java
  }

}

@OptIn(WorkspaceEntityInternalApi::class)
internal class BazelGoPackageEntityData : WorkspaceEntityData<BazelGoPackageEntity>() {
  lateinit var importPath: String
  lateinit var sources: MutableList<VirtualFileUrl>

  internal fun isImportPathInitialized(): Boolean = ::importPath.isInitialized
  internal fun isSourcesInitialized(): Boolean = ::sources.isInitialized

  override fun wrapAsModifiable(diff: MutableEntityStorage): WorkspaceEntityBuilder<BazelGoPackageEntity> {
    val modifiable = BazelGoPackageEntityImpl.Builder(null)
    modifiable.diff = diff
    modifiable.id = createEntityId()
    return modifiable
  }

  override fun createEntity(snapshot: EntityStorageInstrumentation): BazelGoPackageEntity {
    val entityId = createEntityId()
    return snapshot.initializeEntity(entityId) {
      val entity = BazelGoPackageEntityImpl(this)
      entity.snapshot = snapshot
      entity.id = entityId
      entity
    }
  }

  override fun getMetadata(): EntityMetadata {
    return MetadataStorageImpl.getMetadataByTypeFqn("org.jetbrains.bazel.workspacemodel.entities.BazelGoPackageEntity") as EntityMetadata
  }

  override fun clone(): BazelGoPackageEntityData {
    val clonedEntity = super.clone()
    clonedEntity as BazelGoPackageEntityData
    clonedEntity.sources = clonedEntity.sources.toMutableWorkspaceList()
    return clonedEntity
  }

  override fun getEntityInterface(): Class<out WorkspaceEntity> {
    return BazelGoPackageEntity::class.java
  }

  override fun createDetachedEntity(parents: List<WorkspaceEntityBuilder<*>>): WorkspaceEntityBuilder<*> {
    return BazelGoPackageEntity(importPath, sources, entitySource)
  }

  override fun getRequiredParents(): List<Class<out WorkspaceEntity>> {
    val res = mutableListOf<Class<out WorkspaceEntity>>()
    return res
  }

  override fun equals(other: Any?): Boolean {
    if (other == null) return false
    if (this.javaClass != other.javaClass) return false
    other as BazelGoPackageEntityData
    if (this.entitySource != other.entitySource) return false
    if (this.importPath != other.importPath) return false
    if (this.sources != other.sources) return false
    return true
  }

  override fun equalsIgnoringEntitySource(other: Any?): Boolean {
    if (other == null) return false
    if (this.javaClass != other.javaClass) return false
    other as BazelGoPackageEntityData
    if (this.importPath != other.importPath) return false
    if (this.sources != other.sources) return false
    return true
  }

  override fun hashCode(): Int {
    var result = entitySource.hashCode()
    result = 31 * result + importPath.hashCode()
    result = 31 * result + sources.hashCode()
    return result
  }

  override fun hashCodeIgnoringEntitySource(): Int {
    var result = javaClass.hashCode()
    result = 31 * result + importPath.hashCode()
    result = 31 * result + sources.hashCode()
    return result
  }
}
