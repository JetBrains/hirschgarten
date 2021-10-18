package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.workspaceModel.storage.bridgeEntities.LibraryEntity
import com.intellij.workspaceModel.storage.bridgeEntities.LibraryRoot
import com.intellij.workspaceModel.storage.bridgeEntities.LibraryRootTypeId
import com.intellij.workspaceModel.storage.bridgeEntities.LibraryTableId
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleId
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

private data class ExpectedLibraryEntityDetails(
  val libraryEntity: LibraryEntity,
)

@DisplayName("LibraryEntityUpdater.addEntity(entityToAdd, parentModuleEntity) tests")
internal class LibraryEntityUpdaterTest : WorkspaceModelEntityWithParentModuleUpdaterBaseTest() {

  @Test
  fun `should add one library to the workspace model`() {
    // given
    val library = Library(
      displayName = "file:///dependency/test/1.0.0/test-1.0.0-sources.jar",
      sourcesJar = "jar:///dependency/test/1.0.0/test-1.0.0-sources.jar!/",
      classesJar = "jar:///dependency/test/1.0.0/test-1.0.0.jar!/",
    )

    // when
    val libraryEntityUpdater = LibraryEntityUpdater(workspaceModelEntityUpdaterConfig)

    lateinit var returnedLibraryEntity: LibraryEntity

    WriteCommandAction.runWriteCommandAction(project) {
      returnedLibraryEntity = libraryEntityUpdater.addEntity(library, parentModuleEntity)
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
    val expectedLibraryEntityDetails = ExpectedLibraryEntityDetails(
      libraryEntity = LibraryEntity(
        tableId = LibraryTableId.ModuleLibraryTableId(ModuleId(parentModuleEntity.name)),
        name = "file:///dependency/test/1.0.0/test-1.0.0-sources.jar",
        roots = listOf(expectedLibrarySourcesRoot, expectedLibraryClassesRoot),
        excludedRoots = emptyList()
      )
    )

    validateJavaResourceRootEntity(returnedLibraryEntity, expectedLibraryEntityDetails)

    workspaceModelLoadedEntries(LibraryEntity::class.java) shouldContainExactlyInAnyOrder Pair(
      listOf(expectedLibraryEntityDetails), this::validateJavaResourceRootEntity
    )
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
    val libraryEntityUpdater = LibraryEntityUpdater(workspaceModelEntityUpdaterConfig)

    lateinit var returnedLibraryEntries: Collection<LibraryEntity>

    WriteCommandAction.runWriteCommandAction(project) {
      returnedLibraryEntries = libraryEntityUpdater.addEntries(libraries, parentModuleEntity)
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
    val expectedLibraryEntityDetails1 = ExpectedLibraryEntityDetails(
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
    val expectedLibraryEntityDetails2 = ExpectedLibraryEntityDetails(
      libraryEntity = LibraryEntity(
        tableId = LibraryTableId.ModuleLibraryTableId(ModuleId(parentModuleEntity.name)),
        name = "file:///dependency/test2/2.0.0/test2-2.0.0-sources.jar",
        roots = listOf(expectedLibrarySourcesRoot2, expectedLibraryClassesRoot2),
        excludedRoots = emptyList()
      )
    )

    returnedLibraryEntries shouldContainExactlyInAnyOrder Pair(
      listOf(expectedLibraryEntityDetails1, expectedLibraryEntityDetails2),
      this::validateJavaResourceRootEntity
    )

    workspaceModelLoadedEntries(LibraryEntity::class.java) shouldContainExactlyInAnyOrder Pair(
      listOf(expectedLibraryEntityDetails1, expectedLibraryEntityDetails2),
      this::validateJavaResourceRootEntity
    )
  }

  private fun validateJavaResourceRootEntity(
    actual: LibraryEntity,
    expected: ExpectedLibraryEntityDetails
  ) {
    actual.tableId shouldBe expected.libraryEntity.tableId
    actual.name shouldBe expected.libraryEntity.name
    actual.roots shouldContainExactlyInAnyOrder expected.libraryEntity.roots
    actual.excludedRoots shouldContainExactlyInAnyOrder expected.libraryEntity.excludedRoots
  }
}
