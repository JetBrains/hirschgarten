package org.jetbrains.bazel.languages.projectview.language.sections

import org.jetbrains.bazel.languages.projectview.language.SectionKey
import org.jetbrains.bazel.languages.projectview.language.sections.presets.VariantsScalarSection

enum class ShardingApproach {
  EXPAND_AND_SHARD, // expand wildcard targets to package targets, query single targets, and then shard to batches
  QUERY_AND_SHARD, // query single targets from the given list of targets, and then shard to batches
  SHARD_ONLY, // split unexpanded wildcard targets into batches
  ;

  companion object {
    fun fromString(rawValue: String?): ShardingApproach? = ShardingApproach.entries.find { it.name.equals(rawValue, ignoreCase = true) }
  }
}

class ShardingApproachSection : VariantsScalarSection<ShardingApproach>(VARIANTS) {
  override val name = NAME
  override val sectionKey = KEY
  override val doc = "Sharding approach to use for the build, can be: expand_and_shard, query_and_shard, shard_only"

  override fun fromRawValue(rawValue: String): ShardingApproach? = ShardingApproach.fromString(rawValue)

  companion object {
    const val NAME = "sharding_approach"
    val KEY = SectionKey<ShardingApproach>(NAME)
    private val VARIANTS = ShardingApproach.entries.map { it.name.lowercase() }
  }
}
