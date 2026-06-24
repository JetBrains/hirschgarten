package org.jetbrains.bazel.workspace.importer

import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.LibraryRoot
import com.intellij.platform.workspace.jps.entities.LibraryRootTypeId
import com.intellij.platform.workspace.jps.entities.LibraryTableId
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsLibraryName
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
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
        key = WorkspaceTargetKey(label = Label.parse("//dependency/test:test")),
        ijars = emptyList(),
        jars = listOf(Path("/dependency/test/1.0.0/test-1.0.0.jar")),
        sourceJars = listOf(Path("/dependency/test/1.0.0/test-1.0.0-sources.jar")),
        mavenCoordinates = null,
        containsInternalJars = false,
      )

    val returned =
      LibraryBuilder.write(
        libraryItem = libraryItem,
        importIjars = false,
        virtualFileUrlManager = virtualFileUrlManager,
        entitySource = BazelDummyEntitySource,
        libraryNameProvider = { key -> key.formatAsLibraryName(RepoMappingDisabled, withFullKey = true) },
        storage = workspaceEntityStorageBuilder,
      )


    val expectedName = libraryItem.key.label.formatAsModuleName(RepoMappingDisabled)
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
        key = WorkspaceTargetKey(label = Label.parse("//dependency/test1:test1")),
        ijars = emptyList(),
        jars = listOf(Path("/dependency/test1/1.0.0/test1-1.0.0.jar")),
        sourceJars = listOf(Path("/dependency/test1/1.0.0/test1-1.0.0-sources.jar")),
        mavenCoordinates = null,
        containsInternalJars = false,
      )
    val item2 =
      LibraryItem(
        key = WorkspaceTargetKey(label = Label.parse("//dependency/test2:test2")),
        ijars = emptyList(),
        jars = listOf(Path("/dependency/test2/2.0.0/test2-2.0.0.jar")),
        sourceJars = listOf(Path("/dependency/test2/2.0.0/test2-2.0.0-sources.jar")),
        mavenCoordinates = null,
        containsInternalJars = false,
      )

    val returned =
      LibraryBuilder.writeAll(
        libraryItems = listOf(item1, item2),
        importIjars = false,
        virtualFileUrlManager = virtualFileUrlManager,
        entitySource = BazelDummyEntitySource,
        libraryNameProvider = { key -> key.formatAsLibraryName(RepoMappingDisabled, withFullKey = true) },
        storage = workspaceEntityStorageBuilder,
      )

    val expected1 =
      ExpectedLibraryEntity(
        libraryEntity =
          LibraryEntity(
            tableId = LibraryTableId.ProjectLibraryTableId,
            name = item1.key.label.formatAsModuleName(RepoMappingDisabled),
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
            name = item2.key.label.formatAsModuleName(RepoMappingDisabled),
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
        key = WorkspaceTargetKey(label = Label.parse("//dependency/test:test")),
        ijars = emptyList(),
        jars = listOf(Path("/dependency/test/1.0.0/test-1.0.0.jar")),
        sourceJars = listOf(Path("/dependency/test/1.0.0/test-1.0.0-sources.jar")),
        mavenCoordinates = null,
        containsInternalJars = false,
      )

    LibraryBuilder.write(
      libraryItem = item,
      importIjars = false,
      virtualFileUrlManager = virtualFileUrlManager,
      entitySource = BazelDummyEntitySource,
      libraryNameProvider = { key -> key.formatAsLibraryName(RepoMappingDisabled, withFullKey = true) },
      storage = workspaceEntityStorageBuilder,
    )
    LibraryBuilder.write(
      libraryItem = item,
      importIjars = false,
      virtualFileUrlManager = virtualFileUrlManager,
      entitySource = BazelDummyEntitySource,
      libraryNameProvider = { key -> key.formatAsLibraryName(RepoMappingDisabled, withFullKey = true) },
      storage = workspaceEntityStorageBuilder,
    )

    loadedEntries(LibraryEntity::class.java).size shouldBe 1
  }
}
