@file:Suppress("LongMethod", "MaxLineLength")

package org.jetbrains.magicmetamodel.impl

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.SourceItemKind
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.jetbrains.workspace.model.constructors.BuildTargetId
import org.jetbrains.workspace.model.constructors.SourceItem
import org.jetbrains.workspace.model.constructors.SourcesItem
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
  fun `should return graph without edges for non-overlapping targets and without target without sources changing files into directories`() {
    // given
    val targetA1Source1 = SourceItem(
      uri = "file:///project/targetA1/src/main/kotlin/File1.kt",
      kind = SourceItemKind.FILE,
    )
    val targetA1Source2 = SourceItem(
      uri = "file:///project/targetA1/src/main/kotlin/File2.kt",
      kind = SourceItemKind.FILE,
    )
    val targetA1Sources = SourcesItem(
      target = BuildTargetId("targetA1"),
      sources = listOf(targetA1Source1, targetA1Source2),
    )

    val targetB1Source1 = SourceItem(
      uri = "file:///project/targetB1/src/main/kotlin/",
      kind = SourceItemKind.DIRECTORY,
    )
    val targetB1Sources = SourcesItem(
      target = BuildTargetId("targetB1"),
      sources = listOf(targetB1Source1),
    )

    val targetC1Source1 = SourceItem(
      uri = "file:///project/targetC1/src/main/kotlin/File1.kt",
      kind = SourceItemKind.FILE,
    )
    val targetC1Source2 = SourceItem(
      uri = "file:///project/targetC1/src/main/kotlin/",
      kind = SourceItemKind.DIRECTORY,
    )
    val targetC1Sources = SourcesItem(
      target = BuildTargetId("targetC1"),
      sources = listOf(targetC1Source1, targetC1Source2),
    )

    val targetD1Sources = SourcesItem(
      target = BuildTargetId("targetD1"),
      sources = emptyList(),
    )

    val sources = listOf(targetA1Sources, targetB1Sources, targetC1Sources, targetD1Sources)

    val targetsDetailsForDocumentProvider = TargetsDetailsForDocumentProvider(sources)

    // when
    val overlappingTargetsGraph by OverlappingTargetsGraphDelegate(targetsDetailsForDocumentProvider)

    // then
    val expectedGraph = mapOf<BuildTargetIdentifier, Set<BuildTargetIdentifier>>(
      BuildTargetId("targetA1") to setOf(),
      BuildTargetId("targetB1") to setOf(),
      BuildTargetId("targetC1") to setOf(),
    )

    overlappingTargetsGraph shouldContainExactly expectedGraph
  }

  @Test
  fun `should return graph with 2 edges for 2 overlapping targets`() {
    // given
    val targetA1A2Source1 =
      SourceItem(
        uri = "file:///project/targetA/src/main/kotlin/File1.kt",
        kind = SourceItemKind.FILE,
      )

    val targetA1Source1 = SourceItem(
      uri = "file:///project/targetA1/src/main/kotlin/File1.kt",
      kind = SourceItemKind.FILE,
    )
    val targetA1Sources = SourcesItem(
      target = BuildTargetId("targetA1"),
      sources = listOf(targetA1Source1, targetA1A2Source1)
    )

    val targetA2Source1 = SourceItem(
      uri = "file:///project/targetA2/src/main/kotlin/",
      kind = SourceItemKind.DIRECTORY,
    )
    val target2Sources = SourcesItem(
      target = BuildTargetId("targetA2"),
      sources = listOf(targetA2Source1, targetA1A2Source1),
    )

    val sources = listOf(targetA1Sources, target2Sources)

    val targetsDetailsForDocumentProvider = TargetsDetailsForDocumentProvider(sources)

    // when
    val overlappingTargetsGraph by OverlappingTargetsGraphDelegate(targetsDetailsForDocumentProvider)

    // then
    val expectedGraph = mapOf<BuildTargetIdentifier, Set<BuildTargetIdentifier>>(
      BuildTargetId("targetA1") to setOf(BuildTargetId("targetA2")),
      BuildTargetId("targetA2") to setOf(BuildTargetId("targetA1")),
    )

    overlappingTargetsGraph shouldContainExactly expectedGraph
  }

  @Test
  fun `should return graph with 2 edges for 2 files in the same directory (INTELLIJ HACK)`() {
    // given
    val targetA1Source1 = SourceItem(
      uri = "file:///project/targetA/src/main/kotlin/File1.kt",
      kind = SourceItemKind.FILE,
    )
    val targetA1Sources = SourcesItem(
      target = BuildTargetId("targetA1"),
      sources = listOf(targetA1Source1)
    )

    val targetA2Source1 = SourceItem(
      uri = "file:///project/targetA/src/main/kotlin/File2.kt",
      kind = SourceItemKind.FILE,
    )
    val target2Sources = SourcesItem(
      target = BuildTargetId("targetA2"),
      sources = listOf(targetA2Source1),
    )

    val sources = listOf(targetA1Sources, target2Sources)

    val targetsDetailsForDocumentProvider = TargetsDetailsForDocumentProvider(sources)

    // when
    val overlappingTargetsGraph by OverlappingTargetsGraphDelegate(targetsDetailsForDocumentProvider)

    // then
    val expectedGraph = mapOf<BuildTargetIdentifier, Set<BuildTargetIdentifier>>(
      BuildTargetId("targetA1") to setOf(BuildTargetId("targetA2")),
      BuildTargetId("targetA2") to setOf(BuildTargetId("targetA1")),
    )

    overlappingTargetsGraph shouldContainExactly expectedGraph
  }

  @Test
  fun `should return graph for overlapping targets in a complex way`() {
    // given
    val targetA1A2A3Source1 = SourceItem(
      uri = "file:///project/targetA/src/main/kotlin/",
      kind = SourceItemKind.DIRECTORY,
    )

    val targetA1Id = BuildTargetIdentifier("targetA1")
    val targetA1Source1 = SourceItem(
      uri = "file:///project/targetA1/src/main/kotlin/File1.kt",
      kind = SourceItemKind.FILE,
    )
    val targetA1Sources = SourcesItem(
      target = targetA1Id,
      sources = listOf(targetA1Source1, targetA1A2A3Source1),
    )

    val targetB1B2B3Source1 = SourceItem(
      uri = "file:///project/targetB/src/main/kotlin/",
      kind = SourceItemKind.DIRECTORY,
    )

    val targetB1Id = BuildTargetIdentifier("targetB1")
    val targetB1Sources = SourcesItem(
      target = targetB1Id,
      sources = listOf(targetB1B2B3Source1),
    )

    val targetC1C2C3Source1 = SourceItem(
      uri = "file:///project/targetC/src/main/kotlin/",
      kind = SourceItemKind.DIRECTORY,
    )

    val targetA2B2C1Id = BuildTargetIdentifier("targetA2B2C1")
    val targetA2B2C1Source1 = SourceItem(
      uri = "file:///project/targetA2B2C1/src/main/kotlin/File1.kt",
      kind = SourceItemKind.FILE,
    )
    val targetA2B2C1Sources = SourcesItem(
      target = targetA2B2C1Id,
      sources = listOf(targetA1A2A3Source1, targetB1B2B3Source1, targetA2B2C1Source1, targetC1C2C3Source1),
    )

    val targetA3B3C2Id = BuildTargetIdentifier("targetA3B3C2")
    val targetA3B3C2Sources = SourcesItem(
      target = targetA3B3C2Id,
      sources = listOf(targetA1A2A3Source1, targetB1B2B3Source1, targetC1C2C3Source1),
    )

    val targetD1D2D3Source1 = SourceItem(
      uri = "file:///project/targetD/src/main/kotlin/",
      kind = SourceItemKind.DIRECTORY,
    )

    val targetC3D1Id = BuildTargetIdentifier("targetC3D1")
    val targetC3D1Source1 = SourceItem(
      uri = "file:///project/targetC3D1/src/main/kotlin/",
      kind = SourceItemKind.DIRECTORY,
    )
    val targetC3D1Sources = SourcesItem(
      target = targetC3D1Id,
      sources = listOf(targetC1C2C3Source1, targetD1D2D3Source1, targetC3D1Source1),
    )

    val targetE1E2Source1 = SourceItem(
      uri = "file:///project/targetE/src/main/kotlin/",
      kind = SourceItemKind.DIRECTORY,
    )

    val targetD2E1Id = BuildTargetIdentifier("targetD2E1")
    val targetD2E1Source1 = SourceItem(
      uri = "file:///project/targetD2E1/src/main/kotlin/File1.kt",
      kind = SourceItemKind.FILE,
    )
    val targetD2E1Sources = SourcesItem(
      target = targetD2E1Id,
      sources = listOf(targetD1D2D3Source1, targetE1E2Source1, targetD2E1Source1),
    )

    val targetF1Id = BuildTargetIdentifier("targetF1")
    val targetF1Source1 = SourceItem(
      uri = "file:///project/targetF1/src/main/kotlin/File1.kt",
      kind = SourceItemKind.FILE,
    )
    val targetF1Sources = SourcesItem(
      target = targetF1Id,
      sources = listOf(targetF1Source1),
    )

    val targetE2Id = BuildTargetIdentifier("targetE2")
    val targetE2Source1 = SourceItem(
      uri = "file:///project/targetE2/src/main/kotlin/File1.kt",
      kind = SourceItemKind.FILE,
    )
    val targetE2Sources = SourcesItem(
      target = targetE2Id,
      sources = listOf(targetE1E2Source1, targetE2Source1),
    )

    val targetD3Id = BuildTargetIdentifier("targetD3")
    val targetD3Source1 = SourceItem(
      uri = "file:///project/targetD3/src/main/kotlin/File1.kt",
      kind = SourceItemKind.FILE,
    )
    val targetD3Sources = SourcesItem(
      target = targetD3Id,
      sources = listOf(targetD1D2D3Source1, targetD3Source1),
    )

    val sources = listOf(
      targetA1Sources,
      targetB1Sources,
      targetA2B2C1Sources,
      targetA3B3C2Sources,
      targetC3D1Sources,
      targetD2E1Sources,
      targetF1Sources,
      targetE2Sources,
      targetD3Sources,
    )

    val targetsDetailsForDocumentProvider = TargetsDetailsForDocumentProvider(sources)

    // when
    val overlappingTargetsGraph by OverlappingTargetsGraphDelegate(targetsDetailsForDocumentProvider)

    // then
    val expectedGraph = mapOf(
      targetA1Id to setOf(targetA2B2C1Id, targetA3B3C2Id),
      targetB1Id to setOf(targetA2B2C1Id, targetA3B3C2Id),
      targetA2B2C1Id to setOf(targetA1Id, targetB1Id, targetA3B3C2Id, targetC3D1Id),
      targetA3B3C2Id to setOf(targetA1Id, targetB1Id, targetA2B2C1Id, targetC3D1Id),
      targetC3D1Id to setOf(targetA2B2C1Id, targetA3B3C2Id, targetD2E1Id, targetD3Id),
      targetD2E1Id to setOf(targetC3D1Id, targetE2Id, targetD3Id),
      targetF1Id to setOf(),
      targetE2Id to setOf(targetD2E1Id),
      targetD3Id to setOf(targetC3D1Id, targetD2E1Id),
    )

    overlappingTargetsGraph shouldContainExactly expectedGraph
  }
}
