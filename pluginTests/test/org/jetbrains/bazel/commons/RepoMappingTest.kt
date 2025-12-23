package org.jetbrains.bazel.commons

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.test.framework.annotation.BazelTest
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import com.intellij.util.containers.BidirectionalMap as IntellijBidirectionalMap

@BazelTest
class RepoMappingTest {
  private fun createTestBidirectionalMap(): BidirectionalMap<String, String> {
    val delegate = IntellijBidirectionalMap<String, String>()
    return object : BidirectionalMap<String, String>, MutableMap<String, String> by delegate {
      override fun getKeysByValue(value: String): List<String> = delegate.getKeysByValue(value) ?: emptyList()
    }
  }
}
