package org.jetbrains.workspacemodel.storage

import com.intellij.platform.workspace.storage.metadata.impl.MetadataStorageBase
import com.intellij.platform.workspace.storage.metadata.model.FinalClassMetadata
import com.intellij.platform.workspace.storage.metadata.model.OwnPropertyMetadata
import com.intellij.platform.workspace.storage.metadata.model.StorageTypeMetadata
import com.intellij.platform.workspace.storage.metadata.model.ValueTypeMetadata

public object MetadataStorageImpl : MetadataStorageBase() {
  init {

    var typeMetadata: StorageTypeMetadata

    typeMetadata = FinalClassMetadata.ClassMetadata(
      fqName = "org.jetbrains.workspacemodel.storage.BspEntitySource", properties = arrayListOf(
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
      ), supertypes = arrayListOf("com.intellij.platform.workspace.storage.EntitySource")
    )

    addMetadata(typeMetadata)
  }
}