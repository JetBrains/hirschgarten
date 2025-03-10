package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.bsp.protocol.SourceItem
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
  fun `should return single file for single source file`() {
    testGeneratedAndNotGeneratedSources { generated ->
      // given
      val sourceItem = SourceItem("file:///example/source/File.java", generated)

      // when
      val sourceDir = SourceItemToSourceRootTransformer.transform(sourceItem)

      // then
      val expectedSourcePath = URI.create("file:///example/source/File.java").toPath()
      val expectedSource = SourceRoot(expectedSourcePath, generated)

      sourceDir shouldBe expectedSource
    }
  }

  @Test
  fun `should return multiple dirs for multiple source files`() {
    testGeneratedAndNotGeneratedSources { generated ->
      // given
      val sourceItems =
        listOf(
          SourceItem("file:///example/source1/File1.java", generated),
          SourceItem("file:///example/source1/File2.java", generated),
          SourceItem("file:///example/source1/subpackage/File2.java", generated),
          SourceItem("file:///example/source2/File1.java", generated),
        )

      // when
      val sourcesDirs = SourceItemToSourceRootTransformer.transform(sourceItems)

      // then
      val expectedSource1PathA = URI.create("file:///example/source1/File1.java").toPath()
      val expectedSource1A = SourceRoot(expectedSource1PathA, generated)

      val expectedSource1PathB = URI.create("file:///example/source1/File2.java").toPath()
      val expectedSource1B = SourceRoot(expectedSource1PathB, generated)

      val expectedSource1SubpackagePath = URI.create("file:///example/source1/subpackage/File2.java").toPath()
      val expectedSource1Subpackage = SourceRoot(expectedSource1SubpackagePath, generated)

      val expectedSource2Path = URI.create("file:///example/source2/File1.java").toPath()
      val expectedSource2 = SourceRoot(expectedSource2Path, generated)

      sourcesDirs shouldContainExactlyInAnyOrder
        listOf(expectedSource1A, expectedSource1B, expectedSource1Subpackage, expectedSource2)
    }
  }

  @Test
  fun `should return multiple dirs and files for multiple source dirs and files`() {
    testGeneratedAndNotGeneratedSources { generated ->
      // given
      val sourceItems =
        listOf(
          SourceItem("file:///example/source1/subpackage/File1.java", generated),
          SourceItem("file:///example/source2/File1.java", generated),
        )

      // when
      val sourcesDirs = SourceItemToSourceRootTransformer.transform(sourceItems)

      // then
      val expectedSource1SubpackagePath = URI.create("file:///example/source1/subpackage/File1.java").toPath()
      val expectedSource1Subpackage = SourceRoot(expectedSource1SubpackagePath, generated)

      val expectedSource2FilePath = URI.create("file:///example/source2/File1.java").toPath()
      val expectedSource2File = SourceRoot(expectedSource2FilePath, generated)

      sourcesDirs shouldContainExactlyInAnyOrder
        listOf(expectedSource1Subpackage, expectedSource2File)
    }
  }

  private fun testGeneratedAndNotGeneratedSources(testBody: (Boolean) -> Unit) {
    testBody(false)
    testBody(true)
  }
}
