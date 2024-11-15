package org.jetbrains.bsp.bazel.server.sync

import org.jetbrains.bsp.bazel.info.BspTargetInfo
import org.jetbrains.bsp.bazel.server.model.Tag

private val bazelTagToTagMapping =
  mapOf(
    "no-ide" to Tag.NO_IDE,
    "manual" to Tag.MANUAL,
  )

class TargetTagsResolver {
  fun resolveTags(targetInfo: BspTargetInfo.TargetInfo): Set<Tag> {
    val typeTags =
      when {
        targetInfo.isTest() -> setOf(Tag.TEST)
        targetInfo.isIntellijPlugin() -> setOf(Tag.INTELLIJ_PLUGIN, Tag.APPLICATION)
        targetInfo.isApplication() || targetInfo.isAndroidBinary() -> setOf(Tag.APPLICATION)
        else -> setOf(Tag.LIBRARY)
      }

    return typeTags + mapBazelTags(targetInfo.tagsList)
  }

  // https://bazel.build/extending/rules#executable_rules_and_test_rules:
  // "Test rules must have names that end in _test."
  private fun BspTargetInfo.TargetInfo.isTest(): Boolean = isApplication() && kind.endsWith("_test")

  private fun BspTargetInfo.TargetInfo.isIntellijPlugin(): Boolean = kind == "intellij_plugin_debug_target"

  // Not marked as executable by Bazel, but is actually executable via bazel mobile-install
  private fun BspTargetInfo.TargetInfo.isAndroidBinary(): Boolean = kind == "android_binary"

  private fun BspTargetInfo.TargetInfo.isApplication(): Boolean = executable

  private fun mapBazelTags(tags: List<String>): Set<Tag> = tags.mapNotNull { bazelTagToTagMapping[it] }.toSet()
}
