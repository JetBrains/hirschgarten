@file:Suppress("MaxLineLength")

package org.jetbrains.magicmetamodel.impl

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("NonOverlappingTargets(overlappingTargetsGraph) tests")
class NonOverlappingTargetsTest {

  @Test
  fun `should return empty set for no targets`() {
    // given
    val allTargets = emptySet<BuildTargetInfo>()
    val overlappingTargetsGraph = mapOf<BuildTargetId, Set<BuildTargetId>>()

    // when
    val nonOverlappingTargets = NonOverlappingTargets(allTargets, overlappingTargetsGraph)

    // then
    nonOverlappingTargets shouldBe emptySet()
  }

  @Test
  fun `should return set with all targets for non overlapping targets`() {
    // given
    val targetA1 = BuildTargetInfo(
      id = "targetA1",
    )
    val targetB1 = BuildTargetInfo(
      id = "targetB1",
      dependencies = listOf("targetA1", "externalDep"),
    )
    val targetC1 = BuildTargetInfo(
      id = "targetC1",
    )
    val targetD1 = BuildTargetInfo(
      id = "targetD1",
      dependencies = listOf("targetA1", "targetC1"),
    )

    val allTargets = setOf(targetA1, targetB1, targetC1, targetD1)
    val overlappingTargetsGraph = mapOf<BuildTargetId, Set<BuildTargetId>>(
      "targetA1" to emptySet(),
      "targetB1" to emptySet(),
      "targetC1" to emptySet(),
    )

    // when
    val nonOverlappingTargets = NonOverlappingTargets(allTargets, overlappingTargetsGraph)

    // then
    val expectedTargets = setOf(
      "targetA1",
      "targetB1",
      "targetC1",
      "targetD1",
    )
    nonOverlappingTargets shouldContainExactlyInAnyOrder expectedTargets
  }

  @Test
  fun `should return set with non overlapping targets for overlapping targets and one target without sources`() {
    // given
    val targetA1 = BuildTargetInfo(
      id = "targetA1",
    )
    val targetA2 = BuildTargetInfo(
      id = "targetA2",
      dependencies = listOf("externalDep"),
    )
    val targetB1 = BuildTargetInfo(
      id = "targetB1",
      dependencies = listOf("targetA1")
    )
    val targetB2 = BuildTargetInfo(
      id = "targetB2",
      dependencies = listOf("targetA1", "externalDep"),
    )
    val targetB3 = BuildTargetInfo(
      id = "targetB3",
    )
    val targetC1 = BuildTargetInfo(
      id = "targetC1",
      dependencies = listOf(
        "targetA1",
        "targetA2",
        "externalDep"
      ),
    )

    val allTargets = setOf(targetA1, targetA2, targetB1, targetB2, targetB3, targetC1)
    val overlappingTargetsGraph = mapOf<BuildTargetId, Set<BuildTargetId>>(
      "targetA1" to setOf("targetA2"),
      "targetA2" to setOf("targetA1"),
      "targetB1" to setOf("targetB2", "targetB3"),
      "targetB2" to setOf("targetB1", "targetB3"),
      "targetB3" to setOf("targetB1", "targetB2"),
    )

    // when
    val nonOverlappingTargets = NonOverlappingTargets(allTargets, overlappingTargetsGraph)

    // then
    val expectedTargets = setOf(
      "targetA2",
      "targetB3",
      "targetC1",
    )
    nonOverlappingTargets shouldContainExactlyInAnyOrder expectedTargets
  }

  @Test
  fun `should return set with non overlapping targets for overlapping targets and non overlapping targets and one target without sources`() {
    // given
    val targetA1 = BuildTargetInfo(
      id = "targetA1",
    )
    val targetA2B1 = BuildTargetInfo(
      id = "targetA2B1",
    )
    val targetB2 = BuildTargetInfo(
      id = "targetB2",
      dependencies = listOf("targetA1", "externalDep"),
    )
    val targetC1 = BuildTargetInfo(
      id = "targetC1",
      dependencies = listOf("//target1", "externalDep"),
    )
    val targetD1 = BuildTargetInfo(
      id = "targetD1",
      dependencies = listOf("targetC1"),
    )
    val targetE1 = BuildTargetInfo(
      id = "targetE1",
    )

    val allTargets = setOf(targetA1, targetA2B1, targetB2, targetC1, targetD1, targetE1)
    val overlappingTargetsGraph = mapOf<BuildTargetId, Set<BuildTargetId>>(
      "targetA1" to setOf("targetA2B1"),
      "targetA2B1" to setOf(
        "targetA1",
        "targetB2"
      ),
      "targetB2" to setOf("targetA2B1"),
      "targetC1" to emptySet(),
      "targetD1" to emptySet(),
    )

    // when
    val nonOverlappingTargets = NonOverlappingTargets(allTargets, overlappingTargetsGraph)

    // then
    val expectedTargets = setOf(
      "targetA1",
      "targetB2",
      "targetC1",
      "targetD1",
      "targetE1",
    )
    nonOverlappingTargets shouldContainExactlyInAnyOrder expectedTargets
  }

  @Test
  fun `should return set of non overlapping dependent targets for overlapping targets dependent on each other`() {
    // given
    val targetA1 = BuildTargetInfo(
      id = "targetA1",
      dependencies = listOf("targetB1", "externalDep"),
    )
    val targetA2 = BuildTargetInfo(
      id = "targetA2",
      dependencies = listOf("targetB2", "externalDep"),
    )
    val targetB1 = BuildTargetInfo(
      id = "targetB1",
      dependencies = listOf("targetC1")
    )
    val targetB2 = BuildTargetInfo(
      id = "targetB2",
      dependencies = listOf("targetC2")
    )
    val targetC1 = BuildTargetInfo(
      id = "targetC1",
      dependencies = listOf("targetD1")
    )
    val targetC2 = BuildTargetInfo(
      id = "targetC2",
      dependencies = listOf("targetD2")
    )
    val targetD1 = BuildTargetInfo(
      id = "targetD1",
      dependencies = listOf("targetE1")
    )
    val targetD2 = BuildTargetInfo(
      id = "targetD2",
      dependencies = listOf("targetE1")
    )
    val targetE1 = BuildTargetInfo(
      id = "targetE1",
    )

    val allTargets = setOf(targetA1, targetE1, targetD2, targetD1, targetB2, targetC1, targetB1, targetA2, targetC2)
    val overlappingTargetsGraph = mapOf(
      "targetA1" to setOf("targetA2"),
      "targetA2" to setOf("targetA1"),
      "targetB1" to setOf("targetB2"),
      "targetB2" to setOf("targetB1"),
      "targetC1" to setOf("targetC2"),
      "targetC2" to setOf("targetC1"),
      "targetD1" to setOf("targetD2"),
      "targetD2" to setOf("targetD1"),
      "targetE1" to emptySet()
    )

    // when
    val nonOverlappingTargets = NonOverlappingTargets(allTargets, overlappingTargetsGraph)

    // then
    val expectedTargets = setOf(
      "targetA2",
      "targetB2",
      "targetC2",
      "targetD2",
      "targetE1",
    )
    nonOverlappingTargets shouldContainExactlyInAnyOrder expectedTargets
  }

  @Test
  fun cycleInGraph() {
    val targetA = BuildTargetInfo(
      id = "targetA",
      dependencies = listOf("targetB")
    )

    val targetB = BuildTargetInfo(
      id = "targetB",
      dependencies = listOf("targetA")
    )

    val allTargets = setOf(targetA, targetB)

    val nonOverlappingTargets = NonOverlappingTargets(allTargets, conflictGraph = mapOf())
    val expectedTargets = setOf("targetA", "targetB")
    nonOverlappingTargets shouldContainExactlyInAnyOrder expectedTargets
  }
}
