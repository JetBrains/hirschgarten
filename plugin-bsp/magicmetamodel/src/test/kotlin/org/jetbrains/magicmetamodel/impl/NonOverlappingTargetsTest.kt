@file:Suppress("MaxLineLength")

package org.jetbrains.magicmetamodel.impl

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.workspace.model.constructors.BuildTarget
import org.jetbrains.workspace.model.constructors.BuildTargetId
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("NonOverlappingTargets(overlappingTargetsGraph) tests")
class NonOverlappingTargetsTest {

  @Test
  fun `should return empty set for no targets`() {
    // given
    val allTargets = emptySet<BuildTarget>()
    val overlappingTargetsGraph = mapOf<BuildTargetIdentifier, Set<BuildTargetIdentifier>>()

    // when
    val nonOverlappingTargets = NonOverlappingTargets(allTargets, overlappingTargetsGraph)

    // then
    nonOverlappingTargets shouldBe emptySet()
  }

  @Test
  fun `should return set with all targets for non overlapping targets`() {
    // given
    val targetA1 = BuildTarget(
      id = BuildTargetId("targetA1"),
    )
    val targetB1 = BuildTarget(
      id = BuildTargetId("targetB1"),
      dependencies = listOf(BuildTargetId("targetA1"), BuildTargetId("externalDep")),
    )
    val targetC1 = BuildTarget(
      id = BuildTargetId("targetC1"),
    )
    val targetD1 = BuildTarget(
      id = BuildTargetId("targetD1"),
      dependencies = listOf(BuildTargetId("targetA1"), BuildTargetId("targetC1")),
    )

    val allTargets = setOf(targetA1, targetB1, targetC1, targetD1)
    val overlappingTargetsGraph = mapOf<BuildTargetIdentifier, Set<BuildTargetIdentifier>>(
      BuildTargetId("targetA1") to emptySet(),
      BuildTargetId("targetB1") to emptySet(),
      BuildTargetId("targetC1") to emptySet(),
    )

    // when
    val nonOverlappingTargets = NonOverlappingTargets(allTargets, overlappingTargetsGraph)

    // then
    val expectedTargets = setOf(
      BuildTargetId("targetA1"),
      BuildTargetId("targetB1"),
      BuildTargetId("targetC1"),
      BuildTargetId("targetD1"),
    )
    nonOverlappingTargets shouldContainExactlyInAnyOrder expectedTargets
  }

  @Test
  fun `should return set with non overlapping targets for overlapping targets and one target without sources`() {
    // given
    val targetA1 = BuildTarget(
      id = BuildTargetId("targetA1"),
    )
    val targetA2 = BuildTarget(
      id = BuildTargetId("targetA2"),
      dependencies = listOf(BuildTargetId("externalDep")),
    )
    val targetB1 = BuildTarget(
      id = BuildTargetId("targetB1"),
      dependencies = listOf(BuildTargetId("targetA1"))
    )
    val targetB2 = BuildTarget(
      id = BuildTargetId("targetB2"),
      dependencies = listOf(BuildTargetId("targetA1"), BuildTargetId("externalDep")),
    )
    val targetB3 = BuildTarget(
      id = BuildTargetId("targetB3"),
    )
    val targetC1 = BuildTarget(
      id = BuildTargetId("targetC1"),
      dependencies = listOf(
        BuildTargetId("targetA1"),
        BuildTargetId("targetA2"),
        BuildTargetId("externalDep")
      ),
    )

    val allTargets = setOf(targetA1, targetA2, targetB1, targetB2, targetB3, targetC1)
    val overlappingTargetsGraph = mapOf<BuildTargetIdentifier, Set<BuildTargetIdentifier>>(
      BuildTargetId("targetA1") to setOf(BuildTargetId("targetA2")),
      BuildTargetId("targetA2") to setOf(BuildTargetId("targetA1")),
      BuildTargetId("targetB1") to setOf(BuildTargetId("targetB2"), BuildTargetId("targetB3")),
      BuildTargetId("targetB2") to setOf(BuildTargetId("targetB1"), BuildTargetId("targetB3")),
      BuildTargetId("targetB3") to setOf(BuildTargetId("targetB1"), BuildTargetId("targetB2")),
    )

    // when
    val nonOverlappingTargets = NonOverlappingTargets(allTargets, overlappingTargetsGraph)

    // then
    val expectedTargets = setOf(
      BuildTargetId("targetA1"),
      BuildTargetId("targetB1"),
      BuildTargetId("targetC1")
    )
    nonOverlappingTargets shouldContainExactlyInAnyOrder expectedTargets
  }

  @Test
  fun `should return set with non overlapping targets for overlapping targets and non overlapping targets and one target without sources`() {
    // given
    val targetA1 = BuildTarget(
      id = BuildTargetId("targetA1")
    )
    val targetA2B1 = BuildTarget(
      id = BuildTargetId("targetA2B1")
    )
    val targetB2 = BuildTarget(
      id = BuildTargetId("targetB2"),
      dependencies = listOf(BuildTargetId("targetA1"), BuildTargetId("externalDep"))
    )
    val targetC1 = BuildTarget(
      id = BuildTargetId("targetC1"),
      dependencies = listOf(BuildTargetId("//target1"), BuildTargetId("externalDep"))
    )
    val targetD1 = BuildTarget(
      id = BuildTargetId("targetD1"),
      dependencies = listOf(BuildTargetId("targetC1"))
    )
    val targetE1 = BuildTarget(
      id = BuildTargetId("targetE1")
    )

    val allTargets = setOf(targetA1, targetA2B1, targetB2, targetC1, targetD1, targetE1)
    val overlappingTargetsGraph = mapOf<BuildTargetIdentifier, Set<BuildTargetIdentifier>>(
      BuildTargetId("targetA1") to setOf(BuildTargetId("targetA2B1")),
      BuildTargetId("targetA2B1") to setOf(
        BuildTargetId("targetA1"),
        BuildTargetId("targetB2")
      ),
      BuildTargetId("targetB2") to setOf(BuildTargetId("targetA2B1")),
      BuildTargetId("targetC1") to emptySet(),
      BuildTargetId("targetD1") to emptySet(),
    )

    // when
    val nonOverlappingTargets = NonOverlappingTargets(allTargets, overlappingTargetsGraph)

    // then
    val expectedTargets = setOf(
      BuildTargetId("targetA1"),
      BuildTargetId("targetB2"),
      BuildTargetId("targetC1"),
      BuildTargetId("targetD1"),
      BuildTargetId("targetE1")
    )
    nonOverlappingTargets shouldContainExactlyInAnyOrder expectedTargets
  }

  @Test
  fun `should return set of non overlapping dependent targets for overlapping targets dependent on each other`() {
    // given
    val targetA1 = BuildTarget(
      id = BuildTargetId("targetA1"),
      dependencies = listOf(BuildTargetId("targetB1"), BuildTargetId("externalDep")),
    )
    val targetA2 = BuildTarget(
      id = BuildTargetId("targetA2"),
      dependencies = listOf(BuildTargetId("targetB2"), BuildTargetId("externalDep")),
    )
    val targetB1 = BuildTarget(
      id = BuildTargetId("targetB1"),
      dependencies = listOf(BuildTargetId("targetC1"))
    )
    val targetB2 = BuildTarget(
      id = BuildTargetId("targetB2"),
      dependencies = listOf(BuildTargetId("targetC2"))
    )
    val targetC1 = BuildTarget(
      id = BuildTargetId("targetC1"),
      dependencies = listOf(BuildTargetId("targetD1"))
    )
    val targetC2 = BuildTarget(
      id = BuildTargetId("targetC2"),
      dependencies = listOf(BuildTargetId("targetD2"))
    )
    val targetD1 = BuildTarget(
      id = BuildTargetId("targetD1"),
      dependencies = listOf(BuildTargetId("targetE1"))
    )
    val targetD2 = BuildTarget(
      id = BuildTargetId("targetD2"),
      dependencies = listOf(BuildTargetId("targetE1"))
    )
    val targetE1 = BuildTarget(
      id = BuildTargetId("targetE1"),
    )

    val allTargets = setOf(targetA1, targetE1, targetD2, targetD1, targetB2, targetC1, targetB1, targetA2, targetC2)
    val overlappingTargetsGraph = mapOf<BuildTargetIdentifier, Set<BuildTargetIdentifier>>(
      BuildTargetId("targetA1") to setOf(BuildTargetId("targetA2")),
      BuildTargetId("targetA2") to setOf(BuildTargetId("targetA1")),
      BuildTargetId("targetB1") to setOf(BuildTargetId("targetB2")),
      BuildTargetId("targetB2") to setOf(BuildTargetId("targetB1")),
      BuildTargetId("targetC1") to setOf(BuildTargetId("targetC2")),
      BuildTargetId("targetC2") to setOf(BuildTargetId("targetC1")),
      BuildTargetId("targetD1") to setOf(BuildTargetId("targetD2")),
      BuildTargetId("targetD2") to setOf(BuildTargetId("targetD1")),
      BuildTargetId("targetE1") to emptySet()
    )

    // when
    val nonOverlappingTargets = NonOverlappingTargets(allTargets, overlappingTargetsGraph)

    // then
    val expectedTargets = setOf(
      BuildTargetId("targetA1"),
      BuildTargetId("targetB1"),
      BuildTargetId("targetC1"),
      BuildTargetId("targetD1"),
      BuildTargetId("targetE1"),
    )
    nonOverlappingTargets shouldContainExactlyInAnyOrder expectedTargets
  }

  @Test
  fun cycleInGraph() {
    val targetA = BuildTarget(
      id = BuildTargetId("targetA"),
      dependencies = listOf(BuildTargetId("targetB"))
    )

    val targetB = BuildTarget(
      id = BuildTargetId("targetB"),
      dependencies = listOf(BuildTargetId("targetA"))
    )

    val allTargets = setOf(targetA, targetB)

    val nonOverlappingTargets = NonOverlappingTargets(allTargets, overlappingTargetsGraph = mapOf())
    val expectedTargets = setOf(BuildTargetId("targetA"), BuildTargetId("targetB"))
    nonOverlappingTargets shouldContainExactlyInAnyOrder expectedTargets
  }
}
