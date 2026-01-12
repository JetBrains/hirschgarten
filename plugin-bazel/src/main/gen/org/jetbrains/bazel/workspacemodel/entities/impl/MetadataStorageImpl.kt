package org.jetbrains.bazel.workspacemodel.entities.impl

import com.intellij.platform.workspace.storage.ConnectionId
import com.intellij.platform.workspace.storage.WorkspaceEntityInternalApi
import com.intellij.platform.workspace.storage.metadata.impl.MetadataStorageBase
import com.intellij.platform.workspace.storage.metadata.model.EntityMetadata
import com.intellij.platform.workspace.storage.metadata.model.ExtPropertyMetadata
import com.intellij.platform.workspace.storage.metadata.model.ExtendableClassMetadata
import com.intellij.platform.workspace.storage.metadata.model.FinalClassMetadata
import com.intellij.platform.workspace.storage.metadata.model.OwnPropertyMetadata
import com.intellij.platform.workspace.storage.metadata.model.StorageTypeMetadata
import com.intellij.platform.workspace.storage.metadata.model.ValueTypeMetadata

@OptIn(WorkspaceEntityInternalApi::class)
internal object MetadataStorageImpl : MetadataStorageBase() {
  override fun initializeMetadata() {
    val primitiveTypeStringNotNullable = ValueTypeMetadata.SimpleType.PrimitiveType(isNullable = false, type = "String")
    val primitiveTypeIntNotNullable = ValueTypeMetadata.SimpleType.PrimitiveType(isNullable = false, type = "Int")
    val primitiveTypeListNotNullable = ValueTypeMetadata.SimpleType.PrimitiveType(isNullable = false, type = "List")
    val primitiveTypeBooleanNotNullable = ValueTypeMetadata.SimpleType.PrimitiveType(isNullable = false, type = "Boolean")
    val primitiveTypeSetNotNullable = ValueTypeMetadata.SimpleType.PrimitiveType(isNullable = false, type = "Set")

    var typeMetadata: StorageTypeMetadata

    typeMetadata = FinalClassMetadata.ClassMetadata(fqName = "org.jetbrains.bazel.workspacemodel.entities.BazelModuleEntitySource",
                                                    properties = listOf(OwnPropertyMetadata(isComputable = false,
                                                                                            isKey = false,
                                                                                            isOpen = false,
                                                                                            name = "moduleName",
                                                                                            valueType = primitiveTypeStringNotNullable,
                                                                                            withDefault = false),
                                                                        OwnPropertyMetadata(isComputable = false,
                                                                                            isKey = false,
                                                                                            isOpen = false,
                                                                                            name = "virtualFileUrl",
                                                                                            valueType = ValueTypeMetadata.SimpleType.CustomType(
                                                                                              isNullable = true,
                                                                                              typeMetadata = FinalClassMetadata.KnownClass(
                                                                                                fqName = "com.intellij.platform.workspace.storage.url.VirtualFileUrl")),
                                                                                            withDefault = false)),
                                                    supertypes = listOf("com.intellij.platform.workspace.storage.EntitySource",
                                                                        "org.jetbrains.bazel.workspacemodel.entities.BazelEntitySource"))

    addMetadata(typeMetadata)

    typeMetadata = FinalClassMetadata.ObjectMetadata(fqName = "org.jetbrains.bazel.workspacemodel.entities.BazelDummyEntitySource",
                                                     properties = listOf(OwnPropertyMetadata(isComputable = false,
                                                                                             isKey = false,
                                                                                             isOpen = false,
                                                                                             name = "virtualFileUrl",
                                                                                             valueType = ValueTypeMetadata.SimpleType.CustomType(
                                                                                               isNullable = true,
                                                                                               typeMetadata = FinalClassMetadata.KnownClass(
                                                                                                 fqName = "com.intellij.platform.workspace.storage.url.VirtualFileUrl")),
                                                                                             withDefault = false)),
                                                     supertypes = listOf("com.intellij.platform.workspace.storage.EntitySource",
                                                                         "org.jetbrains.bazel.workspacemodel.entities.BazelEntitySource"))

    addMetadata(typeMetadata)

    typeMetadata = FinalClassMetadata.ObjectMetadata(fqName = "org.jetbrains.bazel.workspacemodel.entities.BazelProjectEntitySource",
                                                     properties = listOf(OwnPropertyMetadata(isComputable = false,
                                                                                             isKey = false,
                                                                                             isOpen = false,
                                                                                             name = "virtualFileUrl",
                                                                                             valueType = ValueTypeMetadata.SimpleType.CustomType(
                                                                                               isNullable = true,
                                                                                               typeMetadata = FinalClassMetadata.KnownClass(
                                                                                                 fqName = "com.intellij.platform.workspace.storage.url.VirtualFileUrl")),
                                                                                             withDefault = false)),
                                                     supertypes = listOf("com.intellij.platform.workspace.storage.EntitySource",
                                                                         "org.jetbrains.bazel.workspacemodel.entities.BazelEntitySource"))

    addMetadata(typeMetadata)

    typeMetadata =
      FinalClassMetadata.ClassMetadata(fqName = "org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeId",
                                       properties = listOf(OwnPropertyMetadata(isComputable = false,
                                                                               isKey = false,
                                                                               isOpen = false,
                                                                               name = "id",
                                                                               valueType = primitiveTypeIntNotNullable,
                                                                               withDefault = false),
                                                           OwnPropertyMetadata(isComputable = false,
                                                                               isKey = false,
                                                                               isOpen = false,
                                                                               name = "presentableName",
                                                                               valueType = primitiveTypeStringNotNullable,
                                                                               withDefault = false)),
                                       supertypes = listOf("com.intellij.platform.workspace.storage.SymbolicEntityId"))

    addMetadata(typeMetadata)

    typeMetadata = FinalClassMetadata.ClassMetadata(fqName = "org.jetbrains.bazel.workspacemodel.entities.PackageNameId",
                                                    properties = listOf(OwnPropertyMetadata(isComputable = false,
                                                                                            isKey = false,
                                                                                            isOpen = false,
                                                                                            name = "packageName",
                                                                                            valueType = primitiveTypeStringNotNullable,
                                                                                            withDefault = false),
                                                                        OwnPropertyMetadata(isComputable = false,
                                                                                            isKey = false,
                                                                                            isOpen = false,
                                                                                            name = "presentableName",
                                                                                            valueType = primitiveTypeStringNotNullable,
                                                                                            withDefault = false)),
                                                    supertypes = listOf("com.intellij.platform.workspace.storage.SymbolicEntityId"))

    addMetadata(typeMetadata)

    typeMetadata = EntityMetadata(fqName = "org.jetbrains.bazel.workspacemodel.entities.BazelJavaSourceRootEntity",
                                  entityDataFqName = "org.jetbrains.bazel.workspacemodel.entities.impl.BazelJavaSourceRootEntityData",
                                  supertypes = listOf("com.intellij.platform.workspace.storage.WorkspaceEntity"),
                                  properties = listOf(OwnPropertyMetadata(isComputable = false,
                                                                          isKey = false,
                                                                          isOpen = false,
                                                                          name = "entitySource",
                                                                          valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = false,
                                                                                                                              typeMetadata = FinalClassMetadata.KnownClass(
                                                                                                                                fqName = "com.intellij.platform.workspace.storage.EntitySource")),
                                                                          withDefault = false),
                                                      OwnPropertyMetadata(isComputable = false,
                                                                          isKey = false,
                                                                          isOpen = false,
                                                                          name = "packageNameId",
                                                                          valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = false,
                                                                                                                              typeMetadata = FinalClassMetadata.ClassMetadata(
                                                                                                                                fqName = "org.jetbrains.bazel.workspacemodel.entities.PackageNameId",
                                                                                                                                properties = listOf(
                                                                                                                                  OwnPropertyMetadata(
                                                                                                                                    isComputable = false,
                                                                                                                                    isKey = false,
                                                                                                                                    isOpen = false,
                                                                                                                                    name = "packageName",
                                                                                                                                    valueType = primitiveTypeStringNotNullable,
                                                                                                                                    withDefault = false),
                                                                                                                                  OwnPropertyMetadata(
                                                                                                                                    isComputable = false,
                                                                                                                                    isKey = false,
                                                                                                                                    isOpen = false,
                                                                                                                                    name = "presentableName",
                                                                                                                                    valueType = primitiveTypeStringNotNullable,
                                                                                                                                    withDefault = false)),
                                                                                                                                supertypes = listOf(
                                                                                                                                  "com.intellij.platform.workspace.storage.SymbolicEntityId"))),
                                                                          withDefault = false),
                                                      OwnPropertyMetadata(isComputable = false,
                                                                          isKey = false,
                                                                          isOpen = false,
                                                                          name = "sourceRoots",
                                                                          valueType = ValueTypeMetadata.ParameterizedType(generics = listOf(
                                                                            ValueTypeMetadata.SimpleType.CustomType(isNullable = false,
                                                                                                                    typeMetadata = FinalClassMetadata.KnownClass(
                                                                                                                      fqName = "com.intellij.platform.workspace.storage.url.VirtualFileUrl"))),
                                                                                                                          primitive = primitiveTypeListNotNullable),
                                                                          withDefault = false)),
                                  extProperties = listOf(),
                                  isAbstract = false)

    addMetadata(typeMetadata)

    typeMetadata = EntityMetadata(fqName = "org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntity",
                                  entityDataFqName = "org.jetbrains.bazel.workspacemodel.entities.impl.BazelProjectDirectoriesEntityData",
                                  supertypes = listOf("com.intellij.platform.workspace.storage.WorkspaceEntity"),
                                  properties = listOf(OwnPropertyMetadata(isComputable = false,
                                                                          isKey = false,
                                                                          isOpen = false,
                                                                          name = "entitySource",
                                                                          valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = false,
                                                                                                                              typeMetadata = FinalClassMetadata.KnownClass(
                                                                                                                                fqName = "com.intellij.platform.workspace.storage.EntitySource")),
                                                                          withDefault = false),
                                                      OwnPropertyMetadata(isComputable = false,
                                                                          isKey = false,
                                                                          isOpen = false,
                                                                          name = "projectRoot",
                                                                          valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = false,
                                                                                                                              typeMetadata = FinalClassMetadata.KnownClass(
                                                                                                                                fqName = "com.intellij.platform.workspace.storage.url.VirtualFileUrl")),
                                                                          withDefault = false),
                                                      OwnPropertyMetadata(isComputable = false,
                                                                          isKey = false,
                                                                          isOpen = false,
                                                                          name = "includedRoots",
                                                                          valueType = ValueTypeMetadata.ParameterizedType(generics = listOf(
                                                                            ValueTypeMetadata.SimpleType.CustomType(isNullable = false,
                                                                                                                    typeMetadata = FinalClassMetadata.KnownClass(
                                                                                                                      fqName = "com.intellij.platform.workspace.storage.url.VirtualFileUrl"))),
                                                                                                                          primitive = primitiveTypeListNotNullable),
                                                                          withDefault = false),
                                                      OwnPropertyMetadata(isComputable = false,
                                                                          isKey = false,
                                                                          isOpen = false,
                                                                          name = "excludedRoots",
                                                                          valueType = ValueTypeMetadata.ParameterizedType(generics = listOf(
                                                                            ValueTypeMetadata.SimpleType.CustomType(isNullable = false,
                                                                                                                    typeMetadata = FinalClassMetadata.KnownClass(
                                                                                                                      fqName = "com.intellij.platform.workspace.storage.url.VirtualFileUrl"))),
                                                                                                                          primitive = primitiveTypeListNotNullable),
                                                                          withDefault = false),
                                                      OwnPropertyMetadata(isComputable = false,
                                                                          isKey = false,
                                                                          isOpen = false,
                                                                          name = "indexAllFilesInIncludedRoots",
                                                                          valueType = primitiveTypeBooleanNotNullable,
                                                                          withDefault = false),
                                                      OwnPropertyMetadata(isComputable = false,
                                                                          isKey = false,
                                                                          isOpen = false,
                                                                          name = "indexAdditionalFiles",
                                                                          valueType = ValueTypeMetadata.ParameterizedType(generics = listOf(
                                                                            ValueTypeMetadata.SimpleType.CustomType(isNullable = false,
                                                                                                                    typeMetadata = FinalClassMetadata.KnownClass(
                                                                                                                      fqName = "com.intellij.platform.workspace.storage.url.VirtualFileUrl"))),
                                                                                                                          primitive = primitiveTypeListNotNullable),
                                                                          withDefault = false)),
                                  extProperties = listOf(),
                                  isAbstract = false)

    addMetadata(typeMetadata)

    typeMetadata = EntityMetadata(fqName = "org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeEntity",
                                  entityDataFqName = "org.jetbrains.bazel.workspacemodel.entities.impl.CompiledSourceCodeInsideJarExcludeEntityData",
                                  supertypes = listOf("com.intellij.platform.workspace.storage.WorkspaceEntity",
                                                      "com.intellij.platform.workspace.storage.WorkspaceEntityWithSymbolicId"),
                                  properties = listOf(OwnPropertyMetadata(isComputable = false,
                                                                          isKey = false,
                                                                          isOpen = false,
                                                                          name = "entitySource",
                                                                          valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = false,
                                                                                                                              typeMetadata = FinalClassMetadata.KnownClass(
                                                                                                                                fqName = "com.intellij.platform.workspace.storage.EntitySource")),
                                                                          withDefault = false),
                                                      OwnPropertyMetadata(isComputable = false,
                                                                          isKey = false,
                                                                          isOpen = false,
                                                                          name = "relativePathsInsideJarToExclude",
                                                                          valueType = ValueTypeMetadata.ParameterizedType(generics = listOf(
                                                                            primitiveTypeStringNotNullable),
                                                                                                                          primitive = primitiveTypeSetNotNullable),
                                                                          withDefault = false),
                                                      OwnPropertyMetadata(isComputable = false,
                                                                          isKey = false,
                                                                          isOpen = false,
                                                                          name = "librariesFromInternalTargetsUrls",
                                                                          valueType = ValueTypeMetadata.ParameterizedType(generics = listOf(
                                                                            primitiveTypeStringNotNullable),
                                                                                                                          primitive = primitiveTypeSetNotNullable),
                                                                          withDefault = false),
                                                      OwnPropertyMetadata(isComputable = false,
                                                                          isKey = false,
                                                                          isOpen = false,
                                                                          name = "excludeId",
                                                                          valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = false,
                                                                                                                              typeMetadata = FinalClassMetadata.ClassMetadata(
                                                                                                                                fqName = "org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeId",
                                                                                                                                properties = listOf(
                                                                                                                                  OwnPropertyMetadata(
                                                                                                                                    isComputable = false,
                                                                                                                                    isKey = false,
                                                                                                                                    isOpen = false,
                                                                                                                                    name = "id",
                                                                                                                                    valueType = primitiveTypeIntNotNullable,
                                                                                                                                    withDefault = false),
                                                                                                                                  OwnPropertyMetadata(
                                                                                                                                    isComputable = false,
                                                                                                                                    isKey = false,
                                                                                                                                    isOpen = false,
                                                                                                                                    name = "presentableName",
                                                                                                                                    valueType = primitiveTypeStringNotNullable,
                                                                                                                                    withDefault = false)),
                                                                                                                                supertypes = listOf(
                                                                                                                                  "com.intellij.platform.workspace.storage.SymbolicEntityId"))),
                                                                          withDefault = false),
                                                      OwnPropertyMetadata(isComputable = true,
                                                                          isKey = false,
                                                                          isOpen = false,
                                                                          name = "symbolicId",
                                                                          valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = false,
                                                                                                                              typeMetadata = FinalClassMetadata.ClassMetadata(
                                                                                                                                fqName = "org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeId",
                                                                                                                                properties = listOf(
                                                                                                                                  OwnPropertyMetadata(
                                                                                                                                    isComputable = false,
                                                                                                                                    isKey = false,
                                                                                                                                    isOpen = false,
                                                                                                                                    name = "id",
                                                                                                                                    valueType = primitiveTypeIntNotNullable,
                                                                                                                                    withDefault = false),
                                                                                                                                  OwnPropertyMetadata(
                                                                                                                                    isComputable = false,
                                                                                                                                    isKey = false,
                                                                                                                                    isOpen = false,
                                                                                                                                    name = "presentableName",
                                                                                                                                    valueType = primitiveTypeStringNotNullable,
                                                                                                                                    withDefault = false)),
                                                                                                                                supertypes = listOf(
                                                                                                                                  "com.intellij.platform.workspace.storage.SymbolicEntityId"))),
                                                                          withDefault = false)),
                                  extProperties = listOf(),
                                  isAbstract = false)

    addMetadata(typeMetadata)

    typeMetadata = EntityMetadata(fqName = "org.jetbrains.bazel.workspacemodel.entities.JvmBinaryJarsEntity",
                                  entityDataFqName = "org.jetbrains.bazel.workspacemodel.entities.impl.JvmBinaryJarsEntityData",
                                  supertypes = listOf("com.intellij.platform.workspace.storage.WorkspaceEntity"),
                                  properties = listOf(OwnPropertyMetadata(isComputable = false,
                                                                          isKey = false,
                                                                          isOpen = false,
                                                                          name = "entitySource",
                                                                          valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = false,
                                                                                                                              typeMetadata = FinalClassMetadata.KnownClass(
                                                                                                                                fqName = "com.intellij.platform.workspace.storage.EntitySource")),
                                                                          withDefault = false),
                                                      OwnPropertyMetadata(isComputable = false,
                                                                          isKey = false,
                                                                          isOpen = false,
                                                                          name = "jars",
                                                                          valueType = ValueTypeMetadata.ParameterizedType(generics = listOf(
                                                                            ValueTypeMetadata.SimpleType.CustomType(isNullable = false,
                                                                                                                    typeMetadata = FinalClassMetadata.KnownClass(
                                                                                                                      fqName = "com.intellij.platform.workspace.storage.url.VirtualFileUrl"))),
                                                                                                                          primitive = primitiveTypeListNotNullable),
                                                                          withDefault = false),
                                                      OwnPropertyMetadata(isComputable = false,
                                                                          isKey = false,
                                                                          isOpen = false,
                                                                          name = "module",
                                                                          valueType = ValueTypeMetadata.EntityReference(connectionType = ConnectionId.ConnectionType.ONE_TO_ONE,
                                                                                                                        entityFqName = "com.intellij.platform.workspace.jps.entities.ModuleEntity",
                                                                                                                        isChild = false,
                                                                                                                        isNullable = false),
                                                                          withDefault = false)),
                                  extProperties = listOf(ExtPropertyMetadata(isComputable = false,
                                                                             isOpen = false,
                                                                             name = "jvmBinaryJarsEntity",
                                                                             receiverFqn = "com.intellij.platform.workspace.jps.entities.ModuleEntity",
                                                                             valueType = ValueTypeMetadata.EntityReference(connectionType = ConnectionId.ConnectionType.ONE_TO_ONE,
                                                                                                                           entityFqName = "org.jetbrains.bazel.workspacemodel.entities.JvmBinaryJarsEntity",
                                                                                                                           isChild = true,
                                                                                                                           isNullable = true),
                                                                             withDefault = false)),
                                  isAbstract = false)

    addMetadata(typeMetadata)

    typeMetadata = EntityMetadata(fqName = "org.jetbrains.bazel.workspacemodel.entities.LibraryCompiledSourceCodeInsideJarExcludeEntity",
                                  entityDataFqName = "org.jetbrains.bazel.workspacemodel.entities.impl.LibraryCompiledSourceCodeInsideJarExcludeEntityData",
                                  supertypes = listOf("com.intellij.platform.workspace.storage.WorkspaceEntity"),
                                  properties = listOf(OwnPropertyMetadata(isComputable = false,
                                                                          isKey = false,
                                                                          isOpen = false,
                                                                          name = "entitySource",
                                                                          valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = false,
                                                                                                                              typeMetadata = FinalClassMetadata.KnownClass(
                                                                                                                                fqName = "com.intellij.platform.workspace.storage.EntitySource")),
                                                                          withDefault = false),
                                                      OwnPropertyMetadata(isComputable = false,
                                                                          isKey = false,
                                                                          isOpen = false,
                                                                          name = "libraryId",
                                                                          valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = false,
                                                                                                                              typeMetadata = FinalClassMetadata.ClassMetadata(
                                                                                                                                fqName = "com.intellij.platform.workspace.jps.entities.LibraryId",
                                                                                                                                properties = listOf(
                                                                                                                                  OwnPropertyMetadata(
                                                                                                                                    isComputable = false,
                                                                                                                                    isKey = false,
                                                                                                                                    isOpen = false,
                                                                                                                                    name = "codeCache",
                                                                                                                                    valueType = primitiveTypeIntNotNullable,
                                                                                                                                    withDefault = false),
                                                                                                                                  OwnPropertyMetadata(
                                                                                                                                    isComputable = false,
                                                                                                                                    isKey = false,
                                                                                                                                    isOpen = false,
                                                                                                                                    name = "name",
                                                                                                                                    valueType = primitiveTypeStringNotNullable,
                                                                                                                                    withDefault = false),
                                                                                                                                  OwnPropertyMetadata(
                                                                                                                                    isComputable = false,
                                                                                                                                    isKey = false,
                                                                                                                                    isOpen = false,
                                                                                                                                    name = "presentableName",
                                                                                                                                    valueType = primitiveTypeStringNotNullable,
                                                                                                                                    withDefault = false),
                                                                                                                                  OwnPropertyMetadata(
                                                                                                                                    isComputable = false,
                                                                                                                                    isKey = false,
                                                                                                                                    isOpen = false,
                                                                                                                                    name = "tableId",
                                                                                                                                    valueType = ValueTypeMetadata.SimpleType.CustomType(
                                                                                                                                      isNullable = false,
                                                                                                                                      typeMetadata = ExtendableClassMetadata.AbstractClassMetadata(
                                                                                                                                        fqName = "com.intellij.platform.workspace.jps.entities.LibraryTableId",
                                                                                                                                        subclasses = listOf(
                                                                                                                                          FinalClassMetadata.ObjectMetadata(
                                                                                                                                            fqName = "com.intellij.platform.workspace.jps.entities.LibraryTableId\$ProjectLibraryTableId",
                                                                                                                                            properties = listOf(
                                                                                                                                              OwnPropertyMetadata(
                                                                                                                                                isComputable = false,
                                                                                                                                                isKey = false,
                                                                                                                                                isOpen = false,
                                                                                                                                                name = "level",
                                                                                                                                                valueType = primitiveTypeStringNotNullable,
                                                                                                                                                withDefault = false)),
                                                                                                                                            supertypes = listOf(
                                                                                                                                              "com.intellij.platform.workspace.jps.entities.LibraryTableId",
                                                                                                                                              "java.io.Serializable")),
                                                                                                                                          FinalClassMetadata.ClassMetadata(
                                                                                                                                            fqName = "com.intellij.platform.workspace.jps.entities.LibraryTableId\$GlobalLibraryTableId",
                                                                                                                                            properties = listOf(
                                                                                                                                              OwnPropertyMetadata(
                                                                                                                                                isComputable = false,
                                                                                                                                                isKey = false,
                                                                                                                                                isOpen = false,
                                                                                                                                                name = "level",
                                                                                                                                                valueType = primitiveTypeStringNotNullable,
                                                                                                                                                withDefault = false)),
                                                                                                                                            supertypes = listOf(
                                                                                                                                              "com.intellij.platform.workspace.jps.entities.LibraryTableId",
                                                                                                                                              "java.io.Serializable")),
                                                                                                                                          FinalClassMetadata.ClassMetadata(
                                                                                                                                            fqName = "com.intellij.platform.workspace.jps.entities.LibraryTableId\$ModuleLibraryTableId",
                                                                                                                                            properties = listOf(
                                                                                                                                              OwnPropertyMetadata(
                                                                                                                                                isComputable = false,
                                                                                                                                                isKey = false,
                                                                                                                                                isOpen = false,
                                                                                                                                                name = "level",
                                                                                                                                                valueType = primitiveTypeStringNotNullable,
                                                                                                                                                withDefault = false),
                                                                                                                                              OwnPropertyMetadata(
                                                                                                                                                isComputable = false,
                                                                                                                                                isKey = false,
                                                                                                                                                isOpen = false,
                                                                                                                                                name = "moduleId",
                                                                                                                                                valueType = ValueTypeMetadata.SimpleType.CustomType(
                                                                                                                                                  isNullable = false,
                                                                                                                                                  typeMetadata = FinalClassMetadata.ClassMetadata(
                                                                                                                                                    fqName = "com.intellij.platform.workspace.jps.entities.ModuleId",
                                                                                                                                                    properties = listOf(
                                                                                                                                                      OwnPropertyMetadata(
                                                                                                                                                        isComputable = false,
                                                                                                                                                        isKey = false,
                                                                                                                                                        isOpen = false,
                                                                                                                                                        name = "name",
                                                                                                                                                        valueType = primitiveTypeStringNotNullable,
                                                                                                                                                        withDefault = false),
                                                                                                                                                      OwnPropertyMetadata(
                                                                                                                                                        isComputable = false,
                                                                                                                                                        isKey = false,
                                                                                                                                                        isOpen = false,
                                                                                                                                                        name = "presentableName",
                                                                                                                                                        valueType = primitiveTypeStringNotNullable,
                                                                                                                                                        withDefault = false)),
                                                                                                                                                    supertypes = listOf(
                                                                                                                                                      "com.intellij.platform.workspace.storage.SymbolicEntityId"))),
                                                                                                                                                withDefault = false)),
                                                                                                                                            supertypes = listOf(
                                                                                                                                              "com.intellij.platform.workspace.jps.entities.LibraryTableId",
                                                                                                                                              "java.io.Serializable"))),
                                                                                                                                        supertypes = listOf(
                                                                                                                                          "java.io.Serializable"))),
                                                                                                                                    withDefault = false)),
                                                                                                                                supertypes = listOf(
                                                                                                                                  "com.intellij.platform.workspace.storage.SymbolicEntityId"))),
                                                                          withDefault = false),
                                                      OwnPropertyMetadata(isComputable = false,
                                                                          isKey = false,
                                                                          isOpen = false,
                                                                          name = "compiledSourceCodeInsideJarExcludeId",
                                                                          valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = false,
                                                                                                                              typeMetadata = FinalClassMetadata.ClassMetadata(
                                                                                                                                fqName = "org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeId",
                                                                                                                                properties = listOf(
                                                                                                                                  OwnPropertyMetadata(
                                                                                                                                    isComputable = false,
                                                                                                                                    isKey = false,
                                                                                                                                    isOpen = false,
                                                                                                                                    name = "id",
                                                                                                                                    valueType = primitiveTypeIntNotNullable,
                                                                                                                                    withDefault = false),
                                                                                                                                  OwnPropertyMetadata(
                                                                                                                                    isComputable = false,
                                                                                                                                    isKey = false,
                                                                                                                                    isOpen = false,
                                                                                                                                    name = "presentableName",
                                                                                                                                    valueType = primitiveTypeStringNotNullable,
                                                                                                                                    withDefault = false)),
                                                                                                                                supertypes = listOf(
                                                                                                                                  "com.intellij.platform.workspace.storage.SymbolicEntityId"))),
                                                                          withDefault = false)),
                                  extProperties = listOf(),
                                  isAbstract = false)

    addMetadata(typeMetadata)

    typeMetadata = EntityMetadata(fqName = "org.jetbrains.bazel.workspacemodel.entities.PackageMarkerEntity",
                                  entityDataFqName = "org.jetbrains.bazel.workspacemodel.entities.impl.PackageMarkerEntityData",
                                  supertypes = listOf("com.intellij.platform.workspace.storage.WorkspaceEntity"),
                                  properties = listOf(OwnPropertyMetadata(isComputable = false,
                                                                          isKey = false,
                                                                          isOpen = false,
                                                                          name = "entitySource",
                                                                          valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = false,
                                                                                                                              typeMetadata = FinalClassMetadata.KnownClass(
                                                                                                                                fqName = "com.intellij.platform.workspace.storage.EntitySource")),
                                                                          withDefault = false),
                                                      OwnPropertyMetadata(isComputable = false,
                                                                          isKey = false,
                                                                          isOpen = false,
                                                                          name = "root",
                                                                          valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = false,
                                                                                                                              typeMetadata = FinalClassMetadata.KnownClass(
                                                                                                                                fqName = "com.intellij.platform.workspace.storage.url.VirtualFileUrl")),
                                                                          withDefault = false),
                                                      OwnPropertyMetadata(isComputable = false,
                                                                          isKey = false,
                                                                          isOpen = false,
                                                                          name = "packagePrefix",
                                                                          valueType = primitiveTypeStringNotNullable,
                                                                          withDefault = false),
                                                      OwnPropertyMetadata(isComputable = false,
                                                                          isKey = false,
                                                                          isOpen = false,
                                                                          name = "module",
                                                                          valueType = ValueTypeMetadata.EntityReference(connectionType = ConnectionId.ConnectionType.ONE_TO_MANY,
                                                                                                                        entityFqName = "com.intellij.platform.workspace.jps.entities.ModuleEntity",
                                                                                                                        isChild = false,
                                                                                                                        isNullable = false),
                                                                          withDefault = false)),
                                  extProperties = listOf(ExtPropertyMetadata(isComputable = false,
                                                                             isOpen = false,
                                                                             name = "packageMarkerEntities",
                                                                             receiverFqn = "com.intellij.platform.workspace.jps.entities.ModuleEntity",
                                                                             valueType = ValueTypeMetadata.EntityReference(connectionType = ConnectionId.ConnectionType.ONE_TO_MANY,
                                                                                                                           entityFqName = "org.jetbrains.bazel.workspacemodel.entities.PackageMarkerEntity",
                                                                                                                           isChild = true,
                                                                                                                           isNullable = false),
                                                                             withDefault = false)),
                                  isAbstract = false)

    addMetadata(typeMetadata)

    typeMetadata = EntityMetadata(fqName = "org.jetbrains.bazel.workspacemodel.entities.ScalaAddendumEntity",
                                  entityDataFqName = "org.jetbrains.bazel.workspacemodel.entities.impl.ScalaAddendumEntityData",
                                  supertypes = listOf("com.intellij.platform.workspace.storage.WorkspaceEntity"),
                                  properties = listOf(OwnPropertyMetadata(isComputable = false,
                                                                          isKey = false,
                                                                          isOpen = false,
                                                                          name = "entitySource",
                                                                          valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = false,
                                                                                                                              typeMetadata = FinalClassMetadata.KnownClass(
                                                                                                                                fqName = "com.intellij.platform.workspace.storage.EntitySource")),
                                                                          withDefault = false),
                                                      OwnPropertyMetadata(isComputable = false,
                                                                          isKey = false,
                                                                          isOpen = false,
                                                                          name = "compilerVersion",
                                                                          valueType = primitiveTypeStringNotNullable,
                                                                          withDefault = false),
                                                      OwnPropertyMetadata(isComputable = false,
                                                                          isKey = false,
                                                                          isOpen = false,
                                                                          name = "scalacOptions",
                                                                          valueType = ValueTypeMetadata.ParameterizedType(generics = listOf(
                                                                            primitiveTypeStringNotNullable),
                                                                                                                          primitive = primitiveTypeListNotNullable),
                                                                          withDefault = false),
                                                      OwnPropertyMetadata(isComputable = false,
                                                                          isKey = false,
                                                                          isOpen = false,
                                                                          name = "sdkClasspaths",
                                                                          valueType = ValueTypeMetadata.ParameterizedType(generics = listOf(
                                                                            ValueTypeMetadata.SimpleType.CustomType(isNullable = false,
                                                                                                                    typeMetadata = FinalClassMetadata.KnownClass(
                                                                                                                      fqName = "com.intellij.platform.workspace.storage.url.VirtualFileUrl"))),
                                                                                                                          primitive = primitiveTypeListNotNullable),
                                                                          withDefault = false),
                                                      OwnPropertyMetadata(isComputable = false,
                                                                          isKey = false,
                                                                          isOpen = false,
                                                                          name = "module",
                                                                          valueType = ValueTypeMetadata.EntityReference(connectionType = ConnectionId.ConnectionType.ONE_TO_ONE,
                                                                                                                        entityFqName = "com.intellij.platform.workspace.jps.entities.ModuleEntity",
                                                                                                                        isChild = false,
                                                                                                                        isNullable = false),
                                                                          withDefault = false)),
                                  extProperties = listOf(ExtPropertyMetadata(isComputable = false,
                                                                             isOpen = false,
                                                                             name = "scalaAddendumEntity",
                                                                             receiverFqn = "com.intellij.platform.workspace.jps.entities.ModuleEntity",
                                                                             valueType = ValueTypeMetadata.EntityReference(connectionType = ConnectionId.ConnectionType.ONE_TO_ONE,
                                                                                                                           entityFqName = "org.jetbrains.bazel.workspacemodel.entities.ScalaAddendumEntity",
                                                                                                                           isChild = true,
                                                                                                                           isNullable = true),
                                                                             withDefault = false)),
                                  isAbstract = false)

    addMetadata(typeMetadata)
  }

  override fun initializeMetadataHash() {
    addMetadataHash(typeFqn = "org.jetbrains.bazel.workspacemodel.entities.BazelJavaSourceRootEntity", metadataHash = -35974613)
    addMetadataHash(typeFqn = "org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntity", metadataHash = 1532901196)
    addMetadataHash(typeFqn = "org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeEntity",
                    metadataHash = -844721890)
    addMetadataHash(typeFqn = "org.jetbrains.bazel.workspacemodel.entities.JvmBinaryJarsEntity", metadataHash = -2137707891)
    addMetadataHash(typeFqn = "org.jetbrains.bazel.workspacemodel.entities.LibraryCompiledSourceCodeInsideJarExcludeEntity",
                    metadataHash = 873866698)
    addMetadataHash(typeFqn = "org.jetbrains.bazel.workspacemodel.entities.PackageMarkerEntity", metadataHash = -1844349399)
    addMetadataHash(typeFqn = "org.jetbrains.bazel.workspacemodel.entities.ScalaAddendumEntity", metadataHash = 950673911)
    addMetadataHash(typeFqn = "org.jetbrains.bazel.workspacemodel.entities.PackageNameId", metadataHash = -1335857953)
    addMetadataHash(typeFqn = "org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeId", metadataHash = -914279954)
    addMetadataHash(typeFqn = "com.intellij.platform.workspace.jps.entities.LibraryId", metadataHash = 1783065412)
    addMetadataHash(typeFqn = "com.intellij.platform.workspace.jps.entities.LibraryTableId", metadataHash = 1939585583)
    addMetadataHash(typeFqn = "com.intellij.platform.workspace.jps.entities.LibraryTableId\$GlobalLibraryTableId", metadataHash = 105250347)
    addMetadataHash(typeFqn = "com.intellij.platform.workspace.jps.entities.LibraryTableId\$ModuleLibraryTableId",
                    metadataHash = -1712287206)
    addMetadataHash(typeFqn = "com.intellij.platform.workspace.jps.entities.ModuleId", metadataHash = 369441961)
    addMetadataHash(typeFqn = "com.intellij.platform.workspace.jps.entities.LibraryTableId\$ProjectLibraryTableId",
                    metadataHash = 824092854)
    addMetadataHash(typeFqn = "com.intellij.platform.workspace.storage.EntitySource", metadataHash = 1674399842)
    addMetadataHash(typeFqn = "org.jetbrains.bazel.workspacemodel.entities.BazelDummyEntitySource", metadataHash = 1476524774)
    addMetadataHash(typeFqn = "org.jetbrains.bazel.workspacemodel.entities.BazelEntitySource", metadataHash = -2119790393)
    addMetadataHash(typeFqn = "org.jetbrains.bazel.workspacemodel.entities.BazelModuleEntitySource", metadataHash = 633678949)
    addMetadataHash(typeFqn = "org.jetbrains.bazel.workspacemodel.entities.BazelProjectEntitySource", metadataHash = -1409629483)
    addMetadataHash(typeFqn = "com.intellij.platform.workspace.storage.SymbolicEntityId", metadataHash = -1960707427)
  }
}
