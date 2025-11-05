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

  @Test
  fun `should canonicalize resolved label with apparent repo name`() {
    val repoMapping =
      BzlmodRepoMapping(
        mapOf("rules_blah" to Path("rules_blah")),
        createTestBidirectionalMap().apply { put("rules_blah", "rules_blah~") },
        mapOf("rules_blah~" to Path("bazel_cache/external/rules_blah~")),
      )
    val label = Label.parse("@rules_blah//path/to/target:targetName")
    val canonicalized = label.canonicalize(repoMapping)
    canonicalized.toString() shouldBe "@@rules_blah~//path/to/target:targetName"
  }

  @Test
  fun `should leave canonical label as is`() {
    val repoMapping =
      BzlmodRepoMapping(
        mapOf("rules_blah" to Path("rules_blah")),
        createTestBidirectionalMap().apply { put("rules_blah", "rules_blah~") },
        mapOf("rules_blah~" to Path("bazel_cache/external/rules_blah~")),
      )
    val label = Label.parse("@@rules_blah~//path/to/target:targetName")
    val canonicalized = label.canonicalize(repoMapping)
    canonicalized.toString() shouldBe "@@rules_blah~//path/to/target:targetName"
  }
}
