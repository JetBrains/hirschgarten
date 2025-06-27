package org.jetbrains.bazel.server.bzlmod

import com.intellij.util.containers.BidirectionalMap as IntellijBidirectionalMap
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.BidirectionalMap
import org.jetbrains.bazel.label.Label
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

class RepoMappingTest {

  private fun createTestBidirectionalMap(): BidirectionalMap<String, String> {
    return object : BidirectionalMap<String, String> {
      private val delegate = IntellijBidirectionalMap<String, String>()

      override val keys: Set<String> get() = delegate.keys
      override val values: Collection<String> get() = delegate.values
      override fun get(key: String): String? = delegate[key]
      override fun getKeysByValue(value: String): List<String> = delegate.getKeysByValue(value) ?: emptyList()
      override fun put(key: String, value: String): String? = delegate.put(key, value)
      override fun putAll(map: Map<String, String>) = delegate.putAll(map)
      override fun remove(key: String): String? = delegate.remove(key)
      override fun clear() = delegate.clear()
      override fun isEmpty(): Boolean = delegate.isEmpty()
      override fun size(): Int = delegate.size
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
