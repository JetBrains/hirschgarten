package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.SourceItemKind
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.JavaSourceRoot
import org.jetbrains.workspace.model.constructors.BuildTargetId
import org.jetbrains.workspace.model.constructors.SourceItem
import org.jetbrains.workspace.model.constructors.SourcesItem
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile
import kotlin.io.path.name

@DisplayName("SourcesItemToJavaSourceRootTransformerIntellijHackPleaseRemoveHACK tests")
class SourcesItemToJavaSourceRootTransformerIntellijHackPleaseRemoveHACKTest {

  @Nested
  @DisplayName("SourcesItemToJavaSourceRootTransformerIntellijHackPleaseRemoveHACK.transform(sourcesItem) tests")
  inner class TransformTest {

    @Test
    fun `should return single source root for sources item with one file source and excluded rest of files`() {
      // given
      val projectRoot = createTempDirectory("project")
      projectRoot.toFile().deleteOnExit()

      val package1Root = createTempDirectory(projectRoot, "package1")
      package1Root.toFile().deleteOnExit()

      val package2Root = createTempDirectory(package1Root, "package2")
      package2Root.toFile().deleteOnExit()

      val filePath = createTempFile(package2Root, "File", ".java")
      filePath.toFile().deleteOnExit()

      val sourceItem = SourceItem(
        uri = filePath.toUri().toString(),
        kind = SourceItemKind.FILE,
      )
      val sourcesItem = SourcesItem(
        target = BuildTargetId("target"),
        sources = listOf(sourceItem),
        roots = listOf(projectRoot.toUri().toString()),
      )

      // another java file in the dir
      val excludedFilePath = createTempFile(package2Root, "ExcludedFile", ".java")
      excludedFilePath.toFile().deleteOnExit()

      // another file in the dir but not java
      val notExcludedFilePath = createTempFile(package2Root, "NotExcludedFile")
      notExcludedFilePath.toFile().deleteOnExit()

      // when
      val javaSources = SourcesItemToJavaSourceRootTransformerIntellijHackPleaseRemoveHACK.transform(sourcesItem)

      // then
      val expectedJavaSourceRoot = JavaSourceRoot(
        sourceDir = package2Root,
        generated = false,
        packagePrefix = "${package1Root.name}.${package2Root.name}",
        excludedFiles = listOf(excludedFilePath)
      )
      javaSources shouldContainExactlyInAnyOrder listOf(expectedJavaSourceRoot)
    }
  }

  @Nested
  @DisplayName("SourcesItemToJavaSourceRootTransformerIntellijHackPleaseRemoveHACK.transform(sourcesItems) tests")
  inner class TransformListTest {

    @Test
    fun `should return no sources roots for no sources items`() {
      // given
      val emptySources = listOf<SourcesItem>()

      // when
      val javaSources = SourcesItemToJavaSourceRootTransformerIntellijHackPleaseRemoveHACK.transform(emptySources)

      // then
      javaSources shouldBe emptyList()
    }

    @Test
    fun `should return parent sources root for multiple sources items in the same directory`() {
      // given
      val projectRoot = createTempDirectory("project")
      projectRoot.toFile().deleteOnExit()

      val packageA1Root = createTempDirectory(projectRoot, "packageA1")
      packageA1Root.toFile().deleteOnExit()

      val packageA2Root = createTempDirectory(packageA1Root, "packageA2")
      packageA2Root.toFile().deleteOnExit()

      val fileA2Path = createTempFile(packageA2Root, "File", ".java")
      fileA2Path.toFile().deleteOnExit()

      // another java file in the dir
      val excludedFilePath = createTempFile(packageA2Root, "ExcludedFile", ".java")
      excludedFilePath.toFile().deleteOnExit()

      // another file in the dir but not java
      val notExcludedFilePath = createTempFile(packageA2Root, "NotExcludedFile")
      notExcludedFilePath.toFile().deleteOnExit()

      val packageB1Root = createTempDirectory(projectRoot, "packageB1")
      packageB1Root.toFile().deleteOnExit()

      val sourceItemA2 = SourceItem(
        uri = fileA2Path.toUri().toString(),
        kind = SourceItemKind.FILE,
      )
      val sourcesItem1 = SourcesItem(
        target = BuildTargetId("target"),
        sources = listOf(sourceItemA2),
        roots = listOf(projectRoot.toUri().toString()),
      )

      val sourceItemA1 = SourceItem(
        uri = packageA1Root.toUri().toString(),
        kind = SourceItemKind.DIRECTORY,
      )
      val sourceItemB1 = SourceItem(
        uri = packageB1Root.toUri().toString(),
        kind = SourceItemKind.DIRECTORY,
      )
      val sourcesItem2 = SourcesItem(
        target = BuildTargetId("target"),
        sources = listOf(sourceItemA1, sourceItemB1),
        roots = listOf(projectRoot.toUri().toString()),
      )

      val sourcesItems = listOf(sourcesItem1, sourcesItem2)

      // when
      val javaSources = SourcesItemToJavaSourceRootTransformerIntellijHackPleaseRemoveHACK.transform(sourcesItems)

      // then
      val expectedJavaSourceRoot1 = JavaSourceRoot(
        sourceDir = packageA1Root,
        generated = false,
        packagePrefix = packageA1Root.name,
        excludedFiles = listOf(excludedFilePath)
      )
      val expectedJavaSourceRoot2 = JavaSourceRoot(
        sourceDir = packageB1Root,
        generated = false,
        packagePrefix = packageB1Root.name
      )
      javaSources shouldContainExactlyInAnyOrder listOf(expectedJavaSourceRoot1, expectedJavaSourceRoot2)
    }
  }
}
