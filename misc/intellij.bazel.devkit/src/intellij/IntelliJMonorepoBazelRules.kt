package org.jetbrains.bazel.intellij

import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.sync.JavaLanguageClass
import org.jetbrains.bazel.sync.workspace.targetKind.TargetKindProvider

internal class IntelliJMonorepoBazelRules : TargetKindProvider {
  override val targetKinds: Set<TargetKind> =
    setOf(
      // rules_jvm from IntelliJ monorepo
      // TODO: remove these target kinds after migrating to rules_kotlin https://youtrack.jetbrains.com/issue/MRI-3218
      TargetKind("jvm_library", setOf(JavaLanguageClass.JAVA, JavaLanguageClass.KOTLIN), RuleType.LIBRARY),
      TargetKind("_jvm_library_jps", setOf(JavaLanguageClass.JAVA, JavaLanguageClass.KOTLIN), RuleType.LIBRARY),
      TargetKind("_resourcegroup_jps", setOf(JavaLanguageClass.JAVA), RuleType.LIBRARY),
      TargetKind("jvm_resources", setOf(JavaLanguageClass.JAVA, JavaLanguageClass.KOTLIN), RuleType.LIBRARY),
      TargetKind("jps_test", setOf(JavaLanguageClass.JAVA, JavaLanguageClass.KOTLIN), RuleType.TEST),
      TargetKind("intellij_dev_binary_ultimate", setOf(JavaLanguageClass.JAVA, JavaLanguageClass.KOTLIN), RuleType.BINARY),
      TargetKind("intellij_dev_binary_community", setOf(JavaLanguageClass.JAVA, JavaLanguageClass.KOTLIN), RuleType.BINARY),
      TargetKind("server_bundle", setOf(JavaLanguageClass.JAVA, JavaLanguageClass.KOTLIN), RuleType.BINARY),
    )
}
