package org.jetbrains.bazel.protobuf

import com.intellij.aspect.lib.Rules
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.util.BazelRuleSet
import org.jetbrains.bazel.util.BazelRuleSetProvider

@ApiStatus.Internal
val ProtobufRuleSet: BazelRuleSet = BazelRuleSet(
  aspectLanguage = Rules.PROTO,
  rulesetNames = listOf("protobuf"),
  isBundled = true,
  autoloadHints = listOf(),
  hostLocations = listOf("https://github.com/protocolbuffers/protobuf/"),
  minimalVersion = 27,
  versionRegex = ".*/(?:download|tags)/v(\\d+)\\..*".toRegex(),
)

private val LegacyProtoRuleSet: BazelRuleSet = BazelRuleSet(
  aspectLanguage = Rules.LEGACY_RULES_PROTO,
  rulesetNames = listOf("rules_proto"),
  isBundled = false,
  autoloadHints = listOf(),
  hostLocations = listOf("https://github.com/bazelbuild/rules_proto"),
  deprecated = "`rules_proto` is deprecated and should be replaced by `protobuf` version at least 27.0"
)

internal class ProtobufRuleSetPlugin : BazelRuleSetProvider {
  override fun ruleSets(): List<BazelRuleSet> = listOf(ProtobufRuleSet, LegacyProtoRuleSet)
}
