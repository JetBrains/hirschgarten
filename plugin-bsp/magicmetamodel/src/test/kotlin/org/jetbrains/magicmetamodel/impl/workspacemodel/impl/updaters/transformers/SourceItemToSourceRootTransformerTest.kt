package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.SourceItem
import ch.epfl.scala.bsp4j.SourceItemKind
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.SourceRoot
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.io.path.toPath

@DisplayName("SourceItemToSourceRootTransformer.transform(sourcesItems) tests")
class SourceItemToSourceRootTransformerTest {

  @Test
  fun `should return no dirs for no sources`() {
    // given
    val emptySourceItems = emptyList<SourceItem>()

    // when
    val sourcesDirs = SourceItemToSourceRootTransformer.transform(emptySourceItems)

    // then
    sourcesDirs shouldBe emptyList()
  }

  @Test
  fun `should return single dir for single source file`() {
    testGeneratedAndNotGeneratedSources { generated ->
      // given
      val sourceItem = SourceItem("file:///example/source/File.java", SourceItemKind.FILE, generated)

      // when
      val sourceDir = SourceItemToSourceRootTransformer.transform(sourceItem)

      // then
      val expectedSourcePath = URI.create("file:///example/source/").toPath()
      val expectedSource = SourceRoot(expectedSourcePath, generated)

      sourceDir shouldBe expectedSource
    }
  }

  @Test
  fun `should return single dir for single source dir`() {
    testGeneratedAndNotGeneratedSources { generated ->
      // given
      val sourceItem = SourceItem("file:///example/source/", SourceItemKind.DIRECTORY, generated)

      // when
      val sourceDir = SourceItemToSourceRootTransformer.transform(sourceItem)

      // then
      val expectedSourcePath = URI.create("file:///example/source/").toPath()
      val expectedSource = SourceRoot(expectedSourcePath, generated)

      sourceDir shouldBe expectedSource
    }
  }

  @Test
  fun `should return single dir for multiple source files in the same dir`() {
    testGeneratedAndNotGeneratedSources { generated ->
      // given
      val sourceItems = listOf(
        SourceItem("file:///example/source/File1.java", SourceItemKind.FILE, generated),
        SourceItem("file:///example/source/File2.java", SourceItemKind.FILE, generated),
        SourceItem("file:///example/source/File3.java", SourceItemKind.FILE, generated),
      )

      // when
      val sourcesDirs = SourceItemToSourceRootTransformer.transform(sourceItems)

      // then
      val expectedSourcePath = URI.create("file:///example/source/").toPath()
      val expectedSource = SourceRoot(expectedSourcePath, generated)

      sourcesDirs shouldContainExactlyInAnyOrder listOf(expectedSource)
    }
  }

  @Test
  fun `should return multiple dirs for multiple source files`() {
    testGeneratedAndNotGeneratedSources { generated ->
      // given
      val sourceItems = listOf(
        SourceItem("file:///example/source1/File1.java", SourceItemKind.FILE, generated),
        SourceItem("file:///example/source1/File2.java", SourceItemKind.FILE, generated),
        SourceItem("file:///example/source1/subpackage/File2.java", SourceItemKind.FILE, generated),
        SourceItem("file:///example/source2/File1.java", SourceItemKind.FILE, generated),
      )

      // when
      val sourcesDirs = SourceItemToSourceRootTransformer.transform(sourceItems)

      // then
      val expectedSource1Path = URI.create("file:///example/source1/").toPath()
      val expectedSource1 = SourceRoot(expectedSource1Path, generated)

      val expectedSource1SubpackagePath = URI.create("file:///example/source1/subpackage/").toPath()
      val expectedSource1Subpackage = SourceRoot(expectedSource1SubpackagePath, generated)

      val expectedSource2Path = URI.create("file:///example/source2/").toPath()
      val expectedSource2 = SourceRoot(expectedSource2Path, generated)

      sourcesDirs shouldContainExactlyInAnyOrder listOf(expectedSource1, expectedSource1Subpackage, expectedSource2)
    }
  }

  @Test
  fun `should return multiple dirs for multiple source dirs`() {
    testGeneratedAndNotGeneratedSources { generated ->
      // given
      val sourceItems = listOf(
        SourceItem("file:///example/source1/", SourceItemKind.DIRECTORY, generated),
        SourceItem("file:///example/source1/subpackage/", SourceItemKind.DIRECTORY, generated),
        SourceItem("file:///example/source2/", SourceItemKind.DIRECTORY, generated),
      )

      // when
      val sourcesDirs = SourceItemToSourceRootTransformer.transform(sourceItems)

      // then
      val expectedSource1Path = URI.create("file:///example/source1/").toPath()
      val expectedSource1 = SourceRoot(expectedSource1Path, generated)

      val expectedSource1SubpackagePath = URI.create("file:///example/source1/subpackage/").toPath()
      val expectedSource1Subpackage = SourceRoot(expectedSource1SubpackagePath, generated)

      val expectedSource2Path = URI.create("file:///example/source2/").toPath()
      val expectedSource2 = SourceRoot(expectedSource2Path, generated)

      sourcesDirs shouldContainExactlyInAnyOrder listOf(expectedSource1, expectedSource1Subpackage, expectedSource2)
    }
  }

  @Test
  fun `should return multiple dirs for multiple source dirs and files`() {
    testGeneratedAndNotGeneratedSources { generated ->
      // given
      val sourceItems = listOf(
        SourceItem("file:///example/source1/", SourceItemKind.DIRECTORY, generated),
        SourceItem("file:///example/source1/subpackage/File1.java", SourceItemKind.FILE, generated),
        SourceItem("file:///example/source2/", SourceItemKind.DIRECTORY, generated),
        SourceItem("file:///example/source2/File1.java", SourceItemKind.FILE, generated),
      )

      // when
      val sourcesDirs = SourceItemToSourceRootTransformer.transform(sourceItems)

      // then
      val expectedSource1Path = URI.create("file:///example/source1/").toPath()
      val expectedSource1 = SourceRoot(expectedSource1Path, generated)

      val expectedSource1SubpackagePath = URI.create("file:///example/source1/subpackage/").toPath()
      val expectedSource1Subpackage = SourceRoot(expectedSource1SubpackagePath, generated)

      val expectedSource2Path = URI.create("file:///example/source2/").toPath()
      val expectedSource2 = SourceRoot(expectedSource2Path, generated)

      sourcesDirs shouldContainExactlyInAnyOrder listOf(expectedSource1, expectedSource1Subpackage, expectedSource2)
    }
  }

  private fun testGeneratedAndNotGeneratedSources(testBody: (Boolean) -> Unit) {
    testBody(false)
    testBody(true)
  }
}
