package org.jetbrains.bsp.bazel.server.sync

import io.kotest.matchers.shouldBe
import org.jetbrains.bsp.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bsp.bazel.server.model.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class TargetTagsResolverTest {
  @Test
  fun `should map executable targets`() {
    val targetInfo =
      TargetInfo
        .newBuilder()
        .also {
          it.executable = true
        }.build()

    val tags = TargetTagsResolver().resolveTags(targetInfo)

    tags shouldBe setOf(Tag.APPLICATION)
  }

  @Test
  fun `should map a target with _binary to library if it is not executable`() {
    val targetInfo =
      TargetInfo
        .newBuilder()
        .apply {
          kind = "it_only_looks_like_binary"
          executable = false
        }.build()

    val tags = TargetTagsResolver().resolveTags(targetInfo)

    tags shouldBe setOf(Tag.LIBRARY)
  }

  @Test
  fun `should map test targets`() {
    val targetInfo =
      TargetInfo
        .newBuilder()
        .also {
          it.kind = "blargh_test"
          it.executable = true
        }.build()

    val tags = TargetTagsResolver().resolveTags(targetInfo)

    tags shouldBe setOf(Tag.TEST)
  }

  @Test
  fun `should map intellij_plugin_debug_target`() {
    val targetInfo =
      TargetInfo
        .newBuilder()
        .also {
          it.kind = "intellij_plugin_debug_target"
        }.build()

    val tags = TargetTagsResolver().resolveTags(targetInfo)

    tags shouldBe setOf(Tag.INTELLIJ_PLUGIN, Tag.APPLICATION)
  }

  @Test
  fun `should map no-ide and manual tags for library`() {
    val targetInfo =
      TargetInfo
        .newBuilder()
        .also {
          it.addTags("no-ide")
          it.addTags("manual")
        }.build()

    val tags = TargetTagsResolver().resolveTags(targetInfo)

    tags shouldBe setOf(Tag.LIBRARY, Tag.NO_IDE, Tag.MANUAL)
  }

  @Test
  fun `should map no-ide and manual tags for application`() {
    val targetInfo =
      TargetInfo
        .newBuilder()
        .also {
          it.executable = true
          it.addTags("no-ide")
          it.addTags("manual")
        }.build()

    val tags = TargetTagsResolver().resolveTags(targetInfo)

    tags shouldBe setOf(Tag.APPLICATION, Tag.NO_IDE, Tag.MANUAL)
  }

  // now in the implementation we don't have any special cases, but they used to be handled differently,
  // so let's keep the test just to make sure that they are handled correctly anyway
  @ParameterizedTest
  @ValueSource(strings = ["resources_union", "java_import", "aar_import"])
  fun `should handle special cases`(name: String) {
    val targetInfo =
      TargetInfo
        .newBuilder()
        .also {
          it.kind = name
        }.build()

    val tags = TargetTagsResolver().resolveTags(targetInfo)

    tags shouldBe setOf(Tag.LIBRARY)
  }
}
