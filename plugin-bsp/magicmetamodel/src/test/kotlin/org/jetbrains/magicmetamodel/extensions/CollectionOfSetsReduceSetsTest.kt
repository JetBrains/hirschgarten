package org.jetbrains.magicmetamodel.extensions

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("collectionOfSets.reduceSets() tests")
class CollectionOfSetsReduceSetsTest {

  @Test
  fun `should reduce empty collection`() {
    // given
    val setsToReduce = emptyList<Set<Int>>()

    // when
    val reducedSet = setsToReduce.reduceSets()

    // then
    reducedSet shouldBe emptySet()
  }

  @Test
  fun `should reduce collection of empty sets`() {
    // given
    val setsToReduce = listOf(emptySet<Int>())

    // when
    val reducedSet = setsToReduce.reduceSets()

    // then
    reducedSet shouldBe emptySet()
  }

  @Test
  fun `should reduce collection of sets`() {
    // given
    val setsToReduce = listOf(setOf(1, 2), setOf(3, 4), setOf(1, 7), setOf(3, 3))

    // when
    val reducedSet = setsToReduce.reduceSets()

    // then
    reducedSet shouldContainExactly setOf(1, 2, 3, 4, 7)
  }
}
