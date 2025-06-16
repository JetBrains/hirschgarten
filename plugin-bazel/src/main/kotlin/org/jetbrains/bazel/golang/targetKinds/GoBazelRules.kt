package org.jetbrains.bazel.golang.targetKinds

import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.sync.TargetKindProvider
import org.jetbrains.bazel.sync.fromRuleName

/** Contributes golang rules to [Kind].  */
class GoBazelRules : TargetKindProvider {
  /** Go-specific blaze rules.  */
  enum class RuleTypes(
    val kindString: String,
    val languageClass: LanguageClass,
    val ruleType: RuleType,
  ) {
    GO_GAZELLE_BINARY("gazelle_binary", LanguageClass.GO, RuleType.BINARY),
    GO_TEST("go_test", LanguageClass.GO, RuleType.TEST),
    GO_APPENGINE_TEST("go_appengine_test", LanguageClass.GO, RuleType.TEST),
    GO_BINARY("go_binary", LanguageClass.GO, RuleType.BINARY),
    GO_APPENGINE_BINARY("go_appengine_binary", LanguageClass.GO, RuleType.BINARY),
    GO_LIBRARY("go_library", LanguageClass.GO, RuleType.LIBRARY),
    GO_APPENGINE_LIBRARY("go_appengine_library", LanguageClass.GO, RuleType.LIBRARY),
    GO_PROTO_LIBRARY("go_proto_library", LanguageClass.GO, RuleType.LIBRARY),
    GO_WRAP_CC("go_wrap_cc", LanguageClass.GO, RuleType.UNKNOWN),
    GO_WEB_TEST("go_web_test", LanguageClass.GO, RuleType.TEST),
    ;

    val kind: TargetKind
      get() = TargetKind.fromRuleName(kindString)!!
  }

  override val targetKinds: Set<TargetKind>
    get() = RuleTypes.entries.map { TargetKind(it.kindString, setOf(it.languageClass), it.ruleType) }.toSet()
}
