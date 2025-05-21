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
internal object MetadataStorageImpl: MetadataStorageBase() {
    override fun initializeMetadata() {
        val primitiveTypeStringNotNullable = ValueTypeMetadata.SimpleType.PrimitiveType(isNullable = false, type = "String")
        val primitiveTypeIntNotNullable = ValueTypeMetadata.SimpleType.PrimitiveType(isNullable = false, type = "Int")
        val primitiveTypeMapNotNullable = ValueTypeMetadata.SimpleType.PrimitiveType(isNullable = false, type = "Map")
        val primitiveTypeListNotNullable = ValueTypeMetadata.SimpleType.PrimitiveType(isNullable = false, type = "List")
        val primitiveTypeStringNullable = ValueTypeMetadata.SimpleType.PrimitiveType(isNullable = true, type = "String")
        val primitiveTypeSetNotNullable = ValueTypeMetadata.SimpleType.PrimitiveType(isNullable = false, type = "Set")
        
        var typeMetadata: StorageTypeMetadata
        
        typeMetadata = FinalClassMetadata.ClassMetadata(fqName = "org.jetbrains.bazel.workspacemodel.entities.BazelModuleEntitySource", properties = listOf(OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "moduleName", valueType = primitiveTypeStringNotNullable, withDefault = false),
OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "virtualFileUrl", valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = true, typeMetadata = FinalClassMetadata.KnownClass(fqName = "com.intellij.platform.workspace.storage.url.VirtualFileUrl")), withDefault = false)), supertypes = listOf("com.intellij.platform.workspace.storage.EntitySource",
"org.jetbrains.bazel.workspacemodel.entities.BazelEntitySource"))
        
        addMetadata(typeMetadata)
        
        typeMetadata = FinalClassMetadata.ObjectMetadata(fqName = "org.jetbrains.bazel.workspacemodel.entities.BazelDummyEntitySource", properties = listOf(OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "virtualFileUrl", valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = true, typeMetadata = FinalClassMetadata.KnownClass(fqName = "com.intellij.platform.workspace.storage.url.VirtualFileUrl")), withDefault = false)), supertypes = listOf("com.intellij.platform.workspace.storage.EntitySource",
"org.jetbrains.bazel.workspacemodel.entities.BazelEntitySource"))
        
        addMetadata(typeMetadata)
        
        typeMetadata = FinalClassMetadata.ObjectMetadata(fqName = "org.jetbrains.bazel.workspacemodel.entities.BazelProjectEntitySource", properties = listOf(OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "virtualFileUrl", valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = true, typeMetadata = FinalClassMetadata.KnownClass(fqName = "com.intellij.platform.workspace.storage.url.VirtualFileUrl")), withDefault = false)), supertypes = listOf("com.intellij.platform.workspace.storage.EntitySource",
"org.jetbrains.bazel.workspacemodel.entities.BazelEntitySource"))
        
        addMetadata(typeMetadata)
        
        typeMetadata = FinalClassMetadata.ClassMetadata(fqName = "org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeId", properties = listOf(OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "id", valueType = primitiveTypeIntNotNullable, withDefault = false),
OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "presentableName", valueType = primitiveTypeStringNotNullable, withDefault = false)), supertypes = listOf("com.intellij.platform.workspace.storage.SymbolicEntityId"))
        
        addMetadata(typeMetadata)
        
        typeMetadata = FinalClassMetadata.ClassMetadata(fqName = "org.jetbrains.bazel.workspacemodel.entities.PackageNameId", properties = listOf(OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "packageName", valueType = primitiveTypeStringNotNullable, withDefault = false),
OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "presentableName", valueType = primitiveTypeStringNotNullable, withDefault = false)), supertypes = listOf("com.intellij.platform.workspace.storage.SymbolicEntityId"))
        
        addMetadata(typeMetadata)
        
        typeMetadata = EntityMetadata(fqName = "org.jetbrains.bazel.workspacemodel.entities.AndroidAddendumEntity", entityDataFqName = "org.jetbrains.bazel.workspacemodel.entities.impl.AndroidAddendumEntityData", supertypes = listOf("com.intellij.platform.workspace.storage.WorkspaceEntity"), properties = listOf(OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "entitySource", valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = false, typeMetadata = FinalClassMetadata.KnownClass(fqName = "com.intellij.platform.workspace.storage.EntitySource")), withDefault = false),
OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "androidSdkName", valueType = primitiveTypeStringNotNullable, withDefault = false),
OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "androidTargetType", valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = false, typeMetadata = FinalClassMetadata.EnumClassMetadata(fqName = "org.jetbrains.bazel.workspacemodel.entities.AndroidTargetType", properties = arrayListOf(), supertypes = listOf("java.io.Serializable",
"kotlin.Comparable",
"kotlin.Enum"), values = listOf("APP",
"LIBRARY",
"TEST"))), withDefault = false),
OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "manifest", valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = true, typeMetadata = FinalClassMetadata.KnownClass(fqName = "com.intellij.platform.workspace.storage.url.VirtualFileUrl")), withDefault = false),
OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "manifestOverrides", valueType = ValueTypeMetadata.ParameterizedType(generics = listOf(primitiveTypeStringNotNullable,
primitiveTypeStringNotNullable), primitive = primitiveTypeMapNotNullable), withDefault = false),
OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "resourceDirectories", valueType = ValueTypeMetadata.ParameterizedType(generics = listOf(ValueTypeMetadata.SimpleType.CustomType(isNullable = false, typeMetadata = FinalClassMetadata.KnownClass(fqName = "com.intellij.platform.workspace.storage.url.VirtualFileUrl"))), primitive = primitiveTypeListNotNullable), withDefault = false),
OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "resourceJavaPackage", valueType = primitiveTypeStringNullable, withDefault = false),
OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "assetsDirectories", valueType = ValueTypeMetadata.ParameterizedType(generics = listOf(ValueTypeMetadata.SimpleType.CustomType(isNullable = false, typeMetadata = FinalClassMetadata.KnownClass(fqName = "com.intellij.platform.workspace.storage.url.VirtualFileUrl"))), primitive = primitiveTypeListNotNullable), withDefault = false),
OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "apk", valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = true, typeMetadata = FinalClassMetadata.KnownClass(fqName = "com.intellij.platform.workspace.storage.url.VirtualFileUrl")), withDefault = false),
OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "module", valueType = ValueTypeMetadata.EntityReference(connectionType = ConnectionId.ConnectionType.ONE_TO_ONE, entityFqName = "com.intellij.platform.workspace.jps.entities.ModuleEntity", isChild = false, isNullable = false), withDefault = false)), extProperties = listOf(ExtPropertyMetadata(isComputable = false, isOpen = false, name = "androidAddendumEntity", receiverFqn = "com.intellij.platform.workspace.jps.entities.ModuleEntity", valueType = ValueTypeMetadata.EntityReference(connectionType = ConnectionId.ConnectionType.ONE_TO_ONE, entityFqName = "org.jetbrains.bazel.workspacemodel.entities.AndroidAddendumEntity", isChild = true, isNullable = true), withDefault = false)), isAbstract = false)
        
        addMetadata(typeMetadata)
        
        typeMetadata = EntityMetadata(fqName = "org.jetbrains.bazel.workspacemodel.entities.BazelJavaSourceRootEntity", entityDataFqName = "org.jetbrains.bazel.workspacemodel.entities.impl.BazelJavaSourceRootEntityData", supertypes = listOf("com.intellij.platform.workspace.storage.WorkspaceEntity"), properties = listOf(OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "entitySource", valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = false, typeMetadata = FinalClassMetadata.KnownClass(fqName = "com.intellij.platform.workspace.storage.EntitySource")), withDefault = false),
OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "packageNameId", valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = false, typeMetadata = FinalClassMetadata.ClassMetadata(fqName = "org.jetbrains.bazel.workspacemodel.entities.PackageNameId", properties = listOf(OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "packageName", valueType = primitiveTypeStringNotNullable, withDefault = false),
OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "presentableName", valueType = primitiveTypeStringNotNullable, withDefault = false)), supertypes = listOf("com.intellij.platform.workspace.storage.SymbolicEntityId"))), withDefault = false),
OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "sourceRoots", valueType = ValueTypeMetadata.ParameterizedType(generics = listOf(ValueTypeMetadata.SimpleType.CustomType(isNullable = false, typeMetadata = FinalClassMetadata.KnownClass(fqName = "com.intellij.platform.workspace.storage.url.VirtualFileUrl"))), primitive = primitiveTypeListNotNullable), withDefault = false)), extProperties = arrayListOf(), isAbstract = false)
        
        addMetadata(typeMetadata)
        
        typeMetadata = EntityMetadata(fqName = "org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntity", entityDataFqName = "org.jetbrains.bazel.workspacemodel.entities.impl.BazelProjectDirectoriesEntityData", supertypes = listOf("com.intellij.platform.workspace.storage.WorkspaceEntity"), properties = listOf(OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "entitySource", valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = false, typeMetadata = FinalClassMetadata.KnownClass(fqName = "com.intellij.platform.workspace.storage.EntitySource")), withDefault = false),
OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "projectRoot", valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = false, typeMetadata = FinalClassMetadata.KnownClass(fqName = "com.intellij.platform.workspace.storage.url.VirtualFileUrl")), withDefault = false),
OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "includedRoots", valueType = ValueTypeMetadata.ParameterizedType(generics = listOf(ValueTypeMetadata.SimpleType.CustomType(isNullable = false, typeMetadata = FinalClassMetadata.KnownClass(fqName = "com.intellij.platform.workspace.storage.url.VirtualFileUrl"))), primitive = primitiveTypeListNotNullable), withDefault = false),
OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "excludedRoots", valueType = ValueTypeMetadata.ParameterizedType(generics = listOf(ValueTypeMetadata.SimpleType.CustomType(isNullable = false, typeMetadata = FinalClassMetadata.KnownClass(fqName = "com.intellij.platform.workspace.storage.url.VirtualFileUrl"))), primitive = primitiveTypeListNotNullable), withDefault = false)), extProperties = arrayListOf(), isAbstract = false)
        
        addMetadata(typeMetadata)
        
        typeMetadata = EntityMetadata(fqName = "org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeEntity", entityDataFqName = "org.jetbrains.bazel.workspacemodel.entities.impl.CompiledSourceCodeInsideJarExcludeEntityData", supertypes = listOf("com.intellij.platform.workspace.storage.WorkspaceEntity",
"com.intellij.platform.workspace.storage.WorkspaceEntityWithSymbolicId"), properties = listOf(OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "entitySource", valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = false, typeMetadata = FinalClassMetadata.KnownClass(fqName = "com.intellij.platform.workspace.storage.EntitySource")), withDefault = false),
OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "relativePathsInsideJarToExclude", valueType = ValueTypeMetadata.ParameterizedType(generics = listOf(primitiveTypeStringNotNullable), primitive = primitiveTypeSetNotNullable), withDefault = false),
OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "excludeId", valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = false, typeMetadata = FinalClassMetadata.ClassMetadata(fqName = "org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeId", properties = listOf(OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "id", valueType = primitiveTypeIntNotNullable, withDefault = false),
OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "presentableName", valueType = primitiveTypeStringNotNullable, withDefault = false)), supertypes = listOf("com.intellij.platform.workspace.storage.SymbolicEntityId"))), withDefault = false),
OwnPropertyMetadata(isComputable = true, isKey = false, isOpen = false, name = "symbolicId", valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = false, typeMetadata = FinalClassMetadata.ClassMetadata(fqName = "org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeId", properties = listOf(OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "id", valueType = primitiveTypeIntNotNullable, withDefault = false),
OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "presentableName", valueType = primitiveTypeStringNotNullable, withDefault = false)), supertypes = listOf("com.intellij.platform.workspace.storage.SymbolicEntityId"))), withDefault = false)), extProperties = arrayListOf(), isAbstract = false)
        
        addMetadata(typeMetadata)
        
        typeMetadata = EntityMetadata(fqName = "org.jetbrains.bazel.workspacemodel.entities.JvmBinaryJarsEntity", entityDataFqName = "org.jetbrains.bazel.workspacemodel.entities.impl.JvmBinaryJarsEntityData", supertypes = listOf("com.intellij.platform.workspace.storage.WorkspaceEntity"), properties = listOf(OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "entitySource", valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = false, typeMetadata = FinalClassMetadata.KnownClass(fqName = "com.intellij.platform.workspace.storage.EntitySource")), withDefault = false),
OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "jars", valueType = ValueTypeMetadata.ParameterizedType(generics = listOf(ValueTypeMetadata.SimpleType.CustomType(isNullable = false, typeMetadata = FinalClassMetadata.KnownClass(fqName = "com.intellij.platform.workspace.storage.url.VirtualFileUrl"))), primitive = primitiveTypeListNotNullable), withDefault = false),
OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "module", valueType = ValueTypeMetadata.EntityReference(connectionType = ConnectionId.ConnectionType.ONE_TO_ONE, entityFqName = "com.intellij.platform.workspace.jps.entities.ModuleEntity", isChild = false, isNullable = false), withDefault = false)), extProperties = listOf(ExtPropertyMetadata(isComputable = false, isOpen = false, name = "jvmBinaryJarsEntity", receiverFqn = "com.intellij.platform.workspace.jps.entities.ModuleEntity", valueType = ValueTypeMetadata.EntityReference(connectionType = ConnectionId.ConnectionType.ONE_TO_ONE, entityFqName = "org.jetbrains.bazel.workspacemodel.entities.JvmBinaryJarsEntity", isChild = true, isNullable = true), withDefault = false)), isAbstract = false)
        
        addMetadata(typeMetadata)
        
        typeMetadata = EntityMetadata(fqName = "org.jetbrains.bazel.workspacemodel.entities.LibraryCompiledSourceCodeInsideJarExcludeEntity", entityDataFqName = "org.jetbrains.bazel.workspacemodel.entities.impl.LibraryCompiledSourceCodeInsideJarExcludeEntityData", supertypes = listOf("com.intellij.platform.workspace.storage.WorkspaceEntity"), properties = listOf(OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "entitySource", valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = false, typeMetadata = FinalClassMetadata.KnownClass(fqName = "com.intellij.platform.workspace.storage.EntitySource")), withDefault = false),
OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "libraryId", valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = false, typeMetadata = FinalClassMetadata.ClassMetadata(fqName = "com.intellij.platform.workspace.jps.entities.LibraryId", properties = listOf(OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "codeCache", valueType = primitiveTypeIntNotNullable, withDefault = false),
OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "name", valueType = primitiveTypeStringNotNullable, withDefault = false),
OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "presentableName", valueType = primitiveTypeStringNotNullable, withDefault = false),
OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "tableId", valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = false, typeMetadata = ExtendableClassMetadata.AbstractClassMetadata(fqName = "com.intellij.platform.workspace.jps.entities.LibraryTableId", subclasses = listOf(FinalClassMetadata.ObjectMetadata(fqName = "com.intellij.platform.workspace.jps.entities.LibraryTableId\$ProjectLibraryTableId", properties = listOf(OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "level", valueType = primitiveTypeStringNotNullable, withDefault = false)), supertypes = listOf("com.intellij.platform.workspace.jps.entities.LibraryTableId",
"java.io.Serializable")),
FinalClassMetadata.ClassMetadata(fqName = "com.intellij.platform.workspace.jps.entities.LibraryTableId\$GlobalLibraryTableId", properties = listOf(OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "level", valueType = primitiveTypeStringNotNullable, withDefault = false)), supertypes = listOf("com.intellij.platform.workspace.jps.entities.LibraryTableId",
"java.io.Serializable")),
FinalClassMetadata.ClassMetadata(fqName = "com.intellij.platform.workspace.jps.entities.LibraryTableId\$ModuleLibraryTableId", properties = listOf(OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "level", valueType = primitiveTypeStringNotNullable, withDefault = false),
OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "moduleId", valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = false, typeMetadata = FinalClassMetadata.ClassMetadata(fqName = "com.intellij.platform.workspace.jps.entities.ModuleId", properties = listOf(OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "name", valueType = primitiveTypeStringNotNullable, withDefault = false),
OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "presentableName", valueType = primitiveTypeStringNotNullable, withDefault = false)), supertypes = listOf("com.intellij.platform.workspace.storage.SymbolicEntityId"))), withDefault = false)), supertypes = listOf("com.intellij.platform.workspace.jps.entities.LibraryTableId",
"java.io.Serializable"))), supertypes = listOf("java.io.Serializable"))), withDefault = false)), supertypes = listOf("com.intellij.platform.workspace.storage.SymbolicEntityId"))), withDefault = false),
OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "compiledSourceCodeInsideJarExcludeId", valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = false, typeMetadata = FinalClassMetadata.ClassMetadata(fqName = "org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeId", properties = listOf(OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "id", valueType = primitiveTypeIntNotNullable, withDefault = false),
OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "presentableName", valueType = primitiveTypeStringNotNullable, withDefault = false)), supertypes = listOf("com.intellij.platform.workspace.storage.SymbolicEntityId"))), withDefault = false)), extProperties = arrayListOf(), isAbstract = false)
        
        addMetadata(typeMetadata)
        
        typeMetadata = EntityMetadata(fqName = "org.jetbrains.bazel.workspacemodel.entities.PackageMarkerEntity", entityDataFqName = "org.jetbrains.bazel.workspacemodel.entities.impl.PackageMarkerEntityData", supertypes = listOf("com.intellij.platform.workspace.storage.WorkspaceEntity"), properties = listOf(OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "entitySource", valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = false, typeMetadata = FinalClassMetadata.KnownClass(fqName = "com.intellij.platform.workspace.storage.EntitySource")), withDefault = false),
OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "root", valueType = ValueTypeMetadata.SimpleType.CustomType(isNullable = false, typeMetadata = FinalClassMetadata.KnownClass(fqName = "com.intellij.platform.workspace.storage.url.VirtualFileUrl")), withDefault = false),
OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "packagePrefix", valueType = primitiveTypeStringNotNullable, withDefault = false),
OwnPropertyMetadata(isComputable = false, isKey = false, isOpen = false, name = "module", valueType = ValueTypeMetadata.EntityReference(connectionType = ConnectionId.ConnectionType.ONE_TO_MANY, entityFqName = "com.intellij.platform.workspace.jps.entities.ModuleEntity", isChild = false, isNullable = false), withDefault = false)), extProperties = listOf(ExtPropertyMetadata(isComputable = false, isOpen = false, name = "packageMarkerEntities", receiverFqn = "com.intellij.platform.workspace.jps.entities.ModuleEntity", valueType = ValueTypeMetadata.EntityReference(connectionType = ConnectionId.ConnectionType.ONE_TO_MANY, entityFqName = "org.jetbrains.bazel.workspacemodel.entities.PackageMarkerEntity", isChild = true, isNullable = false), withDefault = false)), isAbstract = false)
        
        addMetadata(typeMetadata)
    }

    override fun initializeMetadataHash() {
        addMetadataHash(typeFqn = "org.jetbrains.bazel.workspacemodel.entities.AndroidAddendumEntity", metadataHash = 280345007)
        addMetadataHash(typeFqn = "org.jetbrains.bazel.workspacemodel.entities.BazelJavaSourceRootEntity", metadataHash = 917847693)
        addMetadataHash(typeFqn = "org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntity", metadataHash = -904066315)
        addMetadataHash(typeFqn = "org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeEntity", metadataHash = -1608740419)
        addMetadataHash(typeFqn = "org.jetbrains.bazel.workspacemodel.entities.JvmBinaryJarsEntity", metadataHash = 867229151)
        addMetadataHash(typeFqn = "org.jetbrains.bazel.workspacemodel.entities.LibraryCompiledSourceCodeInsideJarExcludeEntity", metadataHash = -1407175814)
        addMetadataHash(typeFqn = "org.jetbrains.bazel.workspacemodel.entities.PackageMarkerEntity", metadataHash = 76900813)
        addMetadataHash(typeFqn = "org.jetbrains.bazel.workspacemodel.entities.AndroidTargetType", metadataHash = -1482587061)
        addMetadataHash(typeFqn = "org.jetbrains.bazel.workspacemodel.entities.PackageNameId", metadataHash = -1899896495)
        addMetadataHash(typeFqn = "org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeId", metadataHash = -1715880414)
        addMetadataHash(typeFqn = "com.intellij.platform.workspace.jps.entities.LibraryId", metadataHash = 1192700660)
        addMetadataHash(typeFqn = "com.intellij.platform.workspace.jps.entities.LibraryTableId", metadataHash = -219154451)
        addMetadataHash(typeFqn = "com.intellij.platform.workspace.jps.entities.LibraryTableId\$GlobalLibraryTableId", metadataHash = 1911253865)
        addMetadataHash(typeFqn = "com.intellij.platform.workspace.jps.entities.LibraryTableId\$ModuleLibraryTableId", metadataHash = 777479370)
        addMetadataHash(typeFqn = "com.intellij.platform.workspace.jps.entities.ModuleId", metadataHash = -575206713)
        addMetadataHash(typeFqn = "com.intellij.platform.workspace.jps.entities.LibraryTableId\$ProjectLibraryTableId", metadataHash = -1574432194)
        addMetadataHash(typeFqn = "com.intellij.platform.workspace.storage.EntitySource", metadataHash = -610405892)
        addMetadataHash(typeFqn = "org.jetbrains.bazel.workspacemodel.entities.BazelDummyEntitySource", metadataHash = -1192262342)
        addMetadataHash(typeFqn = "org.jetbrains.bazel.workspacemodel.entities.BazelEntitySource", metadataHash = -1185244447)
        addMetadataHash(typeFqn = "org.jetbrains.bazel.workspacemodel.entities.BazelModuleEntitySource", metadataHash = -1421628915)
        addMetadataHash(typeFqn = "org.jetbrains.bazel.workspacemodel.entities.BazelProjectEntitySource", metadataHash = -468731093)
        addMetadataHash(typeFqn = "com.intellij.platform.workspace.storage.SymbolicEntityId", metadataHash = 761750119)
    }

}
