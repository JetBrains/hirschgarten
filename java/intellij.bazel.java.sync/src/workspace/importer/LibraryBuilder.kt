package org.jetbrains.bazel.workspace.importer

import com.intellij.configurationStore.serialize
import com.intellij.externalSystem.ImportedLibraryProperties
import com.intellij.externalSystem.ImportedLibraryType
import com.intellij.java.library.MavenCoordinates
import com.intellij.openapi.util.JDOMUtil
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.LibraryId
import com.intellij.platform.workspace.jps.entities.LibraryPropertiesEntity
import com.intellij.platform.workspace.jps.entities.LibraryRoot
import com.intellij.platform.workspace.jps.entities.LibraryRootTypeId
import com.intellij.platform.workspace.jps.entities.LibraryTableId
import com.intellij.platform.workspace.jps.entities.LibraryTypeId
import com.intellij.platform.workspace.jps.entities.libraryProperties
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.workspacemodel.entities.BazelLibraryExtensionEntity
import org.jetbrains.bazel.workspacemodel.entities.WorkspaceModelTargetKey
import org.jetbrains.bazel.workspacemodel.entities.bazelLibraryExtension
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.jps.model.serialization.library.JpsLibraryTableSerializer
import java.nio.file.Path

typealias LibraryNameProvider = (key: WorkspaceTargetKey) -> String

// RC: replaces `LibraryEntityUpdater`; goes straight from `LibraryItem` to `LibraryEntity` + `BazelLibraryExtensionEntity`,
// dropping the old `Library` wrapper
@ApiStatus.Internal
object LibraryBuilder {
  fun writeAll(
    libraryItems: List<LibraryItem>,
    importIjars: Boolean,
    virtualFileUrlManager: VirtualFileUrlManager,
    entitySource: EntitySource,
    libraryNameProvider: LibraryNameProvider,
    storage: MutableEntityStorage,
  ): List<LibraryEntity> =
    libraryItems.map { write(it, importIjars, virtualFileUrlManager, entitySource, libraryNameProvider, storage) }

  fun write(
    libraryItem: LibraryItem,
    importIjars: Boolean,
    virtualFileUrlManager: VirtualFileUrlManager,
    entitySource: EntitySource,
    libraryNameProvider: LibraryNameProvider,
    storage: MutableEntityStorage,
  ): LibraryEntity {
    val tableId = LibraryTableId.ProjectLibraryTableId
    val displayName = libraryNameProvider(libraryItem.key)
    val existing = storage.resolve(LibraryId(displayName, tableId))
    if (existing != null) {
      return existing
    }

    val sourcesRoots = libraryItem.sourceJars
      .map { it.toLibraryRoot(virtualFileUrlManager, LibraryRootTypeId.SOURCES) }
    val classesRoots = libraryItem.classesOrIJars(importIjars)
      .map { it.toLibraryRoot(virtualFileUrlManager, LibraryRootTypeId.COMPILED) }

    val libraryEntity =
      LibraryEntity(
        name = displayName,
        tableId = tableId,
        roots = sourcesRoots + classesRoots,
        entitySource = entitySource,
      ) {
        this.excludedRoots = arrayListOf()
        this.typeId = LibraryTypeId(ImportedLibraryType.IMPORTED_LIBRARY_KIND.kindId)
        this.libraryProperties =
          LibraryPropertiesEntity(entitySource) {
            propertiesXmlTag = libraryItem.mavenCoordinates?.toLibraryPropertiesXml()
          }
        this.bazelLibraryExtension = BazelLibraryExtensionEntity(
          entitySource = entitySource,
          _targetKey = WorkspaceModelTargetKey.of(libraryItem.key),
          isSynthetic = libraryItem.key.label.isSynthetic,
        )
      }

    return storage.addEntity(libraryEntity)
  }

  private fun LibraryItem.classesOrIJars(importIjars: Boolean): List<Path> =
    if (importIjars) ijars.ifEmpty { jars } else jars.ifEmpty { ijars }

  private fun Path.toLibraryRoot(virtualFileUrlManager: VirtualFileUrlManager, type: LibraryRootTypeId): LibraryRoot =
    LibraryRoot(
      url = toJarUrlString().toResolvedVirtualFileUrl(virtualFileUrlManager),
      type = type,
    )

  private fun org.jetbrains.bsp.protocol.MavenCoordinates.toLibraryPropertiesXml(): String? {
    val element =
      serialize(
        ImportedLibraryProperties(
          MavenCoordinates(groupId, artifactId, version),
        ).state,
      ) ?: return null
    element.name = JpsLibraryTableSerializer.PROPERTIES_TAG
    return JDOMUtil.writeElement(element)
  }
}
