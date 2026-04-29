@file:OptIn(EntityStorageInstrumentationApi::class)

package org.jetbrains.bazel.workspacemodel.entities.impl

import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.LibraryEntityBuilder
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.ConnectionId
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.GeneratedCodeImplVersion
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.WorkspaceEntityBuilder
import com.intellij.platform.workspace.storage.WorkspaceEntityInternalApi
import com.intellij.platform.workspace.storage.annotations.Parent
import com.intellij.platform.workspace.storage.impl.EntityLink
import com.intellij.platform.workspace.storage.impl.ModifiableWorkspaceEntityBase
import com.intellij.platform.workspace.storage.impl.WorkspaceEntityBase
import com.intellij.platform.workspace.storage.impl.WorkspaceEntityData
import com.intellij.platform.workspace.storage.instrumentation.EntityStorageInstrumentation
import com.intellij.platform.workspace.storage.instrumentation.EntityStorageInstrumentationApi
import com.intellij.platform.workspace.storage.instrumentation.MutableEntityStorageInstrumentation
import com.intellij.platform.workspace.storage.instrumentation.instrumentation
import com.intellij.platform.workspace.storage.metadata.model.EntityMetadata
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.ApiStatus.Internal
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.workspacemodel.entities.BazelLibraryExtensionEntity
import org.jetbrains.bazel.workspacemodel.entities.BazelLibraryExtensionEntityBuilder
import org.jetbrains.bazel.workspacemodel.entities.WorkspaceModelTargetLabel

@Internal
@GeneratedCodeApiVersion(3)
@GeneratedCodeImplVersion(7)
@OptIn(WorkspaceEntityInternalApi::class)
internal class BazelLibraryExtensionEntityImpl(private val dataSource: BazelLibraryExtensionEntityData): BazelLibraryExtensionEntity, WorkspaceEntityBase(dataSource) {

private companion object {
internal val LIBRARY_CONNECTION_ID: ConnectionId = ConnectionId.create(LibraryEntity::class.java, BazelLibraryExtensionEntity::class.java, ConnectionId.ConnectionType.ONE_TO_ONE, false)
private val connections = listOf<ConnectionId>(LIBRARY_CONNECTION_ID)

}

override val library: LibraryEntity
get() = snapshot.instrumentation.getParent(LIBRARY_CONNECTION_ID, this) as? LibraryEntity ?: error("Parent library not found for BazelLibraryExtensionEntity")           
override val label: WorkspaceModelTargetLabel
get() {
readField("label")
return dataSource.label
}
override val isSynthetic: Boolean
get() {
readField("isSynthetic")
return dataSource.isSynthetic
}

override val entitySource: EntitySource
get() {
readField("entitySource")
return dataSource.entitySource
}

override fun connectionIdList(): List<ConnectionId> {
return connections
}


internal class Builder(result: BazelLibraryExtensionEntityData?): ModifiableWorkspaceEntityBase<BazelLibraryExtensionEntity, BazelLibraryExtensionEntityData>(result), BazelLibraryExtensionEntityBuilder {
internal constructor(): this(BazelLibraryExtensionEntityData())

override fun applyToBuilder(builder: MutableEntityStorage){
if (this.diff != null){
if (existsInBuilder(builder)){
this.diff = builder
return
}
else{
error("Entity BazelLibraryExtensionEntity is already created in a different builder")
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

private fun checkInitialization(){
val _diff = diff
if (!getEntityData().isEntitySourceInitialized()){
error("Field WorkspaceEntity#entitySource should be initialized")
}
if (_diff != null){
if (_diff.instrumentation.getParentBuilder(LIBRARY_CONNECTION_ID, this) == null){
error("Field BazelLibraryExtensionEntity#library should be initialized")
}
}
else{
if (this.entityLinks[EntityLink(false, LIBRARY_CONNECTION_ID)] == null){
error("Field BazelLibraryExtensionEntity#library should be initialized")
}
}
if (!getEntityData().isLabelInitialized()){
error("Field BazelLibraryExtensionEntity#label should be initialized")
}
}
override fun connectionIdList(): List<ConnectionId>{
return connections
}
// Relabeling code, move information from dataSource to this builder
override fun relabel(dataSource: WorkspaceEntity, parents: Set<WorkspaceEntity>?){
dataSource as BazelLibraryExtensionEntity
if (this.entitySource != dataSource.entitySource) this.entitySource = dataSource.entitySource
if (this.label != dataSource.label) this.label = dataSource.label
if (this.isSynthetic != dataSource.isSynthetic) this.isSynthetic = dataSource.isSynthetic
updateChildToParentReferences(parents)
}

        
override var entitySource: EntitySource
get() = getEntityData().entitySource
set(value) {
checkModificationAllowed()
getEntityData(true).entitySource = value
changedProperty.add("entitySource")

}
override var library: LibraryEntityBuilder
get(){
val _diff = diff
return if (_diff != null) {
((_diff as MutableEntityStorageInstrumentation).getParentBuilder(LIBRARY_CONNECTION_ID, this) as? LibraryEntityBuilder) ?: (this.entityLinks[EntityLink(false, LIBRARY_CONNECTION_ID)] as? LibraryEntityBuilder) ?: error("library is null for BazelLibraryExtensionEntity")
} else {
(this.entityLinks[EntityLink(false, LIBRARY_CONNECTION_ID)] as? LibraryEntityBuilder) ?: error("library is null for BazelLibraryExtensionEntity")
}
}
set(value){
checkModificationAllowed()
val _diff = diff
if (_diff != null && value is ModifiableWorkspaceEntityBase<*, *> && value.diff == null){
if (value is ModifiableWorkspaceEntityBase<*, *>){
value.entityLinks[EntityLink(true, LIBRARY_CONNECTION_ID)] = this
}
// else you're attaching a new entity to an existing entity that is not modifiable
_diff.addEntity(value as ModifiableWorkspaceEntityBase<WorkspaceEntity, *>)
}
if (_diff != null && (value !is ModifiableWorkspaceEntityBase<*, *> || value.diff != null)){
_diff.instrumentation.addChild(LIBRARY_CONNECTION_ID, value, this)
}
else{
if (value is ModifiableWorkspaceEntityBase<*, *>){
value.entityLinks[EntityLink(true, LIBRARY_CONNECTION_ID)] = this
}
// else you're attaching a new entity to an existing entity that is not modifiable
this.entityLinks[EntityLink(false, LIBRARY_CONNECTION_ID)] = value
}
changedProperty.add("library")
}

override var label: WorkspaceModelTargetLabel
get() = getEntityData().label
set(value) {
checkModificationAllowed()
getEntityData(true).label = value
changedProperty.add("label")

}
override var isSynthetic: Boolean
get() = getEntityData().isSynthetic
set(value) {
checkModificationAllowed()
getEntityData(true).isSynthetic = value
changedProperty.add("isSynthetic")
}

override fun getEntityClass(): Class<BazelLibraryExtensionEntity> = BazelLibraryExtensionEntity::class.java
}

}

@OptIn(WorkspaceEntityInternalApi::class)
internal class BazelLibraryExtensionEntityData : WorkspaceEntityData<BazelLibraryExtensionEntity>(){
lateinit var label: WorkspaceModelTargetLabel
var isSynthetic: Boolean = false

internal fun isLabelInitialized(): Boolean = ::label.isInitialized


override fun wrapAsModifiable(diff: MutableEntityStorage): WorkspaceEntityBuilder<BazelLibraryExtensionEntity>{
val modifiable = BazelLibraryExtensionEntityImpl.Builder(null)
modifiable.diff = diff
modifiable.id = createEntityId()
return modifiable
}

override fun createEntity(snapshot: EntityStorageInstrumentation): BazelLibraryExtensionEntity{
val entityId = createEntityId()
return snapshot.initializeEntity(entityId){
val entity = BazelLibraryExtensionEntityImpl(this)
entity.snapshot = snapshot
entity.id = entityId
entity
}
}

override fun getMetadata(): EntityMetadata{
return MetadataStorageImpl.getMetadataByTypeFqn("org.jetbrains.bazel.workspacemodel.entities.BazelLibraryExtensionEntity") as EntityMetadata
}

override fun getEntityInterface(): Class<out WorkspaceEntity>{
return BazelLibraryExtensionEntity::class.java
}

override fun createDetachedEntity(parents: List<WorkspaceEntityBuilder<*>>): WorkspaceEntityBuilder<*>{
return BazelLibraryExtensionEntity(label, isSynthetic, entitySource){
parents.filterIsInstance<LibraryEntityBuilder>().singleOrNull()?.let { this.library = it }
}
}

override fun getRequiredParents(): List<Class<out WorkspaceEntity>>{
val res = mutableListOf<Class<out WorkspaceEntity>>()
res.add(LibraryEntity::class.java)
return res
}

override fun equals(other: Any?): Boolean{
if (other == null) return false
if (this.javaClass != other.javaClass) return false
other as BazelLibraryExtensionEntityData
if (this.entitySource != other.entitySource) return false
if (this.label != other.label) return false
if (this.isSynthetic != other.isSynthetic) return false
return true
}

override fun equalsIgnoringEntitySource(other: Any?): Boolean{
if (other == null) return false
if (this.javaClass != other.javaClass) return false
other as BazelLibraryExtensionEntityData
if (this.label != other.label) return false
if (this.isSynthetic != other.isSynthetic) return false
return true
}

override fun hashCode(): Int{
var result = entitySource.hashCode()
result = 31 * result + label.hashCode()
result = 31 * result + isSynthetic.hashCode()
return result
}
override fun hashCodeIgnoringEntitySource(): Int{
var result = javaClass.hashCode()
result = 31 * result + label.hashCode()
result = 31 * result + isSynthetic.hashCode()
return result
}
}
