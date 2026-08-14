package org.jetbrains.bazel.sync.workspace.targetKind

import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind

internal class GenericBazelRules : TargetKindProvider {
  override val targetKinds: Set<TargetKind> =
    setOf(
      TargetKind("test_suite", setOf(), RuleType.TEST),
      TargetKind("web_test", setOf(), RuleType.TEST),
      TargetKind("sh_test", setOf(), RuleType.TEST),
      TargetKind("sh_library", setOf(), RuleType.LIBRARY),
      TargetKind("sh_binary", setOf(), RuleType.BINARY),
    )
}
