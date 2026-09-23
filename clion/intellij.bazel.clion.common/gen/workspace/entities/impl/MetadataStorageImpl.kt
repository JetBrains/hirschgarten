package org.jetbrains.bazel.clion.workspace.entities.impl

import com.intellij.platform.workspace.storage.WorkspaceEntityInternalApi
import com.intellij.platform.workspace.storage.metadata.impl.MetadataStorageBase
import com.intellij.platform.workspace.storage.metadata.model.EntityMetadata
import com.intellij.platform.workspace.storage.metadata.model.ExtendableClassMetadata
import com.intellij.platform.workspace.storage.metadata.model.FinalClassMetadata
import com.intellij.platform.workspace.storage.metadata.model.OwnPropertyMetadata
import com.intellij.platform.workspace.storage.metadata.model.StorageTypeMetadata
import com.intellij.platform.workspace.storage.metadata.model.ValueTypeMetadata

@OptIn(WorkspaceEntityInternalApi::class)
internal object MetadataStorageImpl : MetadataStorageBase() {
  override fun initializeMetadata() {
    val primitiveTypeStringNotNullable = ValueTypeMetadata.SimpleType.PrimitiveType(isNullable = false, type = "String")
    val primitiveTypeListNotNullable = ValueTypeMetadata.SimpleType.PrimitiveType(isNullable = false, type = "List")
    val primitiveTypeStringNullable = ValueTypeMetadata.SimpleType.PrimitiveType(isNullable = true, type = "String")
    val primitiveTypeMapNotNullable = ValueTypeMetadata.SimpleType.PrimitiveType(isNullable = false, type = "Map")
    val primitiveTypeBooleanNotNullable = ValueTypeMetadata.SimpleType.PrimitiveType(isNullable = false, type = "Boolean")
    var typeMetadata: StorageTypeMetadata
    typeMetadata = EntityMetadata(
      fqName = "org.jetbrains.bazel.clion.workspace.entities.CcToolchainCompilerInfoEntity",
      entityDataFqName = "org.jetbrains.bazel.clion.workspace.entities.impl.CcToolchainCompilerInfoEntityData",
      supertypes = listOf("com.intellij.platform.workspace.storage.WorkspaceEntity"),
      properties = listOf(
        OwnPropertyMetadata(
          isComputable = false,
          isKey = false,
          isOpen = false,
          name = "entitySource",
          valueType = ValueTypeMetadata.SimpleType.CustomType(
            isNullable = false,
            typeMetadata = FinalClassMetadata.KnownClass(
              fqName = "com.intellij.platform.workspace.storage.EntitySource",
            ),
          ),
          withDefault = false,
        ),
        OwnPropertyMetadata(
          isComputable = false,
          isKey = false,
          isOpen = false,
          name = "_toolchainKey",
          valueType = ValueTypeMetadata.SimpleType.CustomType(
            isNullable = false,
            typeMetadata = FinalClassMetadata.ClassMetadata(
              fqName = "org.jetbrains.bazel.workspacemodel.entities.WorkspaceModelTargetKey",
              properties = listOf(
                OwnPropertyMetadata(
                  isComputable = false,
                  isKey = false,
                  isOpen = false,
                  name = "aspectIds",
                  valueType = ValueTypeMetadata.ParameterizedType(
                    generics = listOf(
                      primitiveTypeStringNotNullable,
                    ),
                    primitive = primitiveTypeListNotNullable,
                  ),
                  withDefault = false,
                ),
                OwnPropertyMetadata(
                  isComputable = false,
                  isKey = false,
                  isOpen = false,
                  name = "configuration",
                  valueType = primitiveTypeStringNullable,
                  withDefault = false,
                ),
                OwnPropertyMetadata(
                  isComputable = false,
                  isKey = false,
                  isOpen = false,
                  name = "label",
                  valueType = primitiveTypeStringNotNullable,
                  withDefault = false,
                ),
              ),
              supertypes = listOf(),
            ),
          ),
          withDefault = false,
        ),
        OwnPropertyMetadata(
          isComputable = false,
          isKey = false,
          isOpen = false,
          name = "cCompilerPath",
          valueType = primitiveTypeStringNotNullable,
          withDefault = false,
        ),
        OwnPropertyMetadata(
          isComputable = false,
          isKey = false,
          isOpen = false,
          name = "cCompilerKindId",
          valueType = primitiveTypeStringNotNullable,
          withDefault = false,
        ),
        OwnPropertyMetadata(
          isComputable = false,
          isKey = false,
          isOpen = false,
          name = "cppCompilerPath",
          valueType = primitiveTypeStringNotNullable,
          withDefault = false,
        ),
        OwnPropertyMetadata(
          isComputable = false,
          isKey = false,
          isOpen = false,
          name = "cppCompilerKindId",
          valueType = primitiveTypeStringNotNullable,
          withDefault = false,
        ),
        OwnPropertyMetadata(
          isComputable = false,
          isKey = false,
          isOpen = false,
          name = "cSwitches",
          valueType = ValueTypeMetadata.ParameterizedType(
            generics = listOf(
              primitiveTypeStringNotNullable,
            ),
            primitive = primitiveTypeListNotNullable,
          ),
          withDefault = false,
        ),
        OwnPropertyMetadata(
          isComputable = false,
          isKey = false,
          isOpen = false,
          name = "cppSwitches",
          valueType = ValueTypeMetadata.ParameterizedType(
            generics = listOf(
              primitiveTypeStringNotNullable,
            ),
            primitive = primitiveTypeListNotNullable,
          ),
          withDefault = false,
        ),
        OwnPropertyMetadata(
          isComputable = false,
          isKey = false,
          isOpen = false,
          name = "compilerName",
          valueType = primitiveTypeStringNotNullable,
          withDefault = false,
        ),
        OwnPropertyMetadata(
          isComputable = false,
          isKey = false,
          isOpen = false,
          name = "environment",
          valueType = ValueTypeMetadata.ParameterizedType(
            generics = listOf(
              primitiveTypeStringNotNullable,
              primitiveTypeStringNotNullable,
            ),
            primitive = primitiveTypeMapNotNullable,
          ),
          withDefault = false,
        ),
        OwnPropertyMetadata(
          isComputable = false,
          isKey = false,
          isOpen = false,
          name = "builtinIncludes",
          valueType = ValueTypeMetadata.ParameterizedType(
            generics = listOf(
              ValueTypeMetadata.SimpleType.CustomType(
                isNullable = false,
                typeMetadata = ExtendableClassMetadata.AbstractClassMetadata(
                  fqName = "org.jetbrains.bsp.protocol.OutputLocation",
                  subclasses = listOf(
                    FinalClassMetadata.ClassMetadata(
                      fqName = "org.jetbrains.bsp.protocol.OutputLocation\$Host",
                      properties = listOf(
                        OwnPropertyMetadata(
                          isComputable = false,
                          isKey = false,
                          isOpen = false,
                          name = "absolutePath",
                          valueType = primitiveTypeStringNotNullable,
                          withDefault = false,
                        ),
                      ),
                      supertypes = listOf(
                        "org.jetbrains.bsp.protocol.OutputLocation",
                      ),
                    ),
                    FinalClassMetadata.ClassMetadata(
                      fqName = "org.jetbrains.bsp.protocol.OutputLocation\$Output",
                      properties = listOf(
                        OwnPropertyMetadata(
                          isComputable = false,
                          isKey = false,
                          isOpen = false,
                          name = "relativePath",
                          valueType = primitiveTypeStringNotNullable,
                          withDefault = false,
                        ),
                        OwnPropertyMetadata(
                          isComputable = false,
                          isKey = false,
                          isOpen = false,
                          name = "root",
                          valueType = ValueTypeMetadata.SimpleType.CustomType(
                            isNullable = false,
                            typeMetadata = FinalClassMetadata.ClassMetadata(
                              fqName = "org.jetbrains.bsp.protocol.OutputRoot",
                              properties = listOf(
                                OwnPropertyMetadata(
                                  isComputable = false,
                                  isKey = false,
                                  isOpen = false,
                                  name = "path",
                                  valueType = primitiveTypeStringNotNullable,
                                  withDefault = false,
                                ),
                                OwnPropertyMetadata(
                                  isComputable = false,
                                  isKey = false,
                                  isOpen = false,
                                  name = "segments",
                                  valueType = ValueTypeMetadata.ParameterizedType(
                                    generics = listOf(
                                      primitiveTypeStringNotNullable,
                                    ),
                                    primitive = primitiveTypeListNotNullable,
                                  ),
                                  withDefault = false,
                                ),
                              ),
                              supertypes = listOf(),
                            ),
                          ),
                          withDefault = false,
                        ),
                      ),
                      supertypes = listOf(
                        "org.jetbrains.bsp.protocol.OutputLocation",
                      ),
                    ),
                    FinalClassMetadata.ClassMetadata(
                      fqName = "org.jetbrains.bsp.protocol.OutputLocation\$External",
                      properties = listOf(
                        OwnPropertyMetadata(
                          isComputable = false,
                          isKey = false,
                          isOpen = false,
                          name = "relativePath",
                          valueType = primitiveTypeStringNotNullable,
                          withDefault = false,
                        ),
                        OwnPropertyMetadata(
                          isComputable = false,
                          isKey = false,
                          isOpen = false,
                          name = "repoName",
                          valueType = primitiveTypeStringNotNullable,
                          withDefault = false,
                        ),
                        OwnPropertyMetadata(
                          isComputable = false,
                          isKey = false,
                          isOpen = false,
                          name = "siblingLayout",
                          valueType = primitiveTypeBooleanNotNullable,
                          withDefault = false,
                        ),
                      ),
                      supertypes = listOf(
                        "org.jetbrains.bsp.protocol.OutputLocation",
                      ),
                    ),
                    FinalClassMetadata.ClassMetadata(
                      fqName = "org.jetbrains.bsp.protocol.OutputLocation\$Workspace",
                      properties = listOf(
                        OwnPropertyMetadata(
                          isComputable = false,
                          isKey = false,
                          isOpen = false,
                          name = "relativePath",
                          valueType = primitiveTypeStringNotNullable,
                          withDefault = false,
                        ),
                      ),
                      supertypes = listOf(
                        "org.jetbrains.bsp.protocol.OutputLocation",
                      ),
                    ),
                  ),
                  supertypes = listOf(),
                ),
              ),
            ),
            primitive = primitiveTypeListNotNullable,
          ),
          withDefault = false,
        ),
        OwnPropertyMetadata(
          isComputable = false,
          isKey = false,
          isOpen = false,
          name = "sysroot",
          valueType = ValueTypeMetadata.SimpleType.CustomType(
            isNullable = true,
            typeMetadata = ExtendableClassMetadata.AbstractClassMetadata(
              fqName = "org.jetbrains.bsp.protocol.OutputLocation",
              subclasses = listOf(
                FinalClassMetadata.ClassMetadata(
                  fqName = "org.jetbrains.bsp.protocol.OutputLocation\$Host",
                  properties = listOf(
                    OwnPropertyMetadata(
                      isComputable = false,
                      isKey = false,
                      isOpen = false,
                      name = "absolutePath",
                      valueType = primitiveTypeStringNotNullable,
                      withDefault = false,
                    ),
                  ),
                  supertypes = listOf(
                    "org.jetbrains.bsp.protocol.OutputLocation",
                  ),
                ),
                FinalClassMetadata.ClassMetadata(
                  fqName = "org.jetbrains.bsp.protocol.OutputLocation\$Output",
                  properties = listOf(
                    OwnPropertyMetadata(
                      isComputable = false,
                      isKey = false,
                      isOpen = false,
                      name = "relativePath",
                      valueType = primitiveTypeStringNotNullable,
                      withDefault = false,
                    ),
                    OwnPropertyMetadata(
                      isComputable = false,
                      isKey = false,
                      isOpen = false,
                      name = "root",
                      valueType = ValueTypeMetadata.SimpleType.CustomType(
                        isNullable = false,
                        typeMetadata = FinalClassMetadata.ClassMetadata(
                          fqName = "org.jetbrains.bsp.protocol.OutputRoot",
                          properties = listOf(
                            OwnPropertyMetadata(
                              isComputable = false,
                              isKey = false,
                              isOpen = false,
                              name = "path",
                              valueType = primitiveTypeStringNotNullable,
                              withDefault = false,
                            ),
                            OwnPropertyMetadata(
                              isComputable = false,
                              isKey = false,
                              isOpen = false,
                              name = "segments",
                              valueType = ValueTypeMetadata.ParameterizedType(
                                generics = listOf(
                                  primitiveTypeStringNotNullable,
                                ),
                                primitive = primitiveTypeListNotNullable,
                              ),
                              withDefault = false,
                            ),
                          ),
                          supertypes = listOf(),
                        ),
                      ),
                      withDefault = false,
                    ),
                  ),
                  supertypes = listOf(
                    "org.jetbrains.bsp.protocol.OutputLocation",
                  ),
                ),
                FinalClassMetadata.ClassMetadata(
                  fqName = "org.jetbrains.bsp.protocol.OutputLocation\$External",
                  properties = listOf(
                    OwnPropertyMetadata(
                      isComputable = false,
                      isKey = false,
                      isOpen = false,
                      name = "relativePath",
                      valueType = primitiveTypeStringNotNullable,
                      withDefault = false,
                    ),
                    OwnPropertyMetadata(
                      isComputable = false,
                      isKey = false,
                      isOpen = false,
                      name = "repoName",
                      valueType = primitiveTypeStringNotNullable,
                      withDefault = false,
                    ),
                    OwnPropertyMetadata(
                      isComputable = false,
                      isKey = false,
                      isOpen = false,
                      name = "siblingLayout",
                      valueType = primitiveTypeBooleanNotNullable,
                      withDefault = false,
                    ),
                  ),
                  supertypes = listOf(
                    "org.jetbrains.bsp.protocol.OutputLocation",
                  ),
                ),
                FinalClassMetadata.ClassMetadata(
                  fqName = "org.jetbrains.bsp.protocol.OutputLocation\$Workspace",
                  properties = listOf(
                    OwnPropertyMetadata(
                      isComputable = false,
                      isKey = false,
                      isOpen = false,
                      name = "relativePath",
                      valueType = primitiveTypeStringNotNullable,
                      withDefault = false,
                    ),
                  ),
                  supertypes = listOf(
                    "org.jetbrains.bsp.protocol.OutputLocation",
                  ),
                ),
              ),
              supertypes = listOf(),
            ),
          ),
          withDefault = false,
        ),
      ),
      extProperties = listOf(),
      isAbstract = false,
    )
    addMetadata(typeMetadata)
  }

  override fun initializeMetadataHash() {
    addMetadataHash(typeFqn = "org.jetbrains.bazel.clion.workspace.entities.CcToolchainCompilerInfoEntity", metadataHash = 652374286)
    addMetadataHash(typeFqn = "org.jetbrains.bazel.workspacemodel.entities.WorkspaceModelTargetKey", metadataHash = -2063063629)
    addMetadataHash(typeFqn = "org.jetbrains.bsp.protocol.OutputLocation", metadataHash = 1431381847)
    addMetadataHash(typeFqn = "org.jetbrains.bsp.protocol.OutputLocation\$External", metadataHash = -449464191)
    addMetadataHash(typeFqn = "org.jetbrains.bsp.protocol.OutputLocation\$Host", metadataHash = -382012921)
    addMetadataHash(typeFqn = "org.jetbrains.bsp.protocol.OutputLocation\$Output", metadataHash = 637434728)
    addMetadataHash(typeFqn = "org.jetbrains.bsp.protocol.OutputRoot", metadataHash = 823878895)
    addMetadataHash(typeFqn = "org.jetbrains.bsp.protocol.OutputLocation\$Workspace", metadataHash = -1979395463)
  }
}
