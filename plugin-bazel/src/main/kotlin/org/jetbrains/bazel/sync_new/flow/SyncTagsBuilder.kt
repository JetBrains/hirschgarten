package org.jetbrains.bazel.sync_new.flow

import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetTag
import org.jetbrains.bsp.protocol.RawAspectTarget
import java.util.EnumSet

class SyncTagsBuilder {
  fun build(raw: RawAspectTarget): EnumSet<BazelTargetTag> {
    val tags = EnumSet.noneOf(BazelTargetTag::class.java)
    val target = raw.target
    when {
      target.executable && target.kind.endsWith("_test") -> tags.add(BazelTargetTag.TEST)
      target.kind == "intellij_plugin_debug_target" -> tags.add(BazelTargetTag.INTELLIJ_PLUGIN)
      target.executable -> tags.add(BazelTargetTag.EXECUTABLE)
      else -> tags.add(BazelTargetTag.LIBRARY)
    }
    target.tagsList.mapNotNull { TAGS_MAPPINGS[it] }
      .forEach { tags.add(it) }
    return tags
  }

  companion object {
    private val TAGS_MAPPINGS = mapOf(
      "no-ide" to BazelTargetTag.NO_IDE,
      "manual" to BazelTargetTag.MANUAL,
    )
  }
}
