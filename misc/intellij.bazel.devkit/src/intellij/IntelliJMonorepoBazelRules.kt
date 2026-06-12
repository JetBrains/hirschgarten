package org.jetbrains.bazel.intellij

import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.sync.workspace.targetKind.TargetKindProvider

internal class IntelliJMonorepoBazelRules : TargetKindProvider {
  override val targetKinds: Set<TargetKind> =
    setOf(
      // rules_jvm from IntelliJ monorepo
      // TODO: remove these target kinds after migrating to rules_kotlin https://youtrack.jetbrains.com/issue/MRI-3218
      TargetKind("jvm_library", setOf(LanguageClass.JAVA, LanguageClass.KOTLIN), RuleType.LIBRARY),
      TargetKind("_jvm_library_jps", setOf(LanguageClass.JAVA, LanguageClass.KOTLIN), RuleType.LIBRARY),
      TargetKind("_resourcegroup_jps", setOf(LanguageClass.JAVA), RuleType.LIBRARY),
      TargetKind("jvm_resources", setOf(LanguageClass.JAVA, LanguageClass.KOTLIN), RuleType.LIBRARY),
      TargetKind("jps_test", setOf(LanguageClass.JAVA, LanguageClass.KOTLIN), RuleType.TEST),
      TargetKind("intellij_dev_binary_ultimate", setOf(LanguageClass.JAVA, LanguageClass.KOTLIN), RuleType.BINARY),
      TargetKind("intellij_dev_binary_community", setOf(LanguageClass.JAVA, LanguageClass.KOTLIN), RuleType.BINARY),
    )
}
