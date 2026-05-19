package org.jetbrains.bazel.server.bsp.managers

import com.intellij.aspect.lib.Rules
import org.apache.velocity.app.VelocityEngine
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.server.bsp.utils.FileUtils.writeIfDifferent
import org.jetbrains.bazel.server.bsp.utils.InternalAspectsResolver
import java.util.Properties

@ApiStatus.Internal
enum class Language(
  val aspectLanguage: Rules,
  private val fileName: String,
  val rulesetNames: List<String>,
  val functions: List<String>,
  /**
   * Whether Bazel exposes this language natively before the Bazel 8 external rules migration.
   *
   * For Bazel 8 and later, bundled languages are only enabled when this language has explicit
   * autoload hints and Bazel reports matching external autoloads, which preserves the external-rules
   * path for languages such as protobuf.
   */
  val isBundled: Boolean,
  val autoloadHints: List<String> = emptyList(),
  val importsFromRuleSet: Map<String, List<String>> = emptyMap(),
  /**
   * https://bazel.build/versions/9.0.0/rules/lib/globals/bzl.html#aspect
   */
  val requiredAspectProviders: List<List<String>> = emptyList(),
  val hostLocations: List<String> = emptyList(),
) {
  Java(
    Rules.JAVA,
    "//" + Constants.DOT_BAZELBSP_DIR_NAME + "/aspects:rules/java/java_info.bzl",
    listOf("rules_java"),
    listOf("extract_java_toolchain", "extract_java_runtime"),
    true,
    listOf("JavaInfo", "java_common", "JavaPluginInfo", "java_binary", "java_library"),
    mapOf("java/common:java_info.bzl" to listOf("JavaInfo")),
    listOf(listOf("JavaInfo")),
    listOf("https://github.com/bazelbuild/rules_java/"),
  ),
  Python(
    Rules.PYTHON,
    "//" + Constants.DOT_BAZELBSP_DIR_NAME + "/aspects:rules/python/python_info.bzl", listOf("rules_python"), listOf("extract_python_info"),
         true, listOf("PyInfo"), mapOf(), listOf(), listOf("https://github.com/bazel-contrib/rules_python/")),
    Languages.PYTHON,
    listOf("rules_python"), listOf("extract_python_info"),
    true, listOf("PyInfo"), listOf("https://github.com/bazel-contrib/rules_python/")),
  Scala(
    Rules.SCALA,
    "//" + Constants.DOT_BAZELBSP_DIR_NAME + "/aspects:rules/scala/scala_info.bzl", listOf("rules_scala", "io_bazel_rules_scala"), listOf("extract_scala_info"),
        false, listOf(), mapOf(), listOf(), listOf("https://github.com/bazel-contrib/rules_scala/")),
  Kotlin(
    Rules.KOTLIN,
    "//" + Constants.DOT_BAZELBSP_DIR_NAME + "/aspects:rules/kt/kt_info.bzl", listOf("rules_kotlin", "io_bazel_rules_kotlin"), listOf("extract_kotlin_info"),
         false, listOf(), mapOf(), listOf(), listOf("https://github.com/bazelbuild/rules_kotlin/")),
  Go(
    Rules.GO,
    "//" + Constants.DOT_BAZELBSP_DIR_NAME + "/aspects:rules/go/go_info.bzl", listOf("rules_go", "io_bazel_rules_go"), listOf("extract_go_info"),
     false, listOf(), mapOf(), listOf(),listOf("https://github.com/bazel-contrib/rules_go/")),
  RulesProto(
    Rules.PROTO, // XXX Need to support rules_proto as well
    "//" + Constants.DOT_BAZELBSP_DIR_NAME + "/aspects:rules/protobuf/rules_proto_info.bzl", listOf("rules_proto"), listOf("extract_rules_proto_info"),
           false, emptyList(), emptyMap(), emptyList(), listOf("https://github.com/bazelbuild/rules_proto")),
    Languages.PROTO, // XXX Need to support rules_proto as well
    listOf("rules_proto"), listOf("extract_rules_proto_info"),
    false, emptyList(),
    listOf("https://github.com/bazelbuild/rules_proto")),
  Protobuf(
    Rules.PROTO,
    "//" + Constants.DOT_BAZELBSP_DIR_NAME + "/aspects:rules/protobuf/protobuf_info.bzl", listOf("protobuf"), listOf("extract_protobuf_info"),
           true, emptyList(), emptyMap(), emptyList(), listOf("https://github.com/protocolbuffers/protobuf/")),
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

  fun toLoadStatement(): String =
    this.functions.joinToString(
      prefix = """load("${this.fileName}", """,
      separator = ", ",
      postfix = ")",
    ) { "\"$it\"" }

  private fun toCanonicalRuleName(repoMapping: RepoMapping) : String? {
    val toCanonical = (repoMapping as? BzlmodRepoMapping)?.apparentRepoNameToCanonicalName ?: return null
    return rulesetNames.filter { it in toCanonical }.firstOrNull()?.let { "@@" + toCanonical[it] }
  }

  fun toRuleImportLoads(repoMapping: RepoMapping): List<String> {
    val name = toCanonicalRuleName(repoMapping) ?: return emptyList()
    return importsFromRuleSet.map { (filename, imports) -> "load(\"$name//$filename\", ${imports.joinToString(", ") { "\"$it\"" }})"}.sorted()
  }

  fun toRequiredAspectProviders(repoMapping: RepoMapping, externalAutoloads: List<String>): List<String> {
    toCanonicalRuleName(repoMapping) ?: (rulesetNames.filter { it in externalAutoloads }.firstOrNull()) ?: return emptyList()
    return requiredAspectProviders.map {it.joinToString(prefix = "[",separator = ",",  postfix = "]") }
  }

  fun toAspectRelativePath(): String = fileName.substringAfter(":")

  fun toAspectTemplateRelativePath(): String = toAspectRelativePath() + Constants.TEMPLATE_EXTENSION
}

@ApiStatus.Internal
class BazelBspLanguageExtensionsGenerator(internalAspectsResolver: InternalAspectsResolver) {
  private val aspectsPath = internalAspectsResolver.aspectsPath
  private val velocityEngine = VelocityEngine()

  init {
    val props = calculateProperties()
    velocityEngine.init(props)
  }

  private fun calculateProperties(): Properties {
    val props = Properties()
    props["resource.loader.file.path"] = aspectsPath.toAbsolutePath().toString()
    props.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogSystem")
    return props
  }

  fun generateLanguageExtensions(rulesetLanguages: List<RulesetLanguage>, toolchains: Map<RulesetLanguage, String?>, repoMapping: RepoMapping, externalAutoloads: List<String> ) {
    val fileContent = prepareFileContent(rulesetLanguages, toolchains, repoMapping, externalAutoloads)
    createNewExtensionsFile(fileContent)
  }

  private fun prepareFileContent(rulesetLanguages: List<RulesetLanguage>, toolchains: Map<RulesetLanguage, String?>, repoMapping: RepoMapping, externalAutoloads: List<String>) =
    listOf(
      "# This is a generated file, do not edit it",
      createLoadStatementsString(rulesetLanguages.map { it.language }, repoMapping),
      createExtensionListString(rulesetLanguages.map { it.language }),
      createToolchainListString(rulesetLanguages, toolchains),
      createRequiredAspectProviders(rulesetLanguages.map { it.language }, repoMapping, externalAutoloads)
    ).joinToString(
      separator = "\n",
      postfix = "\n",
    )

  private fun createLoadStatementsString(languages: List<Language>, repoMapping: RepoMapping): String {
    val ruleImports = languages.flatMap { it.toRuleImportLoads(repoMapping) }
    val loadStatements = languages.map { it.toLoadStatement() }
    return (ruleImports + loadStatements).joinToString(postfix = "\n", separator = "\n")
  }

  private fun createExtensionListString(languages: List<Language>): String {
    val functionNames = languages.flatMap { it.functions }
    return functionNames.joinToString(prefix = "EXTENSIONS = [\n", postfix = "\n]", separator = ",\n ") { "\t$it" }
  }

  private fun createToolchainListString(rulesetLanguages: List<RulesetLanguage>, toolchains: Map<RulesetLanguage, String?>): String =
    rulesetLanguages
      .mapNotNull { toolchains[it] }
      .joinToString(prefix = "TOOLCHAINS = [\n ", postfix = "\n]", separator = ",\n ")

  private fun createRequiredAspectProviders(languages: List<Language>, repoMapping: RepoMapping, externalAutoloads: List<String>): String =
    languages.flatMap { it.toRequiredAspectProviders(repoMapping, externalAutoloads) }.joinToString(prefix = "REQUIRED_ASPECT_PROVIDERS = [\n ", separator = ",\n ", postfix = "\n]")

  private fun createNewExtensionsFile(fileContent: String) {
    val file = aspectsPath.resolve(Constants.EXTENSIONS_BZL)
    file.writeIfDifferent(fileContent)
  }
}
