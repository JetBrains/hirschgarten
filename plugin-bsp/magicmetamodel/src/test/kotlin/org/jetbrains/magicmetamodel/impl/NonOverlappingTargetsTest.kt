@file:Suppress("MaxLineLength")
package org.jetbrains.magicmetamodel.impl

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.collections.shouldNotBeIn
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("NonOverlappingTargetsDelegate(overlappingTargetsGraph) tests")
class NonOverlappingTargetsTest {

  @Test
  fun `should return empty set for no targets`() {
    // given
    val allTargets = emptyList<BuildTargetIdentifier>()
    val overlappingTargetsGraph = mapOf<BuildTargetIdentifier, Set<BuildTargetIdentifier>>()

    // when
    val nonOverlappingTargets by NonOverlappingTargetsDelegate(allTargets, overlappingTargetsGraph)

    // then
    nonOverlappingTargets shouldBe emptySet()
  }

  @Test
  fun `should return set with all targets for non overlapping targets`() {
    // given
    val target1 = BuildTargetIdentifier("//target1")
    val target2 = BuildTargetIdentifier("//target2")
    val target3 = BuildTargetIdentifier("//target3")
    val target4 = BuildTargetIdentifier("//target4")

    val allTargets = listOf(target1, target2, target3, target4)
    val overlappingTargetsGraph = mapOf<BuildTargetIdentifier, Set<BuildTargetIdentifier>>(
      target1 to emptySet(),
      target2 to emptySet(),
      target3 to emptySet(),
    )

    // when
    val nonOverlappingTargets by NonOverlappingTargetsDelegate(allTargets, overlappingTargetsGraph)

    // then
    val expectedTargets = setOf(target1, target2, target3, target4)

    nonOverlappingTargets shouldBe expectedTargets
  }

  @Test
  fun `should return set with non overlapping targets for overlapping targets and one target without sources`() {
    // given
    val target1 = BuildTargetIdentifier("//target1")
    val target2 = BuildTargetIdentifier("//target2")
    val target3 = BuildTargetIdentifier("//target3")
    val target4 = BuildTargetIdentifier("//target4")
    val target5 = BuildTargetIdentifier("//target5")
    val target6 = BuildTargetIdentifier("//target6")

    val allTargets = listOf(target1, target2, target3, target4, target5, target6)
    val overlappingTargetsGraph = mapOf(
      target1 to setOf(target2),
      target2 to setOf(target1),
      target3 to setOf(target4, target5),
      target4 to setOf(target3, target5),
      target5 to setOf(target3, target4),
    )

    // when
    val nonOverlappingTargets by NonOverlappingTargetsDelegate(allTargets, overlappingTargetsGraph)

    // then
    nonOverlappingTargets shouldHaveAtLeastSize 3
    validateNonOverlappingTargetsByCheckingGraph(nonOverlappingTargets, overlappingTargetsGraph)
  }

  @Test
  fun `should return set with non overlapping targets for overlapping targets and non overlapping targets and one target without sources`() {
    // given
    val target1 = BuildTargetIdentifier("//target1")
    val target2 = BuildTargetIdentifier("//target2")
    val target3 = BuildTargetIdentifier("//target3")
    val target4 = BuildTargetIdentifier("//target4")
    val target5 = BuildTargetIdentifier("//target5")
    val target6 = BuildTargetIdentifier("//target6")

    val allTargets = listOf(target1, target2, target3, target4, target5, target6)
    val overlappingTargetsGraph = mapOf(
      target1 to setOf(target2),
      target2 to setOf(target1, target3),
      target3 to setOf(target2),
      target4 to emptySet(),
      target5 to emptySet(),
    )

    // when
    val nonOverlappingTargets by NonOverlappingTargetsDelegate(allTargets, overlappingTargetsGraph)

    // then
    nonOverlappingTargets shouldHaveAtLeastSize 4
    validateNonOverlappingTargetsByCheckingGraph(nonOverlappingTargets, overlappingTargetsGraph)
  }

  private fun validateNonOverlappingTargetsByCheckingGraph(
    nonOverlappingTargets: Set<BuildTargetIdentifier>,
    graph: Map<BuildTargetIdentifier, Set<BuildTargetIdentifier>>,
  ) = nonOverlappingTargets.forEach { validateNonOverlappingTargetByCheckingGraph(it, nonOverlappingTargets, graph) }

  private fun validateNonOverlappingTargetByCheckingGraph(
    targetToValidate: BuildTargetIdentifier,
    nonOverlappingTargets: Set<BuildTargetIdentifier>,
    graph: Map<BuildTargetIdentifier, Set<BuildTargetIdentifier>>,
  ) {
    val overlappingTargetsWithTargetToValidate = graph[targetToValidate] ?: emptySet()

    overlappingTargetsWithTargetToValidate.forEach { it shouldNotBeIn nonOverlappingTargets }
  }
}
