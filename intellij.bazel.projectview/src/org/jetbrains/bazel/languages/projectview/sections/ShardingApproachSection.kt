package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.bazel.commons.ShardingApproach
import org.jetbrains.bazel.languages.projectview.SHARDING_APPROACH_KEY
import org.jetbrains.bazel.languages.projectview.SectionKey
import org.jetbrains.bazel.languages.projectview.sections.presets.VariantsScalarSection

internal class ShardingApproachSection : VariantsScalarSection<ShardingApproach?>(VARIANTS) {
  override val sectionKey: SectionKey<ShardingApproach?> = SHARDING_APPROACH_KEY
  override val doc = "Sharding approach to use for the build, can be: expand_and_shard, query_and_shard, shard_only"

  override fun fromRawValue(rawValue: String): ShardingApproach? = ShardingApproach.fromString(rawValue)

  companion object {
    private val VARIANTS = ShardingApproach.entries.map { it.name.lowercase() }
  }
}
