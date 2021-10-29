package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.workspaceModel.storage.bridgeEntities.LibraryEntity
import com.intellij.workspaceModel.storage.bridgeEntities.LibraryRoot
import com.intellij.workspaceModel.storage.bridgeEntities.LibraryRootTypeId
import com.intellij.workspaceModel.storage.bridgeEntities.LibraryTableId
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleId
import org.jetbrains.workspace.model.matchers.entries.ExpectedLibraryEntity
import org.jetbrains.workspace.model.matchers.entries.shouldBeEqual
import org.jetbrains.workspace.model.matchers.entries.shouldContainExactlyInAnyOrder
import org.jetbrains.workspace.model.test.framework.WorkspaceModelWithParentJavaModuleBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("LibraryEntityUpdater.addEntity(entityToAdd, parentModuleEntity) tests")
internal class LibraryEntityUpdaterTest : WorkspaceModelWithParentJavaModuleBaseTest() {

  private lateinit var libraryEntityUpdater: LibraryEntityUpdater

  @BeforeEach
  override fun beforeEach() {
    // given
    super.beforeEach()

    val workspaceModelEntityUpdaterConfig =
      WorkspaceModelEntityUpdaterConfig(workspaceModel, virtualFileUrlManager, projectConfigSource)
    libraryEntityUpdater = LibraryEntityUpdater(workspaceModelEntityUpdaterConfig)
  }

  @Test
  fun `should add one library to the workspace model`() {
    // given
    val library = Library(
      displayName = "file:///dependency/test/1.0.0/test-1.0.0-sources.jar",
      sourcesJar = "jar:///dependency/test/1.0.0/test-1.0.0-sources.jar!/",
      classesJar = "jar:///dependency/test/1.0.0/test-1.0.0.jar!/",
    )

    // when
    val returnedLibraryEntity = runTestWriteAction {
      libraryEntityUpdater.addEntity(library, parentModuleEntity)
    }

    // then
    val expectedLibrarySourcesRoot = LibraryRoot(
      url = virtualFileUrlManager.fromUrl("jar:///dependency/test/1.0.0/test-1.0.0-sources.jar!/"),
      type = LibraryRootTypeId.SOURCES,
    )
    val expectedLibraryClassesRoot = LibraryRoot(
      url = virtualFileUrlManager.fromUrl("jar:///dependency/test/1.0.0/test-1.0.0.jar!/"),
      type = LibraryRootTypeId.COMPILED,
    )
    val expectedLibraryEntity = ExpectedLibraryEntity(
      libraryEntity = LibraryEntity(
        tableId = LibraryTableId.ModuleLibraryTableId(ModuleId(parentModuleEntity.name)),
        name = "file:///dependency/test/1.0.0/test-1.0.0-sources.jar",
        roots = listOf(expectedLibrarySourcesRoot, expectedLibraryClassesRoot),
        excludedRoots = emptyList()
      )
    )

    returnedLibraryEntity shouldBeEqual expectedLibraryEntity
    loadedEntries(LibraryEntity::class.java) shouldContainExactlyInAnyOrder listOf(expectedLibraryEntity)
  }

  @Test
  fun `should add multiple libraries to the workspace model`() {
    // given
    val library1 = Library(
      displayName = "file:///dependency/test1/1.0.0/test1-1.0.0-sources.jar",
      sourcesJar = "jar:///dependency/test1/1.0.0/test1-1.0.0-sources.jar!/",
      classesJar = "jar:///dependency/test1/1.0.0/test1-1.0.0.jar!/",
    )

    val library2 = Library(
      displayName = "file:///dependency/test2/2.0.0/test2-2.0.0-sources.jar",
      sourcesJar = "jar:///dependency/test2/2.0.0/test2-2.0.0-sources.jar!/",
      classesJar = "jar:///dependency/test2/2.0.0/test2-2.0.0.jar!/",
    )

    val libraries = listOf(library1, library2)

    // when
    val returnedLibraryEntries = runTestWriteAction {
      libraryEntityUpdater.addEntries(libraries, parentModuleEntity)
    }

    // then
    val expectedLibrarySourcesRoot1 = LibraryRoot(
      url = virtualFileUrlManager.fromUrl("jar:///dependency/test1/1.0.0/test1-1.0.0-sources.jar!/"),
      type = LibraryRootTypeId.SOURCES,
    )
    val expectedLibraryClassesRoot1 = LibraryRoot(
      url = virtualFileUrlManager.fromUrl("jar:///dependency/test1/1.0.0/test1-1.0.0.jar!/"),
      type = LibraryRootTypeId.COMPILED,
    )
    val expectedLibraryEntity1 = ExpectedLibraryEntity(
      libraryEntity = LibraryEntity(
        tableId = LibraryTableId.ModuleLibraryTableId(ModuleId(parentModuleEntity.name)),
        name = "file:///dependency/test1/1.0.0/test1-1.0.0-sources.jar",
        roots = listOf(expectedLibrarySourcesRoot1, expectedLibraryClassesRoot1),
        excludedRoots = emptyList()
      )
    )

    val expectedLibrarySourcesRoot2 = LibraryRoot(
      url = virtualFileUrlManager.fromUrl("jar:///dependency/test2/2.0.0/test2-2.0.0-sources.jar!/"),
      type = LibraryRootTypeId.SOURCES,
    )
    val expectedLibraryClassesRoot2 = LibraryRoot(
      url = virtualFileUrlManager.fromUrl("jar:///dependency/test2/2.0.0/test2-2.0.0.jar!/"),
      type = LibraryRootTypeId.COMPILED,
    )
    val expectedLibraryEntity2 = ExpectedLibraryEntity(
      libraryEntity = LibraryEntity(
        tableId = LibraryTableId.ModuleLibraryTableId(ModuleId(parentModuleEntity.name)),
        name = "file:///dependency/test2/2.0.0/test2-2.0.0-sources.jar",
        roots = listOf(expectedLibrarySourcesRoot2, expectedLibraryClassesRoot2),
        excludedRoots = emptyList()
      )
    )

    val expectedLibraryEntries = listOf(expectedLibraryEntity1, expectedLibraryEntity2)

    returnedLibraryEntries shouldContainExactlyInAnyOrder expectedLibraryEntries
    loadedEntries(LibraryEntity::class.java) shouldContainExactlyInAnyOrder expectedLibraryEntries
  }
}
