package org.jetbrains.workspacemodel.entities

import com.intellij.platform.workspace.storage.metadata.impl.MetadataStorageBase
import com.intellij.platform.workspace.storage.metadata.model.EntityMetadata
import com.intellij.platform.workspace.storage.metadata.model.FinalClassMetadata
import com.intellij.platform.workspace.storage.metadata.model.OwnPropertyMetadata
import com.intellij.platform.workspace.storage.metadata.model.StorageTypeMetadata
import com.intellij.platform.workspace.storage.metadata.model.ValueTypeMetadata

public object MetadataStorageImpl : MetadataStorageBase() {
  override fun initializeMetadata() {
    val primitiveTypeListNotNullable = ValueTypeMetadata.SimpleType.PrimitiveType(isNullable = false, type = "List")

    var typeMetadata: StorageTypeMetadata

    typeMetadata = FinalClassMetadata.ObjectMetadata(
      fqName = "org.jetbrains.workspacemodel.entities.BspEntitySource",
      properties = listOf(
        OwnPropertyMetadata(
          isComputable = false,
          isKey = false,
          isOpen = false,
          name = "virtualFileUrl",
          valueType = ValueTypeMetadata.SimpleType.CustomType(
            isNullable = true,
            typeMetadata = FinalClassMetadata.KnownClass(fqName = "com.intellij.platform.workspace.storage.url.VirtualFileUrl")
          ),
          withDefault = false
        )
      ),
      supertypes = listOf("com.intellij.platform.workspace.storage.EntitySource")
    )

    addMetadata(typeMetadata)

    typeMetadata = FinalClassMetadata.ObjectMetadata(
      fqName = "org.jetbrains.workspacemodel.entities.BspDummyEntitySource",
      properties = listOf(
        OwnPropertyMetadata(
          isComputable = false,
          isKey = false,
          isOpen = false,
          name = "virtualFileUrl",
          valueType = ValueTypeMetadata.SimpleType.CustomType(
            isNullable = true,
            typeMetadata = FinalClassMetadata.KnownClass(fqName = "com.intellij.platform.workspace.storage.url.VirtualFileUrl")
          ),
          withDefault = false
        )
      ),
      supertypes = listOf("com.intellij.platform.workspace.storage.EntitySource")
    )

    addMetadata(typeMetadata)

    typeMetadata = EntityMetadata(
      fqName = "org.jetbrains.workspacemodel.entities.BspProjectDirectoriesEntity",
      entityDataFqName = "org.jetbrains.workspacemodel.entities.BspProjectDirectoriesEntityData",
      supertypes = listOf("com.intellij.platform.workspace.storage.WorkspaceEntity"),
      properties = listOf(
        OwnPropertyMetadata(
          isComputable = false,
          isKey = false,
          isOpen = false,
          name = "entitySource",
          valueType = ValueTypeMetadata.SimpleType.CustomType(
            isNullable = false,
            typeMetadata = FinalClassMetadata.KnownClass(fqName = "com.intellij.platform.workspace.storage.EntitySource")
          ),
          withDefault = false
        ),
        OwnPropertyMetadata(
          isComputable = false,
          isKey = false,
          isOpen = false,
          name = "projectRoot",
          valueType = ValueTypeMetadata.SimpleType.CustomType(
            isNullable = false,
            typeMetadata = FinalClassMetadata.KnownClass(fqName = "com.intellij.platform.workspace.storage.url.VirtualFileUrl")
          ),
          withDefault = false
        ),
        OwnPropertyMetadata(
          isComputable = false,
          isKey = false,
          isOpen = false,
          name = "includedRoots",
          valueType = ValueTypeMetadata.ParameterizedType(
            generics = listOf(
              ValueTypeMetadata.SimpleType.CustomType(
                isNullable = false,
                typeMetadata = FinalClassMetadata.KnownClass(fqName = "com.intellij.platform.workspace.storage.url.VirtualFileUrl")
              )
            ), primitive = primitiveTypeListNotNullable
          ),
          withDefault = false
        ),
        OwnPropertyMetadata(
          isComputable = false,
          isKey = false,
          isOpen = false,
          name = "excludedRoots",
          valueType = ValueTypeMetadata.ParameterizedType(
            generics = listOf(
              ValueTypeMetadata.SimpleType.CustomType(
                isNullable = false,
                typeMetadata = FinalClassMetadata.KnownClass(fqName = "com.intellij.platform.workspace.storage.url.VirtualFileUrl")
              )
            ), primitive = primitiveTypeListNotNullable
          ),
          withDefault = false
        )
      ),
      extProperties = arrayListOf(),
      isAbstract = false
    )

    addMetadata(typeMetadata)
  }

  override fun initializeMetadataHash() {
    addMetadataHash(
      typeFqn = "org.jetbrains.workspacemodel.entities.BspProjectDirectoriesEntity",
      metadataHash = -565268675
    )
    addMetadataHash(typeFqn = "com.intellij.platform.workspace.storage.EntitySource", metadataHash = -27777658)
    addMetadataHash(typeFqn = "org.jetbrains.workspacemodel.entities.BspDummyEntitySource", metadataHash = 124279478)
    addMetadataHash(typeFqn = "org.jetbrains.workspacemodel.entities.BspEntitySource", metadataHash = 2092063924)
  }

}
