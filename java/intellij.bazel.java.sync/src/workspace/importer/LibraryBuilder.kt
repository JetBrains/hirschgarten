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
typealias ImportedTargetPredicate = (key: WorkspaceTargetKey) -> Boolean

// RC: replaces `LibraryEntityUpdater`; goes straight from `LibraryItem` to `LibraryEntity` + `BazelLibraryExtensionEntity`,
// dropping the old `Library` wrapper
@ApiStatus.Internal
object LibraryBuilder {
  fun writeAll(
    libraryItems: List<LibraryItem>,
    virtualFileUrlManager: VirtualFileUrlManager,
    entitySource: EntitySource,
    libraryNameProvider: LibraryNameProvider,
    storage: MutableEntityStorage,
    isTargetImported: ImportedTargetPredicate,
  ): List<LibraryEntity> =
    libraryItems.map { write(it, virtualFileUrlManager, entitySource, libraryNameProvider, storage, isTargetImported) }

  fun write(
    libraryItem: LibraryItem,
    virtualFileUrlManager: VirtualFileUrlManager,
    entitySource: EntitySource,
    libraryNameProvider: LibraryNameProvider,
    storage: MutableEntityStorage,
    isTargetImported: ImportedTargetPredicate,
  ): LibraryEntity {
    val tableId = LibraryTableId.ProjectLibraryTableId
    val displayName = libraryNameProvider(libraryItem.key)
    val existing = storage.resolve(LibraryId(displayName, tableId))
    if (existing != null) {
      return existing
    }

    val sourcesRoots = libraryItem.sourceJars
      .map { it.toLibraryRoot(virtualFileUrlManager, LibraryRootTypeId.SOURCES) }
    val classesRoots = libraryItem.fullOrInterfaceJars(isTargetImported)
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

  /**
   * Prefers the interface jar only for a library that:
   * 1. Comes from a target that is not imported (limited depth case)
   * 2. Comes from a target that this workspace builds
   * 3. Source jars are present - should be true if other conditions hold, but let's ensure it is.
   *
   * Every other library keeps the full jar.
   *
   * The usage of interface jars results in smaller indices, and from the user perspective should not be easily distinguishable from the full jar.
   *
   * Noticeable differences:
   * 1. The lack of resources in the index, which could affect the resources-related inspections.
   *    Although, this seems to be an edge case, and in some cases even desired behavior - skipping a high amount of resources that are not needed for any inspections seems to be a nice saving.
   *    In case of somebody really needing resources, importing a given target is the solution here e.g., by increasing the import depth.
   *    Introducing ijars opt-out option should be the last resort here.
   * 2. The lack of bytecode analysis, which is a natural consequence of using interface jars. It does not seem to be that relevant.
   */
  private fun LibraryItem.fullOrInterfaceJars(isTargetImported: ImportedTargetPredicate): List<Path> = when {
    containsInternalJars && sourceJars.isNotEmpty() && !isTargetImported(key) -> ijars.ifEmpty { jars }
    else -> jars.ifEmpty { ijars }
  }

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
