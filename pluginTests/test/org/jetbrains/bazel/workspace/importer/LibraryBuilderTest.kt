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
        virtualFileUrlManager = virtualFileUrlManager,
        entitySource = BazelDummyEntitySource,
        libraryNameProvider = { key -> key.formatAsLibraryName(RepoMappingDisabled, withFullKey = true) },
        storage = workspaceEntityStorageBuilder,
        isTargetImported = { false },
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
                url = virtualFileUrlManager.storeAndGet("jar:///dependency/test/1.0.0/test-1.0.0-sources.jar!/"),
                type = LibraryRootTypeId.SOURCES,
              ),
              LibraryRoot(
                url = virtualFileUrlManager.storeAndGet("jar:///dependency/test/1.0.0/test-1.0.0.jar!/"),
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
        virtualFileUrlManager = virtualFileUrlManager,
        entitySource = BazelDummyEntitySource,
        libraryNameProvider = { key -> key.formatAsLibraryName(RepoMappingDisabled, withFullKey = true) },
        storage = workspaceEntityStorageBuilder,
        isTargetImported = { false },
      )

    val expected1 =
      ExpectedLibraryEntity(
        libraryEntity =
          LibraryEntity(
            tableId = LibraryTableId.ProjectLibraryTableId,
            name = item1.key.label.formatAsModuleName(RepoMappingDisabled),
            roots = listOf(
              LibraryRoot(
                url = virtualFileUrlManager.storeAndGet("jar:///dependency/test1/1.0.0/test1-1.0.0-sources.jar!/"),
                type = LibraryRootTypeId.SOURCES,
              ),
              LibraryRoot(
                url = virtualFileUrlManager.storeAndGet("jar:///dependency/test1/1.0.0/test1-1.0.0.jar!/"),
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
                url = virtualFileUrlManager.storeAndGet("jar:///dependency/test2/2.0.0/test2-2.0.0-sources.jar!/"),
                type = LibraryRootTypeId.SOURCES,
              ),
              LibraryRoot(
                url = virtualFileUrlManager.storeAndGet("jar:///dependency/test2/2.0.0/test2-2.0.0.jar!/"),
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
      virtualFileUrlManager = virtualFileUrlManager,
      entitySource = BazelDummyEntitySource,
      libraryNameProvider = { key -> key.formatAsLibraryName(RepoMappingDisabled, withFullKey = true) },
      storage = workspaceEntityStorageBuilder,
      isTargetImported = { false },
    )
    LibraryBuilder.write(
      libraryItem = item,
      virtualFileUrlManager = virtualFileUrlManager,
      entitySource = BazelDummyEntitySource,
      libraryNameProvider = { key -> key.formatAsLibraryName(RepoMappingDisabled, withFullKey = true) },
      storage = workspaceEntityStorageBuilder,
      isTargetImported = { false },
    )

    loadedEntries(LibraryEntity::class.java).size shouldBe 1
  }

  @Test
  fun `should prefer the interface jar for a library of a target that the import leaves out`() {
    val libraryItem =
      LibraryItem(
        key = WorkspaceTargetKey(label = Label.parse("//dependency/test:test")),
        ijars = listOf(Path("/dependency/test/test-hjar.jar")),
        jars = listOf(Path("/dependency/test/test.jar")),
        sourceJars = listOf(Path("/dependency/test/test-src.jar")),
        mavenCoordinates = null,
        containsInternalJars = true,
      )

    val returned =
      LibraryBuilder.write(
        libraryItem = libraryItem,
        virtualFileUrlManager = virtualFileUrlManager,
        entitySource = BazelDummyEntitySource,
        libraryNameProvider = { key -> key.formatAsLibraryName(RepoMappingDisabled, withFullKey = true) },
        storage = workspaceEntityStorageBuilder,
        isTargetImported = { false },
      )

    val expected =
      ExpectedLibraryEntity(
        libraryEntity =
          LibraryEntity(
            tableId = LibraryTableId.ProjectLibraryTableId,
            name = libraryItem.key.label.formatAsModuleName(RepoMappingDisabled),
            roots = listOf(
              LibraryRoot(
                url = virtualFileUrlManager.getOrCreateFromUrl("jar:///dependency/test/test-src.jar!/"),
                type = LibraryRootTypeId.SOURCES,
              ),
              LibraryRoot(
                url = virtualFileUrlManager.getOrCreateFromUrl("jar:///dependency/test/test-hjar.jar!/"),
                type = LibraryRootTypeId.COMPILED,
              ),
            ),
            entitySource = BazelDummyEntitySource,
          ),
      )

    returned shouldBeEqual expected
  }

  @Test
  fun `should prefer the full jar when the library has no source jars`() {
    val libraryItem =
      LibraryItem(
        key = WorkspaceTargetKey(label = Label.parse("//dependency/test:test")),
        ijars = listOf(Path("/dependency/test/test-hjar.jar")),
        jars = listOf(Path("/dependency/test/test.jar")),
        sourceJars = emptyList(),
        mavenCoordinates = null,
        containsInternalJars = true,
      )

    val returned =
      LibraryBuilder.write(
        libraryItem = libraryItem,
        virtualFileUrlManager = virtualFileUrlManager,
        entitySource = BazelDummyEntitySource,
        libraryNameProvider = { key -> key.formatAsLibraryName(RepoMappingDisabled, withFullKey = true) },
        storage = workspaceEntityStorageBuilder,
        isTargetImported = { false },
      )

    val expected =
      ExpectedLibraryEntity(
        libraryEntity =
          LibraryEntity(
            tableId = LibraryTableId.ProjectLibraryTableId,
            name = libraryItem.key.label.formatAsModuleName(RepoMappingDisabled),
            roots = listOf(
              LibraryRoot(
                url = virtualFileUrlManager.getOrCreateFromUrl("jar:///dependency/test/test.jar!/"),
                type = LibraryRootTypeId.COMPILED,
              ),
            ),
            entitySource = BazelDummyEntitySource,
          ),
      )

    returned shouldBeEqual expected
  }

  @Test
  fun `should prefer the full jar for a library of an imported module`() {
    val libraryItem =
      LibraryItem(
        key = WorkspaceTargetKey(label = Label.parse("//dependency/test:test")),
        ijars = listOf(Path("/dependency/test/test-hjar.jar")),
        jars = listOf(Path("/dependency/test/test.jar")),
        sourceJars = listOf(Path("/dependency/test/test-src.jar")),
        mavenCoordinates = null,
        containsInternalJars = true,
      )

    val returned =
      LibraryBuilder.write(
        libraryItem = libraryItem,
        virtualFileUrlManager = virtualFileUrlManager,
        entitySource = BazelDummyEntitySource,
        libraryNameProvider = { key -> key.formatAsLibraryName(RepoMappingDisabled, withFullKey = true) },
        storage = workspaceEntityStorageBuilder,
        isTargetImported = { it == libraryItem.key },
      )

    val expected =
      ExpectedLibraryEntity(
        libraryEntity =
          LibraryEntity(
            tableId = LibraryTableId.ProjectLibraryTableId,
            name = libraryItem.key.label.formatAsModuleName(RepoMappingDisabled),
            roots = listOf(
              LibraryRoot(
                url = virtualFileUrlManager.getOrCreateFromUrl("jar:///dependency/test/test-src.jar!/"),
                type = LibraryRootTypeId.SOURCES,
              ),
              LibraryRoot(
                url = virtualFileUrlManager.getOrCreateFromUrl("jar:///dependency/test/test.jar!/"),
                type = LibraryRootTypeId.COMPILED,
              ),
            ),
            entitySource = BazelDummyEntitySource,
          ),
      )

    returned shouldBeEqual expected
  }

  @Test
  fun `should prefer the full jar for an external library with source jars`() {
    val libraryItem =
      LibraryItem(
        key = WorkspaceTargetKey(label = Label.parse("@maven//:com_example_lib")),
        ijars = listOf(Path("/external/maven/lib-ijar.jar")),
        jars = listOf(Path("/external/maven/lib.jar")),
        sourceJars = listOf(Path("/external/maven/lib-sources.jar")),
        mavenCoordinates = null,
        containsInternalJars = false,
      )

    val returned =
      LibraryBuilder.write(
        libraryItem = libraryItem,
        virtualFileUrlManager = virtualFileUrlManager,
        entitySource = BazelDummyEntitySource,
        libraryNameProvider = { key -> key.formatAsLibraryName(RepoMappingDisabled, withFullKey = true) },
        storage = workspaceEntityStorageBuilder,
        isTargetImported = { false },
      )

    val expected =
      ExpectedLibraryEntity(
        libraryEntity =
          LibraryEntity(
            tableId = LibraryTableId.ProjectLibraryTableId,
            name = libraryItem.key.label.formatAsModuleName(RepoMappingDisabled),
            roots = listOf(
              LibraryRoot(
                url = virtualFileUrlManager.getOrCreateFromUrl("jar:///external/maven/lib-sources.jar!/"),
                type = LibraryRootTypeId.SOURCES,
              ),
              LibraryRoot(
                url = virtualFileUrlManager.getOrCreateFromUrl("jar:///external/maven/lib.jar!/"),
                type = LibraryRootTypeId.COMPILED,
              ),
            ),
            entitySource = BazelDummyEntitySource,
          ),
      )

    returned shouldBeEqual expected
  }

  @Test
  fun `should fall back to the full jar when the target has no interface jar`() {
    val libraryItem =
      LibraryItem(
        key = WorkspaceTargetKey(label = Label.parse("//dependency/test:test")),
        ijars = emptyList(),
        jars = listOf(Path("/dependency/test/test.jar")),
        sourceJars = listOf(Path("/dependency/test/test-src.jar")),
        mavenCoordinates = null,
        containsInternalJars = true,
      )

    val returned =
      LibraryBuilder.write(
        libraryItem = libraryItem,
        virtualFileUrlManager = virtualFileUrlManager,
        entitySource = BazelDummyEntitySource,
        libraryNameProvider = { key -> key.formatAsLibraryName(RepoMappingDisabled, withFullKey = true) },
        storage = workspaceEntityStorageBuilder,
        isTargetImported = { false },
      )

    val expected =
      ExpectedLibraryEntity(
        libraryEntity =
          LibraryEntity(
            tableId = LibraryTableId.ProjectLibraryTableId,
            name = libraryItem.key.label.formatAsModuleName(RepoMappingDisabled),
            roots = listOf(
              LibraryRoot(
                url = virtualFileUrlManager.getOrCreateFromUrl("jar:///dependency/test/test-src.jar!/"),
                type = LibraryRootTypeId.SOURCES,
              ),
              LibraryRoot(
                url = virtualFileUrlManager.getOrCreateFromUrl("jar:///dependency/test/test.jar!/"),
                type = LibraryRootTypeId.COMPILED,
              ),
            ),
            entitySource = BazelDummyEntitySource,
          ),
      )

    returned shouldBeEqual expected
  }
}
