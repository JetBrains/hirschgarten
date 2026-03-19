package org.jetbrains.bazel.sync.workspace.targetKind

import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo

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
      // rules_jvm from IntelliJ monorepo
      TargetKind("jvm_library", setOf(LanguageClass.JAVA, LanguageClass.KOTLIN), RuleType.LIBRARY),
      TargetKind("_jvm_library_jps", setOf(LanguageClass.JAVA, LanguageClass.KOTLIN), RuleType.LIBRARY),
      TargetKind("jvm_resources", setOf(LanguageClass.JAVA, LanguageClass.KOTLIN), RuleType.LIBRARY),
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

  override fun inferTargetKind(target: TargetInfo): TargetKind? {
    val inferredLanguages = buildSet {
      if (target.hasProtobufTargetInfo()) {
        add(LanguageClass.PROTOBUF)
      }
      if (target.getJvmTarget()) {
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

    val inferredRuleType = when {
      target.isTest() -> RuleType.TEST
      target.isApplication() -> RuleType.BINARY
      else -> RuleType.LIBRARY
    }
    return TargetKind(target.kind, inferredLanguages, inferredRuleType)
  }

  private fun TargetInfo.isTest(): Boolean = isApplication() && kind.endsWith("_test")

  private fun TargetInfo.isApplication(): Boolean = executable
}
