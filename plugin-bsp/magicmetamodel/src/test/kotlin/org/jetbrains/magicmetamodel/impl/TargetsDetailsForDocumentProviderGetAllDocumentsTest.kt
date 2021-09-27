package org.jetbrains.magicmetamodel.impl

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.SourceItem
import ch.epfl.scala.bsp4j.SourceItemKind
import ch.epfl.scala.bsp4j.SourcesItem
import ch.epfl.scala.bsp4j.TextDocumentIdentifier
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("targetsDetailsForDocumentProvider.getAllDocumentsTest() tests")
class TargetsDetailsForDocumentProviderGetAllDocumentsTest {

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

    val file1InTarget1Source = SourceItem(file1InTarget1Uri, SourceItemKind.FILE, false)
    val file2InTarget1Source = SourceItem(file2InTarget1Uri, SourceItemKind.FILE, false)
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

    val fileInTarget1Target2Source = SourceItem(fileInTarget1Target2Uri, SourceItemKind.FILE, false)

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
