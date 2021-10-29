package org.jetbrains.workspace.model.matchers

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("actualValues.shouldContainExactlyInAnyOrder(assertion, expectedValues) tests")
class ShouldContainExactlyInAnyOrderTest {

  @Test
  fun `should pass for empty collections`() {
    // given
    val actualValues = emptyList<Int>()
    val expectedValues = emptyList<String>()

    //  when & then
    shouldNotThrow<AssertionError> {
      actualValues.shouldContainExactlyInAnyOrder(::shouldBeTheSame, expectedValues)
    }
  }

  @Test
  fun `should fail for collections with different sizes`() {
    // given
    val actualValues = listOf(1, 3)
    val expectedValues = listOf("1")

    //  when & then
    shouldThrow<AssertionError> {
      actualValues.shouldContainExactlyInAnyOrder(::shouldBeTheSame, expectedValues)
    }
  }

  @Test
  fun `should pass for equal collections in the same order`() {
    // given
    val actualValues = listOf(1, 9, 4)
    val expectedValues = listOf("1", "9", "4")

    //  when & then
    shouldNotThrow<AssertionError> {
      actualValues.shouldContainExactlyInAnyOrder(::shouldBeTheSame, expectedValues)
    }
  }

  @Test
  fun `should pass for equal collections in any order`() {
    // given
    val actualValues = listOf(4, 1, 9, 4)
    val expectedValues = listOf("4", "9", "4", "1")

    //  when & then
    shouldNotThrow<AssertionError> {
      actualValues.shouldContainExactlyInAnyOrder(::shouldBeTheSame, expectedValues)
    }
  }

  private fun shouldBeTheSame(actual: Int, expected: String) =
    actual shouldBe expected.toInt()
}
