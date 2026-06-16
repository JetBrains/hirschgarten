package org.jetbrains.bazel.server.bsp.managers

import com.intellij.aspect.lib.Rules
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelRelease

@ApiStatus.Internal
enum class Language(
  val aspectLanguage: Rules,
  val rulesetNames: List<String>,
  /**
   * Whether Bazel exposes this language natively before the Bazel 8 external rules migration.
   *
   * For Bazel 8 and later, bundled languages are only enabled when this language has explicit
   * autoload hints and Bazel reports matching external autoloads, which preserves the external-rules
   * path for languages such as protobuf.
   */
  val isBundled: Boolean,
  val autoloadHints: List<String> = emptyList(),
  val hostLocations: List<String> = emptyList(),
) {
  Java(
    Rules.JAVA,
    listOf("rules_java"),
    true,
    listOf("JavaInfo", "java_common", "JavaPluginInfo", "java_binary", "java_library"),
    listOf("https://github.com/bazelbuild/rules_java/"),
  ),
  Python(
    Rules.PYTHON,
    listOf("rules_python"),
    true, listOf("PyInfo"), listOf("https://github.com/bazel-contrib/rules_python/")),
  Scala(
    Rules.SCALA,
    listOf("rules_scala", "io_bazel_rules_scala"),
    false, listOf(),
    listOf("https://github.com/bazel-contrib/rules_scala/")),
  Kotlin(
    Rules.KOTLIN,
    listOf("rules_kotlin", "io_bazel_rules_kotlin"),
    false, listOf(),
    listOf("https://github.com/bazelbuild/rules_kotlin/")),
  Go(
    Rules.GO,
    listOf("rules_go", "io_bazel_rules_go"),
    false, listOf(),
    listOf("https://github.com/bazel-contrib/rules_go/")),
  RulesProto(
    Rules.PROTO,
    listOf("rules_proto"),
    false, emptyList(),
    listOf("https://github.com/bazelbuild/rules_proto")),
  Protobuf(
    Rules.PROTO,
    listOf("protobuf"),
    true, emptyList(),
    listOf("https://github.com/protocolbuffers/protobuf/")),
  ;

  fun isBundledFor(bazelRelease: BazelRelease, externalAutoloads: List<String>): Boolean {
    if (!isBundled) return false
    if (bazelRelease.major < 8) return true
    // Bazel 8+ only restores the bundled path for languages that opt in via [autoloadHints].
    // Languages without [autoloadHints] (e.g. Protobuf) must instead be discovered as an
    // external ruleset by name. Match against both [rulesetNames] and [autoloadHints]
    // because --incompatible_autoload_externally accepts ruleset names (e.g. "rules_java")
    // as well as individual symbol names (e.g. "JavaInfo", "PyInfo").
    if (autoloadHints.isEmpty()) return false
    return (rulesetNames + autoloadHints).any { it in externalAutoloads }
  }
}
