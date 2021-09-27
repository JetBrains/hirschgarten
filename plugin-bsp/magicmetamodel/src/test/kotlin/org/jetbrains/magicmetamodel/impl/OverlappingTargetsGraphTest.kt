@file:Suppress("LongMethod")

package org.jetbrains.magicmetamodel.impl

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.SourceItem
import ch.epfl.scala.bsp4j.SourceItemKind
import ch.epfl.scala.bsp4j.SourcesItem
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("OverlappingTargetsGraphDelegate(targetsDetailsForDocumentProvider) tests")
class OverlappingTargetsGraphTest {

  @Test
  fun `should return empty graph for no targets`() {
    // given
    val targetsDetailsForDocumentProvider = TargetsDetailsForDocumentProvider(emptyList())

    // when
    val overlappingTargetsGraph by OverlappingTargetsGraphDelegate(targetsDetailsForDocumentProvider)

    // then
    overlappingTargetsGraph shouldBe mapOf()
  }

  @Test
  fun `should return graph without edges for non-overlapping targets`() {
    // given
    val target1Id = BuildTargetIdentifier("//target1")
    val source1InTarget1 = SourceItem("file:///file1/in/target1", SourceItemKind.FILE, false)
    val source2InTarget1 = SourceItem("file:///file2/in/target1", SourceItemKind.FILE, false)
    val target1Sources = SourcesItem(target1Id, listOf(source1InTarget1, source2InTarget1))

    val target2Id = BuildTargetIdentifier("//target2")
    val source1InTarget2 = SourceItem("file:///dir1/in/target2/", SourceItemKind.DIRECTORY, false)
    val target2Sources = SourcesItem(target2Id, listOf(source1InTarget2))

    val target3Id = BuildTargetIdentifier("//target3")
    val source1InTarget3 = SourceItem("file:///file1/in/target3", SourceItemKind.FILE, false)
    val source2InTarget3 = SourceItem("file:///dir2/in/target3", SourceItemKind.DIRECTORY, false)
    val target3Sources = SourcesItem(target3Id, listOf(source1InTarget3, source2InTarget3))

    val sources = listOf(target1Sources, target2Sources, target3Sources)

    val targetsDetailsForDocumentProvider = TargetsDetailsForDocumentProvider(sources)

    // when
    val overlappingTargetsGraph by OverlappingTargetsGraphDelegate(targetsDetailsForDocumentProvider)

    // then
    val expectedGraph = mapOf<BuildTargetIdentifier, Set<BuildTargetIdentifier>>(
      target1Id to setOf(),
      target2Id to setOf(),
      target3Id to setOf(),
    )

    overlappingTargetsGraph shouldContainExactly expectedGraph
  }

  @Test
  fun `should return graph with 2 edges for 2 overlapping targets`() {
    // given
    val overlappingSource1InTarget1Target2 =
      SourceItem("file:///overlapping/file1/in/target1/target2", SourceItemKind.FILE, false)

    val target1Id = BuildTargetIdentifier("//target1")
    val source1InTarget1 = SourceItem("file:///file1/in/target1", SourceItemKind.FILE, false)
    val target1Sources = SourcesItem(target1Id, listOf(source1InTarget1, overlappingSource1InTarget1Target2))

    val target2Id = BuildTargetIdentifier("//target2")
    val source1InTarget2 = SourceItem("file:///dir1/in/target2/", SourceItemKind.DIRECTORY, false)
    val target2Sources = SourcesItem(target2Id, listOf(source1InTarget2, overlappingSource1InTarget1Target2))

    val sources = listOf(target1Sources, target2Sources)

    val targetsDetailsForDocumentProvider = TargetsDetailsForDocumentProvider(sources)

    // when
    val overlappingTargetsGraph by OverlappingTargetsGraphDelegate(targetsDetailsForDocumentProvider)

    // then
    val expectedGraph = mapOf(
      target1Id to setOf(target2Id),
      target2Id to setOf(target1Id),
    )

    overlappingTargetsGraph shouldContainExactly expectedGraph
  }

  @Test
  fun `should return graph for overlapping targets in a complex way`() {
    // given
    val overlappingDirInTarget1Target2Target3Target4Target5Uri =
      "file:///overlapping/dir/in/target1/target2/target3/target4/target5/src"
    val overlappingDirInTarget1Target2Target3Target4Uri =
      "$overlappingDirInTarget1Target2Target3Target4Target5Uri/dir/in/target1/target2/target3/target4"

    val overlappingSource1InTarget3Target4Target5 =
      SourceItem(
        "$overlappingDirInTarget1Target2Target3Target4Target5Uri/dir1/in/target3/target4/target5/",
        SourceItemKind.DIRECTORY,
        false
      )
    val overlappingSource1InTarget5Target6Target9 =
      SourceItem("file:///overlapping/file1/in/target5/target6/target9", SourceItemKind.FILE, false)
    val overlappingSource1InTarget6Target8 =
      SourceItem("file:///overlapping/file1/in/target6/target8", SourceItemKind.FILE, false)

    val target1Id = BuildTargetIdentifier("//target1")
    val source1InTarget1 =
      SourceItem("$overlappingDirInTarget1Target2Target3Target4Target5Uri/file1/in/target1", SourceItemKind.FILE, false)
    val source2InTarget1 =
      SourceItem("$overlappingDirInTarget1Target2Target3Target4Uri/file2/in/target1", SourceItemKind.FILE, false)
    val source3InTarget1 =
      SourceItem("$overlappingDirInTarget1Target2Target3Target4Uri/file3/in/target1", SourceItemKind.FILE, false)
    val target1SourcesList = listOf(source1InTarget1, source2InTarget1, source3InTarget1)
    val target1Sources = SourcesItem(target1Id, target1SourcesList)

    val target2Id = BuildTargetIdentifier("//target2")
    val source1InTarget2 =
      SourceItem("$overlappingDirInTarget1Target2Target3Target4Uri/dir1/in/target2", SourceItemKind.DIRECTORY, false)
    val target2SourcesList = listOf(source1InTarget2)
    val target2Sources = SourcesItem(target2Id, target2SourcesList)

    val target3Id = BuildTargetIdentifier("//target3")
    val source1InTarget3 =
      SourceItem(overlappingDirInTarget1Target2Target3Target4Uri, SourceItemKind.DIRECTORY, false)
    val target3SourcesList = listOf(source1InTarget3, overlappingSource1InTarget3Target4Target5)
    val target3Sources = SourcesItem(target3Id, target3SourcesList)

    val target4Id = BuildTargetIdentifier("//target4")
    val source1InTarget4 =
      SourceItem(overlappingDirInTarget1Target2Target3Target4Target5Uri, SourceItemKind.DIRECTORY, false)
    val target4SourcesList = listOf(source1InTarget4)
    val target4Sources = SourcesItem(target4Id, target4SourcesList)

    val target5Id = BuildTargetIdentifier("//target5")
    val source1InTarget5 = SourceItem("file:///dir1/in/target5/", SourceItemKind.DIRECTORY, false)
    val target5SourcesList =
      listOf(source1InTarget5, overlappingSource1InTarget3Target4Target5, overlappingSource1InTarget5Target6Target9)
    val target5Sources = SourcesItem(target5Id, target5SourcesList)

    val target6Id = BuildTargetIdentifier("//target6")
    val source1InTarget6 = SourceItem("file:///file1/in/target6", SourceItemKind.FILE, false)
    val target6SourcesList =
      listOf(source1InTarget6, overlappingSource1InTarget5Target6Target9, overlappingSource1InTarget6Target8)
    val target6Sources = SourcesItem(target6Id, target6SourcesList)

    val target7Id = BuildTargetIdentifier("//target7")
    val source1InTarget7 = SourceItem("file:///file1/in/target7", SourceItemKind.FILE, false)
    val target7SourcesList = listOf(source1InTarget7)
    val target7Sources = SourcesItem(target7Id, target7SourcesList)

    val target8Id = BuildTargetIdentifier("//target8")
    val source1InTarget8 = SourceItem("file:///file1/in/target8", SourceItemKind.FILE, false)
    val target8SourcesList = listOf(source1InTarget8, overlappingSource1InTarget6Target8)
    val target8Sources = SourcesItem(target8Id, target8SourcesList)

    val target9Id = BuildTargetIdentifier("//target9")
    val source1InTarget9 = SourceItem("file:///file1/in/target9", SourceItemKind.FILE, false)
    val target9SourcesList = listOf(source1InTarget9, overlappingSource1InTarget5Target6Target9)
    val target9Sources = SourcesItem(target9Id, target9SourcesList)

    val sources = listOf(
      target1Sources,
      target2Sources,
      target3Sources,
      target4Sources,
      target5Sources,
      target6Sources,
      target7Sources,
      target8Sources,
      target9Sources,
    )

    val targetsDetailsForDocumentProvider = TargetsDetailsForDocumentProvider(sources)

    // when
    val overlappingTargetsGraph by OverlappingTargetsGraphDelegate(targetsDetailsForDocumentProvider)

    // then
    val expectedGraph = mapOf(
      target1Id to setOf(target3Id, target4Id),
      target2Id to setOf(target3Id, target4Id),
      target3Id to setOf(target1Id, target2Id, target4Id, target5Id),
      target4Id to setOf(target1Id, target2Id, target3Id, target5Id),
      target5Id to setOf(target3Id, target4Id, target6Id, target9Id),
      target6Id to setOf(target5Id, target8Id, target9Id),
      target7Id to setOf(),
      target8Id to setOf(target6Id),
      target9Id to setOf(target5Id, target6Id),
    )

    overlappingTargetsGraph shouldContainExactly expectedGraph
  }
}
