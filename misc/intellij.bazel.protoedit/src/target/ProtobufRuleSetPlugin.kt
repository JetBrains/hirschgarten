package org.jetbrains.bazel.protobuf

import org.jetbrains.bazel.util.BazelRuleSetProvider

internal class ProtobufRuleSetPlugin : BazelRuleSetProvider {
  override fun ruleSets(): Set<String> = setOf("rules_proto", "protobuf")
}
