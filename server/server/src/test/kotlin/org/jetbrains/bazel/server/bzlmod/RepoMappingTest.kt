package org.jetbrains.bazel.server.bzlmod

import com.intellij.util.containers.BidirectionalMap
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.label.Label
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

class RepoMappingTest {
  @Test
  fun `should canonicalize resolved label with apparent repo name`() {
    val repoMapping =
      BzlmodRepoMapping(
        mapOf("rules_blah" to Path("rules_blah")),
        BidirectionalMap<String, String>().apply { put("rules_blah", "rules_blah~") },
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
        BidirectionalMap<String, String>().apply { put("rules_blah", "rules_blah~") },
        mapOf("rules_blah~" to Path("bazel_cache/external/rules_blah~")),
      )
    val label = Label.parse("@@rules_blah~//path/to/target:targetName")
    val canonicalized = label.canonicalize(repoMapping)
    canonicalized.toString() shouldBe "@@rules_blah~//path/to/target:targetName"
  }
}
