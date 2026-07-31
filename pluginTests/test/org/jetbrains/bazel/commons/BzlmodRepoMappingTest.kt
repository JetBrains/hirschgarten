package org.jetbrains.bazel.commons

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BzlmodRepoMappingTest {
  @Test
  fun `resolves a ruleset by its direct apparent name`() {
    val mapping = BzlmodRepoMapping(mapOf(), mapOf("rules_java" to "rules_java+"), mapOf())

    mapping.canonicalNameForRuleset("rules_java") shouldBe "rules_java+"
  }

  @Test
  fun `resolves a ruleset whose apparent name was customized via repo_name`() {
    // protobuf declared with repo_name = "com_google_protobuf" (apex-source ARCH-361) removes the
    // default "protobuf" apparent name, which previously made aspects load a non-existent @protobuf.
    val mapping = BzlmodRepoMapping(mapOf(), mapOf("com_google_protobuf" to "protobuf+"), mapOf())

    mapping.canonicalNameForRuleset("protobuf") shouldBe "protobuf+"
  }

  @Test
  fun `resolves a ruleset for older tilde-style canonical names`() {
    val mapping = BzlmodRepoMapping(mapOf(), mapOf("com_google_protobuf" to "protobuf~33.6"), mapOf())

    mapping.canonicalNameForRuleset("protobuf") shouldBe "protobuf~33.6"
  }

  @Test
  fun `does not match extension-generated repos that merely contain the module name`() {
    val mapping = BzlmodRepoMapping(mapOf(), mapOf("dep" to "gazelle++go_deps+com_github_golang_protobuf"), mapOf())

    mapping.canonicalNameForRuleset("protobuf") shouldBe null
  }

  @Test
  fun `returns null when the module is absent`() {
    val mapping = BzlmodRepoMapping(mapOf(), mapOf(), mapOf())

    mapping.canonicalNameForRuleset("protobuf") shouldBe null
  }
}