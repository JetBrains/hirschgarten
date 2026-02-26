package org.jetbrains.bazel.server.bsp.managers

import org.apache.velocity.app.VelocityEngine
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.server.bsp.utils.FileUtils.writeIfDifferent
import org.jetbrains.bazel.server.bsp.utils.InternalAspectsResolver
import java.util.Properties

enum class Language(
  private val fileName: String,
  val rulesetNames: List<String>,
  val functions: List<String>,
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
    "//" + Constants.DOT_BAZELBSP_DIR_NAME + "/aspects:rules/java/java_info.bzl",
    listOf("rules_java"),
    listOf("extract_java_toolchain", "extract_java_runtime"),
    true,
    listOf("JavaInfo", "java_common", "JavaPluginInfo", "java_binary", "java_library"),
    mapOf("java/common:java_info.bzl" to listOf("JavaInfo")),
    listOf(listOf("JavaInfo")),
    listOf("https://github.com/bazelbuild/rules_java/"),
  ),
  Python("//" + Constants.DOT_BAZELBSP_DIR_NAME + "/aspects:rules/python/python_info.bzl", listOf("rules_python"), listOf("extract_python_info"),
         true, listOf("PyInfo"), mapOf(), listOf(), listOf("https://github.com/bazel-contrib/rules_python/")),
  Scala("//" + Constants.DOT_BAZELBSP_DIR_NAME + "/aspects:rules/scala/scala_info.bzl", listOf("io_bazel_rules_scala", "rules_scala"), listOf("extract_scala_info"),
        false, listOf(), mapOf(), listOf(), listOf("https://github.com/bazel-contrib/rules_scala/")),
  Kotlin("//" + Constants.DOT_BAZELBSP_DIR_NAME + "/aspects:rules/kt/kt_info.bzl", listOf("io_bazel_rules_kotlin", "rules_kotlin"), listOf("extract_kotlin_info"),
         false, listOf(), mapOf(), listOf(), listOf("https://github.com/bazelbuild/rules_kotlin/")),
  Jvm(
    "//" + Constants.DOT_BAZELBSP_DIR_NAME + "/aspects:rules/jvm/jvm_info.bzl",
    Java.rulesetNames + Scala.rulesetNames + Kotlin.rulesetNames,
    listOf("extract_jvm_info"),
    true,
    Java.autoloadHints + Scala.autoloadHints + Kotlin.autoloadHints,
  ),
  Go("//" + Constants.DOT_BAZELBSP_DIR_NAME + "/aspects:rules/go/go_info.bzl", listOf("rules_go", "io_bazel_rules_go"), listOf("extract_go_info"),
     false, listOf(), mapOf(), listOf(),listOf("https://github.com/bazel-contrib/rules_go/")),
  RulesProto("//" + Constants.DOT_BAZELBSP_DIR_NAME + "/aspects:rules/protobuf/rules_proto_info.bzl", listOf("rules_proto"), listOf("extract_rules_proto_info"),
           false, emptyList(), emptyMap(), emptyList(), listOf("https://github.com/bazelbuild/rules_proto")),
  Protobuf("//" + Constants.DOT_BAZELBSP_DIR_NAME + "/aspects:rules/protobuf/protobuf_info.bzl", listOf("protobuf"), listOf("extract_protobuf_info"),
           false, emptyList(), emptyMap(), emptyList(), listOf("https://github.com/protocolbuffers/protobuf/")),
  ;

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
