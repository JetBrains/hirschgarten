@file:Suppress("MaxLineLength")

package org.jetbrains.magicmetamodel.impl

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.SourceItem
import ch.epfl.scala.bsp4j.SourceItemKind
import ch.epfl.scala.bsp4j.SourcesItem
import ch.epfl.scala.bsp4j.TextDocumentIdentifier
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("magicMetaModel.getTargetsDetailsForDocument(documentId) tests")
class MagicMetaModelGetTargetsDetailsForDocumentTest {

  @Test
  fun `should return no loaded target and no not loaded targets for not existing document`() {
    // given
    val workspaceModel = WorkspaceModelTestMockImpl()

    val target1Id = BuildTargetIdentifier("//target1")
    val target1 = BuildTarget(
      target1Id,
      emptyList(),
      listOf("kotlin"),
      listOf(
        BuildTargetIdentifier("@maven//:dep1.1"),
        BuildTargetIdentifier("@maven//:dep1.2"),
      ),
      BuildTargetCapabilities(),
    )
    val targets = listOf(target1)

    val source1InTarget1 = SourceItem(
      "file:///file1/in/target1",
      SourceItemKind.FILE,
      false
    )
    val target1Sources = SourcesItem(
      target1Id,
      listOf(source1InTarget1),
    )
    val sources = listOf(target1Sources)

    // when
    val magicMetaModel = MagicMetaModelImpl(workspaceModel, targets, sources)

    val notExistingDocumentId = TextDocumentIdentifier("file:///not/existing/document")

    val targetsDetails = magicMetaModel.getTargetsDetailsForDocument(notExistingDocumentId)

    // then
    targetsDetails.loadedTargetId shouldBe null
    targetsDetails.notLoadedTargetsIds shouldBe emptyList()
  }

  @Test
  fun `should return no loaded target for model without loaded targets`() {
    // given
    val workspaceModel = WorkspaceModelTestMockImpl()

    val target1Id = BuildTargetIdentifier("//target1")
    val target1 = BuildTarget(
      target1Id,
      emptyList(),
      listOf("kotlin"),
      listOf(
        BuildTargetIdentifier("@maven//:dep1.1"),
        BuildTargetIdentifier("@maven//:dep1.2"),
      ),
      BuildTargetCapabilities(),
    )

    val target2Id = BuildTargetIdentifier("//target2")
    val target2 = BuildTarget(
      target2Id,
      emptyList(),
      listOf("kotlin"),
      emptyList(),
      BuildTargetCapabilities(),
    )
    val targets = listOf(target1, target2)

    val sourceInTarget1Target2Uri = "file:///file/in/target1/target2"
    val sourceInTarget1Target2 = SourceItem(
      sourceInTarget1Target2Uri,
      SourceItemKind.FILE,
      false
    )
    val target1Sources = SourcesItem(
      target1Id,
      listOf(sourceInTarget1Target2),
    )
    val target2Sources = SourcesItem(
      target2Id,
      listOf(sourceInTarget1Target2),
    )
    val sources = listOf(target1Sources, target2Sources)

    // when
    val magicMetaModel = MagicMetaModelImpl(workspaceModel, targets, sources)

    val sourceInTarget1Target2Id = TextDocumentIdentifier(sourceInTarget1Target2Uri)

    val targetsDetails = magicMetaModel.getTargetsDetailsForDocument(sourceInTarget1Target2Id)

    // then
    targetsDetails.loadedTargetId shouldBe null
    targetsDetails.notLoadedTargetsIds shouldContainExactlyInAnyOrder listOf(target1Id, target2Id)
  }

  @Test
  fun `should return loaded target for non overlapping targets after loading default targets (all targets)`() {
    // given
    val workspaceModel = WorkspaceModelTestMockImpl()

    val target1Id = BuildTargetIdentifier("//target1")
    val target1 = BuildTarget(
      target1Id,
      emptyList(),
      listOf("kotlin"),
      listOf(
        BuildTargetIdentifier("@maven//:dep1.1"),
        BuildTargetIdentifier("@maven//:dep1.2"),
      ),
      BuildTargetCapabilities(),
    )

    val target2Id = BuildTargetIdentifier("//target2")
    val target2 = BuildTarget(
      target2Id,
      emptyList(),
      listOf("kotlin"),
      emptyList(),
      BuildTargetCapabilities(),
    )
    val targets = listOf(target1, target2)

    val source1InTarget1Uri = "file:///file1/in/target1"
    val source1InTarget1 = SourceItem(
      source1InTarget1Uri,
      SourceItemKind.FILE,
      false
    )
    val target1Sources = SourcesItem(
      target1Id,
      listOf(source1InTarget1),
    )

    val source1InTarget2Uri = "file:///file1/in/target2"
    val source1InTarget2 = SourceItem(
      source1InTarget2Uri,
      SourceItemKind.FILE,
      false
    )
    val target2Sources = SourcesItem(
      target2Id,
      listOf(source1InTarget2),
    )
    val sources = listOf(target1Sources, target2Sources)

    // when
    val magicMetaModel = MagicMetaModelImpl(workspaceModel, targets, sources)
    magicMetaModel.loadDefaultTargets()

    val source1InTarget1Id = TextDocumentIdentifier(source1InTarget1Uri)
    val source1InTarget2Id = TextDocumentIdentifier(source1InTarget2Uri)

    val source1InTarget1TargetsDetails = magicMetaModel.getTargetsDetailsForDocument(source1InTarget1Id)
    val source1InTarget2TargetsDetails = magicMetaModel.getTargetsDetailsForDocument(source1InTarget2Id)

    // then
    source1InTarget1TargetsDetails.loadedTargetId shouldBe target1Id
    source1InTarget1TargetsDetails.notLoadedTargetsIds shouldBe emptyList()

    source1InTarget2TargetsDetails.loadedTargetId shouldBe target2Id
    source1InTarget2TargetsDetails.notLoadedTargetsIds shouldBe emptyList()
  }

  @Test
  fun `should return loaded target for source in loaded target and no loaded target for source in not loaded target for model with overlapping targets`() {
    // given
    val workspaceModel = WorkspaceModelTestMockImpl()

    val target1Id = BuildTargetIdentifier("//target1")
    val target1 = BuildTarget(
      target1Id,
      emptyList(),
      listOf("kotlin"),
      listOf(
        BuildTargetIdentifier("@maven//:dep1.1"),
        BuildTargetIdentifier("@maven//:dep1.2"),
      ),
      BuildTargetCapabilities(),
    )

    val target2Id = BuildTargetIdentifier("//target2")
    val target2 = BuildTarget(
      target2Id,
      emptyList(),
      listOf("kotlin"),
      emptyList(),
      BuildTargetCapabilities(),
    )
    val targets = listOf(target1, target2)

    val overlappingSource1InTarget1Target2Uri = "file:///file/in/target1/target2"
    val source1InTarget1Target2 = SourceItem(
      overlappingSource1InTarget1Target2Uri,
      SourceItemKind.FILE,
      false
    )
    val target1Sources = SourcesItem(
      target1Id,
      listOf(source1InTarget1Target2),
    )

    val source1InTarget2Uri = "file:///file1/in/target2"
    val source1InTarget2 = SourceItem(
      source1InTarget2Uri,
      SourceItemKind.FILE,
      false
    )
    val target2Sources = SourcesItem(
      target2Id,
      listOf(source1InTarget2, source1InTarget1Target2),
    )
    val sources = listOf(target1Sources, target2Sources)

    // when
    val magicMetaModel = MagicMetaModelImpl(workspaceModel, targets, sources)
    magicMetaModel.loadTarget(target1Id)

    val sourceInTarget1Target2Id = TextDocumentIdentifier(overlappingSource1InTarget1Target2Uri)
    val source1InTarget2UriId = TextDocumentIdentifier(source1InTarget2Uri)

    val sourceInTarget1Target2TargetsDetails = magicMetaModel.getTargetsDetailsForDocument(sourceInTarget1Target2Id)
    val source1InTarget2TargetsDetails = magicMetaModel.getTargetsDetailsForDocument(source1InTarget2UriId)

    // then
    sourceInTarget1Target2TargetsDetails.loadedTargetId shouldBe target1Id
    sourceInTarget1Target2TargetsDetails.notLoadedTargetsIds shouldBe listOf(target2Id)

    source1InTarget2TargetsDetails.loadedTargetId shouldBe null
    source1InTarget2TargetsDetails.notLoadedTargetsIds shouldBe listOf(target2Id)
  }
}
