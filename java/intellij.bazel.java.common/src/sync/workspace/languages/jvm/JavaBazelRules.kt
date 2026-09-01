package org.jetbrains.bazel.sync.workspace.languages.jvm

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.sync.JavaLanguageClass
import org.jetbrains.bazel.sync.workspace.targetKind.TargetKindProvider

internal class JavaBazelRules: TargetKindProvider {
  override val targetKinds: Set<TargetKind> =
    setOf(
      TargetKind("java_library", setOf(JavaLanguageClass.JAVA), RuleType.LIBRARY),
      TargetKind("java_binary", setOf(JavaLanguageClass.JAVA), RuleType.BINARY),
      JAVA_TEST_KIND,
      // a workaround, register this target type as Java module in IntelliJ IDEA
      TargetKind("intellij_plugin_debug_target", setOf(JavaLanguageClass.JAVA), RuleType.BINARY),
      TargetKind("kt_jvm_library", setOf(JavaLanguageClass.JAVA, JavaLanguageClass.KOTLIN), RuleType.LIBRARY),
      TargetKind("kt_jvm_binary", setOf(JavaLanguageClass.JAVA, JavaLanguageClass.KOTLIN), RuleType.BINARY),
      TargetKind("kt_jvm_test", setOf(JavaLanguageClass.JAVA, JavaLanguageClass.KOTLIN), RuleType.TEST),
      TargetKind("scala_library", setOf(JavaLanguageClass.JAVA, JavaLanguageClass.SCALA), RuleType.LIBRARY),
      TargetKind("scala_binary", setOf(JavaLanguageClass.JAVA, JavaLanguageClass.SCALA), RuleType.BINARY),
      TargetKind("scala_test", setOf(JavaLanguageClass.JAVA, JavaLanguageClass.SCALA), RuleType.TEST),
    )
}

@ApiStatus.Internal
val JAVA_TEST_KIND: TargetKind = TargetKind("java_test", setOf(JavaLanguageClass.JAVA), RuleType.TEST)
