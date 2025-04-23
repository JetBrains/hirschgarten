package org.jetbrains.bazel.server.bsp.managers

import com.google.gson.JsonObject
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.gson.bazelGson
import org.jetbrains.bazel.server.bsp.utils.toJson
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BzlModGraphTest {
  private val gson = bazelGson

  @Test
  fun `should throw NullPointerException for an invalid BzlMod json`() {
    // given
    val s = "{}"

    // when
    val parsedJson = s.toJson() as? JsonObject
    val parsedBzlModGraph = gson.fromJson(parsedJson, BzlmodGraph::class.java)

    // then
    assertThrows<NullPointerException> { parsedBzlModGraph.getAllDirectRulesetDependencies() }
  }

  @Test
  fun `should return dependencies from a valid BzlMod json`() {
    // given
    val s =
      """
      {
        "key": "test",
        "dependencies": [
          {
            "key": "rules_java@7.4.0",
            "dependencies": [
              {
                "key": "bazel_skylib@1.4.2",
                "unexpanded": true
              },
              {
                "key": "platforms@0.0.8",
                "unexpanded": true
              },
              {
                "key": "rules_cc@0.0.9",
                "unexpanded": true
              }
            ]
          },
          {
            "key": "rules_jvm_external@6.0",
            "dependencies": [
              {
                "key": "rules_kotlin@1.9.0"
              }
            ]
          }
        ]
      }
      """.trimIndent()

    // when
    val parsedJson = s.toJson() as? JsonObject
    val parsedBzlModGraph = gson.fromJson(parsedJson, BzlmodGraph::class.java)

    // then
    parsedBzlModGraph.getAllDirectRulesetDependencies() shouldContainExactlyInAnyOrder listOf("rules_java", "rules_jvm_external")
    parsedBzlModGraph.isIncludedTransitively("rules_java", "rules_cc") shouldBe true
    parsedBzlModGraph.isIncludedTransitively("rules_java", "rules_xd") shouldBe false
  }
}
