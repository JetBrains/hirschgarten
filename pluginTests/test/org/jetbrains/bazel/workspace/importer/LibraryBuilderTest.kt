package org.jetbrains.bazel.workspace.importer

import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.LibraryRoot
import com.intellij.platform.workspace.jps.entities.LibraryRootTypeId
import com.intellij.platform.workspace.jps.entities.LibraryTableId
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.workspace.model.matchers.entries.ExpectedLibraryEntity
import org.jetbrains.bazel.workspace.model.matchers.entries.shouldBeEqual
import org.jetbrains.bazel.workspace.model.matchers.entries.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.jetbrains.bazel.workspacemodel.entities.BazelDummyEntitySource
import org.jetbrains.bsp.protocol.LibraryItem
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

internal class LibraryBuilderTest : WorkspaceModelBaseTest() {
  @Test
  fun `should add one project library`() {
    val libraryItem =
      LibraryItem(
        id = Label.parse("//dependency/test:test"),
        ijars = emptyList(),
        jars = listOf(Path("/dependency/test/1.0.0/test-1.0.0.jar")),
        sourceJars = listOf(Path("/dependency/test/1.0.0/test-1.0.0-sources.jar")),
        mavenCoordinates = null,
        containsInternalJars = false,
      )

    val returned =
      LibraryBuilder.write(
        libraryItem = libraryItem,
        repoMapping = RepoMappingDisabled,
        importIjars = false,
        virtualFileUrlManager = virtualFileUrlManager,
        entitySource = BazelDummyEntitySource,
        storage = workspaceEntityStorageBuilder,
      )


    val expectedName = libraryItem.id.formatAsModuleName(RepoMappingDisabled)
    val expected =
      ExpectedLibraryEntity(
        libraryEntity =
          LibraryEntity(
            tableId = LibraryTableId.ProjectLibraryTableId,
            name = expectedName,
            roots = listOf(
              LibraryRoot(
                url = virtualFileUrlManager.getOrCreateFromUrl("jar:///dependency/test/1.0.0/test-1.0.0-sources.jar!/"),
                type = LibraryRootTypeId.SOURCES,
              ),
              LibraryRoot(
                url = virtualFileUrlManager.getOrCreateFromUrl("jar:///dependency/test/1.0.0/test-1.0.0.jar!/"),
                type = LibraryRootTypeId.COMPILED,
              ),
            ),
            entitySource = BazelDummyEntitySource,
          ),
      )

    returned shouldBeEqual expected
    loadedEntries(LibraryEntity::class.java) shouldContainExactlyInAnyOrder listOf(expected)
  }

  @Test
  fun `should add multiple project libraries`() {
    val item1 =
      LibraryItem(
        id = Label.parse("//dependency/test1:test1"),
        ijars = emptyList(),
        jars = listOf(Path("/dependency/test1/1.0.0/test1-1.0.0.jar")),
        sourceJars = listOf(Path("/dependency/test1/1.0.0/test1-1.0.0-sources.jar")),
        mavenCoordinates = null,
        containsInternalJars = false,
      )
    val item2 =
      LibraryItem(
        id = Label.parse("//dependency/test2:test2"),
        ijars = emptyList(),
        jars = listOf(Path("/dependency/test2/2.0.0/test2-2.0.0.jar")),
        sourceJars = listOf(Path("/dependency/test2/2.0.0/test2-2.0.0-sources.jar")),
        mavenCoordinates = null,
        containsInternalJars = false,
      )

    val returned =
      LibraryBuilder.write(
        libraryItems = listOf(item1, item2),
        repoMapping = RepoMappingDisabled,
        importIjars = false,
        virtualFileUrlManager = virtualFileUrlManager,
        entitySource = BazelDummyEntitySource,
        storage = workspaceEntityStorageBuilder,
      )

    val expected1 =
      ExpectedLibraryEntity(
        libraryEntity =
          LibraryEntity(
            tableId = LibraryTableId.ProjectLibraryTableId,
            name = item1.id.formatAsModuleName(RepoMappingDisabled),
            roots = listOf(
              LibraryRoot(
                url = virtualFileUrlManager.getOrCreateFromUrl("jar:///dependency/test1/1.0.0/test1-1.0.0-sources.jar!/"),
                type = LibraryRootTypeId.SOURCES,
              ),
              LibraryRoot(
                url = virtualFileUrlManager.getOrCreateFromUrl("jar:///dependency/test1/1.0.0/test1-1.0.0.jar!/"),
                type = LibraryRootTypeId.COMPILED,
              ),
            ),
            entitySource = BazelDummyEntitySource,
          ),
      )
    val expected2 =
      ExpectedLibraryEntity(
        libraryEntity =
          LibraryEntity(
            tableId = LibraryTableId.ProjectLibraryTableId,
            name = item2.id.formatAsModuleName(RepoMappingDisabled),
            roots = listOf(
              LibraryRoot(
                url = virtualFileUrlManager.getOrCreateFromUrl("jar:///dependency/test2/2.0.0/test2-2.0.0-sources.jar!/"),
                type = LibraryRootTypeId.SOURCES,
              ),
              LibraryRoot(
                url = virtualFileUrlManager.getOrCreateFromUrl("jar:///dependency/test2/2.0.0/test2-2.0.0.jar!/"),
                type = LibraryRootTypeId.COMPILED,
              ),
            ),
            entitySource = BazelDummyEntitySource,
          ),
      )

    returned shouldContainExactlyInAnyOrder listOf(expected1, expected2)
    loadedEntries(LibraryEntity::class.java) shouldContainExactlyInAnyOrder listOf(expected1, expected2)
  }

  @Test
  fun `should deduplicate libraries with same id`() {
    val item =
      LibraryItem(
        id = Label.parse("//dependency/test:test"),
        ijars = emptyList(),
        jars = listOf(Path("/dependency/test/1.0.0/test-1.0.0.jar")),
        sourceJars = listOf(Path("/dependency/test/1.0.0/test-1.0.0-sources.jar")),
        mavenCoordinates = null,
        containsInternalJars = false,
      )

    LibraryBuilder.write(item, RepoMappingDisabled, false, virtualFileUrlManager, BazelDummyEntitySource, workspaceEntityStorageBuilder)
    LibraryBuilder.write(item, RepoMappingDisabled, false, virtualFileUrlManager, BazelDummyEntitySource, workspaceEntityStorageBuilder)

    loadedEntries(LibraryEntity::class.java).size shouldBe 1
  }
}
