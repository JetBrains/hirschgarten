package org.jetbrains.bsp.bazel.server.sync

import org.jetbrains.bsp.bazel.info.BspTargetInfo
import org.jetbrains.bsp.bazel.server.model.Tag

class TargetTagsResolver {
  fun resolveTags(targetInfo: BspTargetInfo.TargetInfo): Set<Tag> {
    if (targetInfo.kind == "resources_union" ||
      targetInfo.kind == "java_import" ||
      targetInfo.kind == "aar_import"
    ) {
      return setOf(Tag.LIBRARY)
    }
    val tagsFromSuffix =
      ruleSuffixToTargetType
        .filterKeys {
          targetInfo.kind.endsWith("_$it") || targetInfo.kind == it
        }.values
        .firstOrNull()
        .orEmpty()

    // Tests *are* executable, but there's hardly a reason why one would like to `bazel run` a test
    val application = Tag.APPLICATION.takeIf { targetInfo.executable && !tagsFromSuffix.contains(Tag.TEST) }

    return setOfNotNull(
      application,
    ) + mapBazelTags(targetInfo.tagsList) + tagsFromSuffix
  }

  private fun mapBazelTags(tags: List<String>): Set<Tag> = tags.mapNotNull { bazelTagMap[it] }.toSet()

  companion object {
    private val bazelTagMap =
      mapOf(
        "no-ide" to Tag.NO_IDE,
        "manual" to Tag.MANUAL,
      )

    private val ruleSuffixToTargetType =
      mapOf(
        "library" to setOf(Tag.LIBRARY),
        "binary" to setOf(Tag.APPLICATION),
        "test" to setOf(Tag.TEST),
        "proc_macro" to setOf(Tag.LIBRARY),
        "intellij_plugin_debug_target" to setOf(Tag.INTELLIJ_PLUGIN, Tag.APPLICATION),
        "plugin" to setOf(Tag.LIBRARY),
      )
  }
}
