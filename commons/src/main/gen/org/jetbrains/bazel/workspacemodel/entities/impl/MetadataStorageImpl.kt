package org.jetbrains.bazel.workspacemodel.entities.impl

import com.intellij.platform.workspace.storage.WorkspaceEntityInternalApi
import com.intellij.platform.workspace.storage.metadata.impl.MetadataStorageBase
import com.intellij.platform.workspace.storage.metadata.model.EntityMetadata
import com.intellij.platform.workspace.storage.metadata.model.FinalClassMetadata
import com.intellij.platform.workspace.storage.metadata.model.OwnPropertyMetadata
import com.intellij.platform.workspace.storage.metadata.model.StorageTypeMetadata
import com.intellij.platform.workspace.storage.metadata.model.ValueTypeMetadata

@OptIn(WorkspaceEntityInternalApi::class)
internal object MetadataStorageImpl : MetadataStorageBase() {
  override fun initializeMetadata() {

    var typeMetadata: StorageTypeMetadata

    typeMetadata = EntityMetadata(fqName = "org.jetbrains.bazel.workspacemodel.entities.AbstractBazelProjectDirectoriesEntity",
                                  entityDataFqName = "org.jetbrains.bazel.workspacemodel.entities.impl.AbstractBazelProjectDirectoriesEntityData",
                                  supertypes = listOf("com.intellij.platform.workspace.storage.WorkspaceEntity"), properties = listOf(
        OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "entitySource",
                            valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = false,
                                                                                typeMetadata = FinalClassMetadata.KnownClass(
                                                                                  fqName = "com.intellij.platform.workspace.storage.EntitySource")),
                            withDefault = false),
        OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "projectRoot",
                            valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = false,
                                                                                typeMetadata = FinalClassMetadata.KnownClass(
                                                                                  fqName = "com.intellij.platform.workspace.storage.url.VirtualFileUrl")),
                            withDefault = false)), extProperties = listOf(), isAbstract = true)

    addMetadata(typeMetadata)
  }

  override fun initializeMetadataHash() {
    addMetadataHash(typeFqn = "org.jetbrains.bazel.workspacemodel.entities.AbstractBazelProjectDirectoriesEntity",
                    metadataHash = -1844520802)
  }

}
