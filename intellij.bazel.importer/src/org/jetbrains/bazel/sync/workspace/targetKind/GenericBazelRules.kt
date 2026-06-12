package org.jetbrains.bazel.sync.workspace.targetKind

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind

private class GenericBazelRules : TargetKindProvider {
  override val targetKinds: Set<TargetKind> =
    setOf(
      TargetKind("java_library", setOf(LanguageClass.JAVA), RuleType.LIBRARY),
      TargetKind("java_binary", setOf(LanguageClass.JAVA), RuleType.BINARY),
      TargetKind("java_test", setOf(LanguageClass.JAVA), RuleType.TEST),
      // a workaround, register this target type as Java module in IntelliJ IDEA
      TargetKind("intellij_plugin_debug_target", setOf(LanguageClass.JAVA), RuleType.BINARY),
      TargetKind("kt_jvm_library", setOf(LanguageClass.JAVA, LanguageClass.KOTLIN), RuleType.LIBRARY),
      TargetKind("kt_jvm_binary", setOf(LanguageClass.JAVA, LanguageClass.KOTLIN), RuleType.BINARY),
      TargetKind("kt_jvm_test", setOf(LanguageClass.JAVA, LanguageClass.KOTLIN), RuleType.TEST),
      TargetKind("scala_library", setOf(LanguageClass.JAVA, LanguageClass.SCALA), RuleType.LIBRARY),
      TargetKind("scala_binary", setOf(LanguageClass.JAVA, LanguageClass.SCALA), RuleType.BINARY),
      TargetKind("scala_test", setOf(LanguageClass.JAVA, LanguageClass.SCALA), RuleType.TEST),

      TargetKind("go_binary", setOf(LanguageClass.GO), RuleType.BINARY),
      TargetKind("go_test", setOf(LanguageClass.GO), RuleType.TEST),
      TargetKind("go_library", setOf(LanguageClass.GO), RuleType.LIBRARY),
      TargetKind("go_source", setOf(LanguageClass.GO), RuleType.LIBRARY),
      TargetKind("py_binary", setOf(LanguageClass.PYTHON), RuleType.BINARY),
      TargetKind("py_test", setOf(LanguageClass.PYTHON), RuleType.TEST),
      TargetKind("py_library", setOf(LanguageClass.PYTHON), RuleType.LIBRARY),
      TargetKind("proto_library", setOf(LanguageClass.JAVA, LanguageClass.PROTOBUF), RuleType.LIBRARY),
      TargetKind("test_suite", setOf(), RuleType.TEST),
      TargetKind("web_test", setOf(), RuleType.TEST),
      TargetKind("sh_test", setOf(), RuleType.TEST),
      TargetKind("sh_library", setOf(), RuleType.LIBRARY),
      TargetKind("sh_binary", setOf(), RuleType.BINARY),
    )

  override fun inferTargetKind(target: TargetIdeInfo): TargetKind? {
    val inferredLanguages = buildSet {
      if (target.hasProtobufTargetInfo()) {
        add(LanguageClass.PROTOBUF)
      }
      if (target.javaCommon.jvmTarget) {
        add(LanguageClass.JAVA)
      }
      if (target.hasPythonTargetInfo()) {
        add(LanguageClass.PYTHON)
      }
      if (target.hasGoTargetInfo()) {
        add(LanguageClass.GO)
      }
    }

    if (inferredLanguages.isEmpty()) return null
    return TargetKind(target.kind, inferredLanguages, target.inferRuleType())
  }
}
