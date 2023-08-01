package org.jetbrains.magicmetamodel.impl

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.SourceItem
import ch.epfl.scala.bsp4j.SourceItemKind
import ch.epfl.scala.bsp4j.SourcesItem
import ch.epfl.scala.bsp4j.TextDocumentIdentifier
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("TargetsDetailsForDocumentProvider tests")
class TargetsDetailsForDocumentProviderTest {

  @Nested
  @DisplayName("targetsDetailsForDocumentProvider.getAllDocumentsTest() tests")
  inner class TargetsDetailsForDocumentProviderGetAllDocumentsTest {

    @Test
    fun `should return no documents for no sources`() {
      // given
      val sources = emptyList<SourcesItem>()

      // when
      val targetsDetailsForDocumentProvider = TargetsDetailsForDocumentProvider(sources)

      val allDocuments = targetsDetailsForDocumentProvider.getAllDocuments()

      // then
      allDocuments shouldBe emptyList()
    }

    @Test
    fun `should return all documents for not shared sources`() {
      // given
      val file1InTarget1Uri = "file:///file1/in/target1"
      val file2InTarget1Uri = "file:///file2/in/target1"

      val target1 = BuildTargetIdentifier("//target1")

      val file1InTarget1Source = SourceItem(file1InTarget1Uri, SourceItemKind.DIRECTORY, false)
      val file2InTarget1Source = SourceItem(file2InTarget1Uri, SourceItemKind.DIRECTORY, false)
      val target1Sources = SourcesItem(target1, listOf(file1InTarget1Source, file2InTarget1Source))

      val sources = listOf(target1Sources)

      // when
      val targetsDetailsForDocumentProvider = TargetsDetailsForDocumentProvider(sources)

      val allDocuments = targetsDetailsForDocumentProvider.getAllDocuments()

      // then
      val expectedFile1 = TextDocumentIdentifier(file1InTarget1Uri)
      val expectedFile2 = TextDocumentIdentifier(file2InTarget1Uri)
      val expectedDocuments = listOf(expectedFile1, expectedFile2)

      allDocuments shouldContainExactlyInAnyOrder expectedDocuments
    }

    @Test
    fun `should return all documents for shared sources`() {
      // given
      val fileInTarget1Target2Uri = "file:///file/in/target1/target2"

      val target1 = BuildTargetIdentifier("//target1")
      val target2 = BuildTargetIdentifier("//target2")

      val fileInTarget1Target2Source = SourceItem(fileInTarget1Target2Uri, SourceItemKind.DIRECTORY, false)

      val target1Sources = SourcesItem(target1, listOf(fileInTarget1Target2Source))
      val target2Sources = SourcesItem(target2, listOf(fileInTarget1Target2Source))

      val sources = listOf(target1Sources, target2Sources)

      // when
      val targetsDetailsForDocumentProvider = TargetsDetailsForDocumentProvider(sources)

      val allDocuments = targetsDetailsForDocumentProvider.getAllDocuments()

      // then
      val expectedFile = TextDocumentIdentifier(fileInTarget1Target2Uri)
      val expectedDocuments = listOf(expectedFile)

      allDocuments shouldContainExactlyInAnyOrder expectedDocuments
    }
  }

  @Nested
  @DisplayName("targetsDetailsForDocumentProvider.getTargetsDetailsForDocument(sources) tests")
  inner class TargetsDetailsForDocumentProviderGetTargetsDetailsForDocumentTest {

    @Nested
    @DisplayName("tests with files as sources")
    inner class FileBasedSourcesTests {

      @Test
      fun `should map multiple files to single target`() {
        // given
        val file1InTarget1Uri = "file:///file1/in/target1"
        val file2InTarget1Uri = "file:///file2/in/target1"

        val target1 = BuildTargetIdentifier("//target1")

        val file1InTarget1Source = SourceItem(file1InTarget1Uri, SourceItemKind.FILE, false)
        val file2InTarget1Source = SourceItem(file2InTarget1Uri, SourceItemKind.FILE, false)

        val target1Sources = SourcesItem(target1, listOf(file1InTarget1Source, file2InTarget1Source))

        val sources = listOf(target1Sources)

        // when
        val targetsDetailsForDocumentProvider = TargetsDetailsForDocumentProvider(sources)

        val file1InTarget1Id = TextDocumentIdentifier(file1InTarget1Uri)
        val file2InTarget1Id = TextDocumentIdentifier(file2InTarget1Uri)

        val file1InTarget1TargetsDetails =
          targetsDetailsForDocumentProvider.getTargetsDetailsForDocument(file1InTarget1Id)
        val file2InTarget1TargetsDetails =
          targetsDetailsForDocumentProvider.getTargetsDetailsForDocument(file2InTarget1Id)

        // then
        file1InTarget1TargetsDetails shouldBe listOf("//target1")
        file2InTarget1TargetsDetails shouldBe listOf("//target1")
      }

      @Test
      fun `should map one file to multiple targets`() {
        // given
        val fileInTarget1Target2Uri = "file:///file/in/target1/target2"

        val target1 = BuildTargetIdentifier("//target1")
        val target2 = BuildTargetIdentifier("//target2")

        val fileInTarget1Target2Source = SourceItem(fileInTarget1Target2Uri, SourceItemKind.FILE, false)

        val target1Sources = SourcesItem(target1, listOf(fileInTarget1Target2Source))
        val target2Sources = SourcesItem(target2, listOf(fileInTarget1Target2Source))

        val sources = listOf(target1Sources, target2Sources)

        // when
        val targetsDetailsForDocumentProvider = TargetsDetailsForDocumentProvider(sources)

        val fileInTarget1Target2Id = TextDocumentIdentifier(fileInTarget1Target2Uri)

        val fileInTarget1Target2TargetsDetails =
          targetsDetailsForDocumentProvider.getTargetsDetailsForDocument(fileInTarget1Target2Id)

        // then
        fileInTarget1Target2TargetsDetails shouldContainExactlyInAnyOrder listOf("//target1", "//target2")
      }

      @Test
      fun `should map multiple files to multiple targets`() {
        // given
        val fileInTarget1Target2Uri = "file:///file/in/target1/target2"
        val fileInTarget1Target3Uri = "file:///file/in/target1/target3"

        val target1 = BuildTargetIdentifier("//target1")
        val target2 = BuildTargetIdentifier("//target2")
        val target3 = BuildTargetIdentifier("//target3")

        val fileInTarget1Target2Source = SourceItem(fileInTarget1Target2Uri, SourceItemKind.DIRECTORY, false)
        val fileInTarget1Target3Source = SourceItem(fileInTarget1Target3Uri, SourceItemKind.DIRECTORY, false)

        val target1Sources = SourcesItem(target1, listOf(fileInTarget1Target2Source, fileInTarget1Target3Source))
        val target2Sources = SourcesItem(target2, listOf(fileInTarget1Target2Source))
        val target3Sources = SourcesItem(target3, listOf(fileInTarget1Target3Source))

        val sources = listOf(target1Sources, target2Sources, target3Sources)

        // when
        val targetsDetailsForDocumentProvider = TargetsDetailsForDocumentProvider(sources)

        val fileInTarget1Target2Id = TextDocumentIdentifier(fileInTarget1Target2Uri)
        val fileInTarget1Target3Id = TextDocumentIdentifier(fileInTarget1Target3Uri)

        val fileInTarget1Target2TargetsDetails =
          targetsDetailsForDocumentProvider.getTargetsDetailsForDocument(fileInTarget1Target2Id)
        val fileInTarget1Target3TargetsDetails =
          targetsDetailsForDocumentProvider.getTargetsDetailsForDocument(fileInTarget1Target3Id)

        // then
        fileInTarget1Target2TargetsDetails shouldContainExactlyInAnyOrder listOf("//target1", "//target2")
        fileInTarget1Target3TargetsDetails shouldContainExactlyInAnyOrder listOf("//target1", "//target3")
      }
    }

    @Nested
    @DisplayName("tests with directories as sources")
    inner class DirectoryBasedSourcesTests {

      @Test
      fun `should map multiple files in single directory to single target`() {
        // given
        val commonDirectoryUri = "file:///common/directory"

        val commonDirectoryFile1InTarget1Uri = "$commonDirectoryUri/file1/in/target1"
        val commonDirectoryFile2InTarget1Uri = "$commonDirectoryUri/file2/in/target1"

        val target1 = BuildTargetIdentifier("//target1")

        val commonDirectorySource = SourceItem(commonDirectoryUri, SourceItemKind.DIRECTORY, false)

        val target1Sources = SourcesItem(target1, listOf(commonDirectorySource))

        val sources = listOf(target1Sources)

        // when
        val targetsDetailsForDocumentProvider = TargetsDetailsForDocumentProvider(sources)

        val commonDirectoryFile1InTarget1Id = TextDocumentIdentifier(commonDirectoryFile1InTarget1Uri)
        val commonDirectoryFile2InTarget1Id = TextDocumentIdentifier(commonDirectoryFile2InTarget1Uri)

        val commonDirectoryFile1InTarget1TargetsDetails =
          targetsDetailsForDocumentProvider.getTargetsDetailsForDocument(commonDirectoryFile1InTarget1Id)
        val commonDirectoryFile2InTarget1TargetsDetails =
          targetsDetailsForDocumentProvider.getTargetsDetailsForDocument(commonDirectoryFile2InTarget1Id)

        // then
        commonDirectoryFile1InTarget1TargetsDetails shouldBe listOf("//target1")
        commonDirectoryFile2InTarget1TargetsDetails shouldBe listOf("//target1")
      }

      @Test
      fun `should map multiple files in nested directories to multiple targets`() {
        // given
        val commonDirectoryUri = "file:///common/directory"
        val commonDirectoryChildUri = "file:///common/directory/child"

        val commonDirectoryFileInTarget1Uri = "$commonDirectoryUri/file/in/target1"
        val commonDirectoryChildFileInTarget1Target2Uri = "$commonDirectoryChildUri/file/in/target1/target2"

        val target1 = BuildTargetIdentifier("//target1")
        val target2 = BuildTargetIdentifier("//target2")

        val commonDirectorySource = SourceItem(commonDirectoryUri, SourceItemKind.DIRECTORY, false)
        val commonDirectoryChildSource = SourceItem(commonDirectoryChildUri, SourceItemKind.DIRECTORY, false)

        val target1Sources = SourcesItem(target1, listOf(commonDirectorySource))
        val target2Sources = SourcesItem(target2, listOf(commonDirectoryChildSource))

        val sources = listOf(target1Sources, target2Sources)

        // when
        val targetsDetailsForDocumentProvider = TargetsDetailsForDocumentProvider(sources)

        val commonDirectoryFileInTarget1Id = TextDocumentIdentifier(commonDirectoryFileInTarget1Uri)
        val commonDirectoryChildFileInTarget1Target2Id =
          TextDocumentIdentifier(commonDirectoryChildFileInTarget1Target2Uri)

        val commonDirectoryFileInTarget1TargetsDetails =
          targetsDetailsForDocumentProvider.getTargetsDetailsForDocument(commonDirectoryFileInTarget1Id)
        val commonDirectoryChildFileInTarget1Target2TargetsDetails =
          targetsDetailsForDocumentProvider.getTargetsDetailsForDocument(commonDirectoryChildFileInTarget1Target2Id)

        // then
        commonDirectoryFileInTarget1TargetsDetails shouldContainExactlyInAnyOrder listOf("//target1")
        commonDirectoryChildFileInTarget1Target2TargetsDetails shouldContainExactlyInAnyOrder listOf("//target1", "//target2")
      }
    }

    @Nested
    @DisplayName("tests with files and directories as sources")
    inner class FileAndDirectoryBasedSourcesTests {

      @Test
      fun `should map document to no targets for no sources`() {
        // given

        val fileInNoTargetUri = "file:///file/in/no/target"

        val sources = emptyList<SourcesItem>()

        // when
        val targetsDetailsForDocumentProvider = TargetsDetailsForDocumentProvider(sources)

        val fileInNoTargetId = TextDocumentIdentifier(fileInNoTargetUri)

        val fileInNoTargetTargetsDetails =
          targetsDetailsForDocumentProvider.getTargetsDetailsForDocument(fileInNoTargetId)

        // then
        fileInNoTargetTargetsDetails shouldBe emptySet()
      }

      @Test
      fun `should map file based source inside nested directories to multiple targets`() {
        // given
        val commonDirectoryUri = "file:///common/directory"
        val commonDirectoryChildUri = "file:///common/directory/child"

        val fileInNoTargetUri = "file:///file/in/no/target"
        val commonDirectoryFileInTarget1Uri = "$commonDirectoryUri/file1/in/target1"
        val commonDirectoryChildFileInTarget1Target2Target3Uri =
          "$commonDirectoryChildUri/file/in/target1/target2/target3"

        val target1 = BuildTargetIdentifier("//target1")
        val target2 = BuildTargetIdentifier("//target2")
        val target3 = BuildTargetIdentifier("//target3")

        val commonDirectorySource = SourceItem(commonDirectoryUri, SourceItemKind.DIRECTORY, false)
        val commonDirectoryChildSource = SourceItem(commonDirectoryChildUri, SourceItemKind.DIRECTORY, false)
        val commonDirectoryChildFileInTarget1Target2Target3Source =
          SourceItem(commonDirectoryChildFileInTarget1Target2Target3Uri, SourceItemKind.FILE, false)

        val target1Sources = SourcesItem(target1, listOf(commonDirectorySource))
        val target2Sources = SourcesItem(target2, listOf(commonDirectoryChildSource))
        val target3Sources = SourcesItem(target3, listOf(commonDirectoryChildFileInTarget1Target2Target3Source))

        val sources = listOf(target1Sources, target2Sources, target3Sources)

        // when
        val targetsDetailsForDocumentProvider = TargetsDetailsForDocumentProvider(sources)

        val fileInNoTargetId = TextDocumentIdentifier(fileInNoTargetUri)
        val commonDirectoryFileInTarget1Id = TextDocumentIdentifier(commonDirectoryFileInTarget1Uri)
        val commonDirectoryChildFileInTarget1Target2Target3Id =
          TextDocumentIdentifier(commonDirectoryChildFileInTarget1Target2Target3Uri)

        val fileInNoTargetTargetsDetails =
          targetsDetailsForDocumentProvider.getTargetsDetailsForDocument(fileInNoTargetId)
        val commonDirectoryFileInTarget1TargetsDetails =
          targetsDetailsForDocumentProvider.getTargetsDetailsForDocument(commonDirectoryFileInTarget1Id)
        val commonDirectoryChildFileInTarget1Target2Target3Targets =
          targetsDetailsForDocumentProvider.getTargetsDetailsForDocument(
            commonDirectoryChildFileInTarget1Target2Target3Id
          )

        // then
        fileInNoTargetTargetsDetails shouldBe emptySet()
        commonDirectoryFileInTarget1TargetsDetails shouldContainExactlyInAnyOrder listOf("//target1")
        commonDirectoryChildFileInTarget1Target2Target3Targets shouldContainExactlyInAnyOrder listOf(
          "//target1",
          "//target2",
          "//target3"
        )
      }
    }
  }
}
