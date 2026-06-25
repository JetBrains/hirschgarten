package org.jetbrains.bazel.golang.targetKinds

import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.golang.GoLanguageClass
import org.jetbrains.bazel.sync.workspace.targetKind.TargetKindProvider

internal enum class GoRuleTypes(
  val kindString: String,
  val ruleType: RuleType,
) {
  GO_GAZELLE_BINARY("gazelle_binary", RuleType.BINARY),
  GO_TEST("go_test", RuleType.TEST),
  GO_APPENGINE_TEST("go_appengine_test", RuleType.TEST),
  GO_BINARY("go_binary", RuleType.BINARY),
  GO_APPENGINE_BINARY("go_appengine_binary", RuleType.BINARY),
  GO_LIBRARY("go_library", RuleType.LIBRARY),
  GO_SOURCE("go_source", RuleType.LIBRARY),
  GO_APPENGINE_LIBRARY("go_appengine_library", RuleType.LIBRARY),
  GO_PROTO_LIBRARY("go_proto_library", RuleType.LIBRARY),
  GO_WRAP_CC("go_wrap_cc", RuleType.LIBRARY),
  GO_WEB_TEST("go_web_test", RuleType.TEST),
  ;
}

internal class GoBazelRules : TargetKindProvider {
  override val targetKinds: Set<TargetKind>
    get() = GoRuleTypes.entries.map { TargetKind(kind = it.kindString, ruleType = it.ruleType) }.toSet()
}

internal fun TargetKind.includesGo(): Boolean = languageClasses.contains(GoLanguageClass.GO)
