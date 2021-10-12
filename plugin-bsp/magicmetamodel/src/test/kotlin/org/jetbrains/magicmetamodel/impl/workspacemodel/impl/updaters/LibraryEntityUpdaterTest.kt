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
    val libraryUri = "file:///dependency/test/1.0.0/test-1.0.0-sources.jar"
    val libraryJar = "jar:$libraryUri!/"
    val library = Library(
      displayName = libraryUri,
      jar = libraryJar,
    )

    // when
    val libraryEntityUpdater = LibraryEntityUpdater(workspaceModelEntityUpdaterConfig)

    lateinit var returnedLibraryEntity: LibraryEntity

    WriteCommandAction.runWriteCommandAction(project) {
      returnedLibraryEntity = libraryEntityUpdater.addEntity(library, parentModuleEntity)
    }

    // then
    val expectedLibraryRoot = LibraryRoot(
      url = virtualFileUrlManager.fromUrl(libraryJar),
      type = LibraryRootTypeId.SOURCES,
    )
    val expectedLibraryEntityDetails = ExpectedLibraryEntityDetails(
      libraryEntity = LibraryEntity(
        tableId = LibraryTableId.ModuleLibraryTableId(ModuleId(parentModuleEntity.name)),
        name = libraryUri,
        roots = listOf(expectedLibraryRoot),
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
    val libraryUri1 = "file:///dependency/test1/1.0.0/test1-1.0.0-sources.jar"
    val libraryJar1 = "jar:$libraryUri1!/"
    val library1 = Library(
      displayName = libraryUri1,
      jar = libraryJar1,
    )

    val libraryUri2 = "file:///dependency/test2/1.0.0/test2-1.0.0-sources.jar"
    val libraryJar2 = "jar:$libraryUri2!/"
    val library2 = Library(
      displayName = libraryUri2,
      jar = libraryJar2,
    )

    val libraries = listOf(library1, library2)

    // when
    val libraryEntityUpdater = LibraryEntityUpdater(workspaceModelEntityUpdaterConfig)

    lateinit var returnedLibraryEntries: Collection<LibraryEntity>

    WriteCommandAction.runWriteCommandAction(project) {
      returnedLibraryEntries = libraryEntityUpdater.addEntries(libraries, parentModuleEntity)
    }

    // then
    val expectedLibraryRoot1 = LibraryRoot(
      url = virtualFileUrlManager.fromUrl(libraryJar1),
      type = LibraryRootTypeId.SOURCES,
    )
    val expectedLibraryEntityDetails1 = ExpectedLibraryEntityDetails(
      libraryEntity = LibraryEntity(
        tableId = LibraryTableId.ModuleLibraryTableId(ModuleId(parentModuleEntity.name)),
        name = libraryUri1,
        roots = listOf(expectedLibraryRoot1),
        excludedRoots = emptyList()
      )
    )

    val expectedLibraryRoot2 = LibraryRoot(
      url = virtualFileUrlManager.fromUrl(libraryJar2),
      type = LibraryRootTypeId.SOURCES,
    )
    val expectedLibraryEntityDetails2 = ExpectedLibraryEntityDetails(
      libraryEntity = LibraryEntity(
        tableId = LibraryTableId.ModuleLibraryTableId(ModuleId(parentModuleEntity.name)),
        name = libraryUri2,
        roots = listOf(expectedLibraryRoot2),
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
