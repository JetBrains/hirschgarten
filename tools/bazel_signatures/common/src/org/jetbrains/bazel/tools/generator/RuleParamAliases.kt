package org.jetbrains.bazel.tools.generator

/**
 * A second name that Bazel accepts for a rule parameter from [since] up to [until], exclusive.
 * Bazel changes [alias] to [canonical] before it looks up the parameter.
 * Thus a rule accepts [alias] only when it has [canonical].
 */
data class ParamAlias(
  val alias: String,
  val canonical: String,
  val since: String,
  val until: String?,
)

/**
 * Parameter aliases that the Bazel output does not include. The generators add them at generation time.
 *
 * When Bazel removes an alias, set [ParamAlias.until] to the first version without it.
 * Keep the entry, because the JSON files of older versions still need it.
 *
 * Remove an entry when the oldest Bazel version that the plugin supports is at or after its `until` version.
 * When no entry is left, remove the `aliases` field from `ParamSignature` and from `BazelGlobalFunctionParameter`.
 */
object RuleParamAliases {
  val all: List<ParamAlias> = listOf(
    ParamAlias(alias = "applicable_licenses", canonical = "package_metadata", since = "8.0.0", until = null),
  )
}

fun FunctionSignature.withParamAliases(version: String?): FunctionSignature {
  val accepted = RuleParamAliases.all.filter { it.isAcceptedIn(version) }
  return copy(params = params.map { param -> param.withAliases(accepted) })
}

private fun ParamSignature.withAliases(accepted: List<ParamAlias>): ParamSignature {
  val aliases = accepted.filter { it.canonical == name }.map { it.alias }
  return if (aliases.isEmpty()) this else copy(aliases = aliases)
}

private fun ParamAlias.isAcceptedIn(version: String?): Boolean {
  if (version == null) return until == null
  return version.compareAsVersions(since) >= 0 && (until == null || version.compareAsVersions(until) < 0)
}
