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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId as WMBuildTargetId
import java.util.concurrent.TimeUnit

@DisplayName("OverlappingTargetsGraph(targetsDetailsForDocumentProvider) tests")
class OverlappingTargetsGraphTest {

  @Test
  fun `should return empty graph for no targets`() {
    // given
    val targetsDetailsForDocumentProvider = TargetsDetailsForDocumentProvider(emptyList())

    // when
    val overlappingTargetsGraph = OverlappingTargetsGraph(targetsDetailsForDocumentProvider)

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
    val overlappingTargetsGraph = OverlappingTargetsGraph(targetsDetailsForDocumentProvider)

    // then
    val expectedGraph = mapOf<WMBuildTargetId, Set<WMBuildTargetId>>(
      "targetA1" to setOf(),
      "targetB1" to setOf(),
      "targetC1" to setOf(),
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
    val overlappingTargetsGraph = OverlappingTargetsGraph(targetsDetailsForDocumentProvider)

    // then
    val expectedGraph = mapOf(
      "targetA1" to setOf("targetA2"),
      "targetA2" to setOf("targetA1"),
    )

    overlappingTargetsGraph shouldContainExactly expectedGraph
  }

  @Test
  fun `should return graph with 0 edges for 2 files in the same directory`() {
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
    val overlappingTargetsGraph = OverlappingTargetsGraph(targetsDetailsForDocumentProvider)

    // then
    val expectedGraph = mapOf<WMBuildTargetId, Set<WMBuildTargetId>>(
      "targetA1" to emptySet(),
      "targetA2" to emptySet(),
    )

    overlappingTargetsGraph shouldContainExactly expectedGraph
  }

  @Test
  fun `should return graph with no edges for 2 files in the directory and subdirectory`() {
    // given
    val targetA1Source1 = SourceItem(
      uri = "file:///project/src/main/kotlin/File1.kt",
      kind = SourceItemKind.FILE,
    )
    val targetA1Sources = SourcesItem(
      target = BuildTargetId("targetA1"),
      sources = listOf(targetA1Source1)
    )

    val targetB1Source1 = SourceItem(
      uri = "file:///project/src/main/kotlin/subdirectory/File1.kt",
      kind = SourceItemKind.FILE,
    )
    val targetB1Sources = SourcesItem(
      target = BuildTargetId("targetB1"),
      sources = listOf(targetB1Source1),
    )

    val sources = listOf(targetA1Sources, targetB1Sources)

    val targetsDetailsForDocumentProvider = TargetsDetailsForDocumentProvider(sources)

    // when
    val overlappingTargetsGraph = OverlappingTargetsGraph(targetsDetailsForDocumentProvider)

    // then
    val expectedGraph = mapOf<WMBuildTargetId, Set<WMBuildTargetId>>(
      "targetA1" to emptySet(),
      "targetB1" to emptySet()
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
    val overlappingTargetsGraph = OverlappingTargetsGraph(targetsDetailsForDocumentProvider)

    // then
    val expectedGraph = mapOf(
      "targetA1" to setOf("targetA2B2C1", "targetA3B3C2"),
      "targetB1" to setOf("targetA2B2C1", "targetA3B3C2"),
      "targetA2B2C1" to setOf("targetA1", "targetB1", "targetA3B3C2", "targetC3D1"),
      "targetA3B3C2" to setOf("targetA1", "targetB1", "targetA2B2C1", "targetC3D1"),
      "targetC3D1" to setOf("targetA2B2C1", "targetA3B3C2", "targetD2E1", "targetD3"),
      "targetD2E1" to setOf("targetC3D1", "targetE2", "targetD3"),
      "targetF1" to setOf(),
      "targetE2" to setOf("targetD2E1"),
      "targetD3" to setOf("targetC3D1", "targetD2E1"),
    )

    overlappingTargetsGraph shouldContainExactly expectedGraph
  }

  @Nested
  @DisplayName("performance tests")
  inner class PerformanceTests {

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    fun `should finish before timeout for project without shared sources (each target has 1 source file)`() {
      // given
      val numberOfTargets = 100_000

      val sources = (0..numberOfTargets).toList().map {
        SourcesItem(
          target = BuildTargetIdentifier("target$it"),
          sources = listOf(
            SourceItem(
              uri = "file:///project/target$it/src/main/kotlin/File.kt",
              kind = SourceItemKind.FILE,
            )
          ),
        )
      }

      val targetsDetailsForDocumentProvider = TargetsDetailsForDocumentProvider(sources)

      // when
      val overlappingTargetsGraph = OverlappingTargetsGraph(targetsDetailsForDocumentProvider)

      // then
      val expectedOverlappingTargetsGraph =
        sources
          .map { it.target.uri }
          .associateWith { emptySet<BuildTargetIdentifier>() }
      overlappingTargetsGraph shouldBe expectedOverlappingTargetsGraph
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    fun `should finish before timeout for project with all targets sharing 1 source`() {
      // given
      val numberOfTargets = 1_000

      val sources = (0..numberOfTargets).map {
        SourcesItem(
          target = BuildTargetIdentifier("target$it"),
          sources = listOf(
            SourceItem(
              uri = "file:///project/target$it/src/main/kotlin/File.kt",
              kind = SourceItemKind.FILE,
            ),
            SourceItem(
              uri = "file:///project/target/src/main/kotlin/File.kt",
              kind = SourceItemKind.FILE,
            )
          ),
        )
      }

      val targetsDetailsForDocumentProvider = TargetsDetailsForDocumentProvider(sources)

      // when
      val overlappingTargetsGraph = OverlappingTargetsGraph(targetsDetailsForDocumentProvider)

      // then
      val expectedOverlappingTargetsGraph =
        sources
          .map { it.target.uri }
          .associateWith { sources.map { it.target.uri }.filter { t -> t != it } }
      overlappingTargetsGraph shouldBe expectedOverlappingTargetsGraph
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    fun `should finish before timeout for project where each 2 targets share source`() {
      // given
      val numberOfTargets = 100_000

      val sources = (0..numberOfTargets).toList().zipWithNext().map {
        SourcesItem(
          target = BuildTargetIdentifier("target${it.first}"),
          sources = listOf(
            SourceItem(
              uri = "file:///project/target${it.first}/src/main/kotlin/File.kt",
              kind = SourceItemKind.FILE,
            ),
            SourceItem(
              uri = "file:///project/target${it.second}/src/main/kotlin/File.kt",
              kind = SourceItemKind.FILE,
            )
          ),
        )
      }

      val targetsDetailsForDocumentProvider = TargetsDetailsForDocumentProvider(sources)

      // when
      val overlappingTargetsGraph = OverlappingTargetsGraph(targetsDetailsForDocumentProvider)

      // then
      val expectedOverlappingTargetsGraph =
        sources.zipWithNext().zipWithNext()
          .associateBy({ it.first.second.target.uri }, { setOf(it.first.first.target.uri, it.second.second.target.uri) }) +
          setOf(
            sources[0].target.uri to setOf(sources[1].target.uri),
            sources[sources.lastIndex].target.uri to setOf(sources[sources.lastIndex - 1].target.uri)
          )
      overlappingTargetsGraph shouldBe expectedOverlappingTargetsGraph
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    fun `should finish before timeout for project where all targets share 1 source and each target has lots of sources`() {
      // given
      val numberOfTargets = 100
      val numberOfSourcesPerTarget = 1_000

      val sources = (0..numberOfTargets).map {
        SourcesItem(
          target = BuildTargetIdentifier("target$it"),
          sources = (0..numberOfSourcesPerTarget).map { rand ->
            SourceItem(
              uri = "file:///project/target$it$rand/src/main/kotlin/File.kt",
              kind = SourceItemKind.FILE,
            )
          } + SourceItem(
            uri = "file:///project/target/src/main/kotlin/File.kt",
            kind = SourceItemKind.FILE,
          )
        )
      }

      val targetsDetailsForDocumentProvider = TargetsDetailsForDocumentProvider(sources)

      // when
      val overlappingTargetsGraph = OverlappingTargetsGraph(targetsDetailsForDocumentProvider)

      // then
      val expectedOverlappingTargetsGraph =
        sources
          .map { it.target.uri }
          .associateWith { sources.map { it.target.uri }.filter { t -> t != it } }
      overlappingTargetsGraph shouldBe expectedOverlappingTargetsGraph
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    fun `should finish before timeout for project where all targets share all sources`() {
      // given
      val numberOfTargets = 1_000
      val numberOfSharedSources = 1_000

      val sources = (0..numberOfTargets).map {
        SourcesItem(
          target = BuildTargetIdentifier("target$it"),
          sources = (0..numberOfSharedSources).map { rand ->
            SourceItem(
              uri = "file:///project/target$rand/src/main/kotlin/File.kt",
              kind = SourceItemKind.FILE,
            )
          }
        )
      }

      val targetsDetailsForDocumentProvider = TargetsDetailsForDocumentProvider(sources)

      // when
      val overlappingTargetsGraph = OverlappingTargetsGraph(targetsDetailsForDocumentProvider)

      // then
      val expectedOverlappingTargetsGraph =
        sources
          .map { it.target.uri }
          .associateWith { sources.map { it.target.uri }.filter { t -> t != it } }
      overlappingTargetsGraph shouldBe expectedOverlappingTargetsGraph
    }
  }
}
