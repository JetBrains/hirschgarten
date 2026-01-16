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
    assertThrows<NullPointerException> { parsedBzlModGraph.getAllDirectRulesetDependencyNames() }
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
            "name": "rules_java",
            "apparentName": "rules_java",
            "dependencies": [
              {
                "key": "bazel_skylib@1.4.2",
                "name": "bazel_skylib",
                "apparentName": "bazel_skylib",
                "unexpanded": true
              },
              {
                "key": "platforms@0.0.8",
                "name": "platforms",
                "apparentName": "platforms",
                "unexpanded": true
              },
              {
                "key": "rules_cc@0.0.9",
                "name": "rules_cc",
                "apparentName": "rules_cc",
                "unexpanded": true
              }
            ]
          },
          {
            "key": "rules_jvm_external@6.0",
            "name": "rules_jvm_external",
            "apparentName": "rules_jvm_external",
            "dependencies": [
              {
                "key": "rules_kotlin@1.9.0",
                "name": "rules_kotlin",
                "apparentName": "rules_kotlin"
              }
            ]
          }
        ],
        "indirectDependencies": [],
        "cycles": [],
        "root": true
      }
      """.trimIndent()

    // when
    val parsedJson = s.toJson() as? JsonObject
    val parsedBzlModGraph = gson.fromJson(parsedJson, BzlmodGraph::class.java)

    // then
    parsedBzlModGraph.getAllDirectRulesetDependencyNames() shouldContainExactlyInAnyOrder listOf("rules_java", "rules_jvm_external")
    parsedBzlModGraph.includedByDirectDeps("rules_java", "rules_cc").isNotEmpty() shouldBe true
    parsedBzlModGraph.includedByDirectDeps("rules_java", "rules_xd").isNotEmpty() shouldBe false
  }
}
