package org.jetbrains.bazel.sync

import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind

/** Contributes generic rules to [TargetKind].  */
class GenericBazelRules : TargetKindProvider {
  /** Generic bazel rule types.  */
  enum class RuleTypes(
    val ruleName: String,
    val languageClass: LanguageClass,
    val ruleType: RuleType,
  ) {
    PROTO_LIBRARY("proto_library", LanguageClass.GENERIC, RuleType.LIBRARY),
    TEST_SUITE("test_suite", LanguageClass.GENERIC, RuleType.TEST),
    INTELLIJ_PLUGIN_DEBUG_TARGET(
      "intellij_plugin_debug_target",
      LanguageClass.JAVA,
      RuleType.UNKNOWN,
    ),
    WEB_TEST("web_test", LanguageClass.GENERIC, RuleType.TEST),
    SH_TEST("sh_test", LanguageClass.GENERIC, RuleType.TEST),
    SH_LIBRARY("sh_library", LanguageClass.GENERIC, RuleType.LIBRARY),
    SH_BINARY("sh_binary", LanguageClass.GENERIC, RuleType.BINARY),
    ;

    val kind: TargetKind
      get() = TargetKind.fromRuleName(name)!!
  }

  override val targetKinds: Set<TargetKind>
    get() =
      RuleTypes.entries
        .map { TargetKind(it.name, setOf(it.languageClass), it.ruleType) }
        .toSet()
}
