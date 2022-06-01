package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.SourceItem
import ch.epfl.scala.bsp4j.SourceItemKind
import ch.epfl.scala.bsp4j.SourcesItem
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.JavaSourceRoot
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.io.path.toPath

@DisplayName("SourcesItemToWorkspaceModelJavaSourceRootTransformer.transform(sourcesItem)")
class SourcesItemToJavaSourceRootTransformerTest {

  @Test
  fun `should return no sources roots for no sources items`() {
    // given
    val emptySources = listOf<SourcesItem>()

    // when
    val javaSources = SourcesItemToJavaSourceRootTransformer.transform(emptySources)

    // then
    javaSources shouldBe emptyList()
  }

  @Test
  fun `should return single source root for sources item with one file source`() {
    // given
    val sourceItem = SourceItem(
      "file:///root/dir/example/package/File.java",
      SourceItemKind.FILE,
      false
    )
    val sourceRoots = listOf("file:///root/dir/")

    val sourcesItem = SourcesItem(
      BuildTargetIdentifier("//target"),
      listOf(sourceItem)
    )
    sourcesItem.roots = sourceRoots

    // when
    val javaSources = SourcesItemToJavaSourceRootTransformer.transform(sourcesItem)

    // then
    val expectedJavaSourceRoot = JavaSourceRoot(
      sourceDir = URI.create("file:///root/dir/example/package/").toPath(),
      generated = false,
      packagePrefix = "example.package"
    )

    javaSources shouldContainExactlyInAnyOrder listOf(expectedJavaSourceRoot)
  }

  @Test
  fun `should return single source root for sources item with one dir source`() {
    // given
    val sourceItem = SourceItem(
      "file:///root/dir/example/package/",
      SourceItemKind.DIRECTORY,
      false
    )
    val sourceRoots = listOf("file:///root/dir/")

    val sourcesItem = SourcesItem(
      BuildTargetIdentifier("//target"),
      listOf(sourceItem)
    )
    sourcesItem.roots = sourceRoots

    // when
    val javaSources = SourcesItemToJavaSourceRootTransformer.transform(sourcesItem)

    // then
    val expectedJavaSourceRoot = JavaSourceRoot(
      sourceDir = URI.create("file:///root/dir/example/package/").toPath(),
      generated = false,
      packagePrefix = "example.package"
    )

    javaSources shouldContainExactlyInAnyOrder listOf(expectedJavaSourceRoot)
  }

  @Test
  fun `should return sources roots for sources item with multiple sources`() {
    // given
    val sourceItem1 = SourceItem(
      "file:///root/dir/example/package/File1.java",
      SourceItemKind.FILE,
      false
    )
    val sourceItem2 = SourceItem(
      "file:///root/dir/example/package/File2.java",
      SourceItemKind.FILE,
      false
    )
    val sourceItem3 = SourceItem(
      "file:///another/root/dir/another/example/package/",
      SourceItemKind.DIRECTORY,
      false
    )
    val sourceRoots = listOf(
      "file:///root/dir/",
      "file:///another/root/dir/",
    )

    val sourcesItem = SourcesItem(
      BuildTargetIdentifier("//target"),
      listOf(sourceItem1, sourceItem2, sourceItem3)
    )
    sourcesItem.roots = sourceRoots

    // when
    val javaSources = SourcesItemToJavaSourceRootTransformer.transform(sourcesItem)

    // then
    val expectedJavaSourceRoot1 = JavaSourceRoot(
      sourceDir = URI.create("file:///root/dir/example/package/").toPath(),
      generated = false,
      packagePrefix = "example.package"
    )
    val expectedJavaSourceRoot2 = JavaSourceRoot(
      sourceDir = URI.create("file:///another/root/dir/another/example/package/").toPath(),
      generated = false,
      packagePrefix = "another.example.package"
    )
    javaSources shouldContainExactlyInAnyOrder listOf(expectedJavaSourceRoot1, expectedJavaSourceRoot2)
  }

  @Test
  fun `should return parent sources root for multiple sources items in the same directory`() {
    // given
    val sourceItem1 = SourceItem(
      "file:///root/dir/example/package/a/",
      SourceItemKind.DIRECTORY,
      false
    )
    val sourceItem2 = SourceItem(
      "file:///root/dir/example/package/a/b/",
      SourceItemKind.DIRECTORY,
      false
    )
    val sourceItem3 = SourceItem(
      "file:///root/dir/example/package/a/b/c",
      SourceItemKind.DIRECTORY,
      false
    )
    val sourceItem4 = SourceItem(
      "file:///another/root/dir/another/example/package/",
      SourceItemKind.DIRECTORY,
      false
    )
    val sourceRoots = listOf(
      "file:///root/dir/",
      "file:///another/root/dir/",
    )

    val sourcesItem1 = SourcesItem(
      BuildTargetIdentifier("//target"),
      listOf(sourceItem1)
    )
    sourcesItem1.roots = sourceRoots
    val sourcesItem2 = SourcesItem(
      BuildTargetIdentifier("//target"),
      listOf(sourceItem2, sourceItem3, sourceItem4)
    )
    sourcesItem2.roots = sourceRoots

    val sourcesItems = listOf(sourcesItem1, sourcesItem2)

    // when
    val javaSources = SourcesItemToJavaSourceRootTransformer.transform(sourcesItems)

    // then
    val expectedJavaSourceRoot1 = JavaSourceRoot(
      sourceDir = URI.create("file:///root/dir/example/package/a/").toPath(),
      generated = false,
      packagePrefix = "example.package.a"
    )
    val expectedJavaSourceRoot2 = JavaSourceRoot(
      sourceDir = URI.create("file:///another/root/dir/another/example/package/").toPath(),
      generated = false,
      packagePrefix = "another.example.package"
    )
    javaSources shouldContainExactlyInAnyOrder listOf(expectedJavaSourceRoot1, expectedJavaSourceRoot2)
  }

  @Test
  fun `should return sources roots for multiple sources items`() {
    // given
    val sourceItem1 = SourceItem(
      "file:///root/dir/example/package/File1.java",
      SourceItemKind.FILE,
      false
    )
    val sourceItem2 = SourceItem(
      "file:///root/dir/example/package/File2.java",
      SourceItemKind.FILE,
      false
    )
    val sourceItem3 = SourceItem(
      "file:///another/root/dir/another/example/package/",
      SourceItemKind.DIRECTORY,
      false
    )
    val sourceRoots = listOf(
      "file:///root/dir/",
      "file:///another/root/dir/",
    )

    val sourcesItem1 = SourcesItem(
      BuildTargetIdentifier("//target"),
      listOf(sourceItem1)
    )
    sourcesItem1.roots = sourceRoots
    val sourcesItem2 = SourcesItem(
      BuildTargetIdentifier("//target"),
      listOf(sourceItem2, sourceItem3)
    )
    sourcesItem2.roots = sourceRoots

    val sourcesItems = listOf(sourcesItem1, sourcesItem2)

    // when
    val javaSources = SourcesItemToJavaSourceRootTransformer.transform(sourcesItems)

    // then
    val expectedJavaSourceRoot1 = JavaSourceRoot(
      sourceDir = URI.create("file:///root/dir/example/package/").toPath(),
      generated = false,
      packagePrefix = "example.package"
    )
    val expectedJavaSourceRoot2 = JavaSourceRoot(
      sourceDir = URI.create("file:///another/root/dir/another/example/package/").toPath(),
      generated = false,
      packagePrefix = "another.example.package"
    )
    javaSources shouldContainExactlyInAnyOrder listOf(expectedJavaSourceRoot1, expectedJavaSourceRoot2)
  }
}
