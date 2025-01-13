package org.jetbrains.bsp.bazel.server.bsp.managers

import org.apache.velocity.app.VelocityEngine
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.commons.label.Label
import org.jetbrains.bsp.bazel.server.bsp.utils.FileUtils.writeIfDifferent
import org.jetbrains.bsp.bazel.server.bsp.utils.InternalAspectsResolver
import java.nio.file.Paths
import java.util.Properties

enum class Language(
  private val fileName: String,
  val ruleNames: List<String>,
  val functions: List<String>,
  val isTemplate: Boolean,
  val isBundled: Boolean,
) {
  Java("//aspects:rules/java/java_info.bzl", listOf("rules_java"), listOf("extract_java_toolchain", "extract_java_runtime"), true, true),
  Python("//aspects:rules/python/python_info.bzl", listOf("rules_python"), listOf("extract_python_info"), true, true),
  Scala("//aspects:rules/scala/scala_info.bzl", listOf("io_bazel_rules_scala", "rules_scala"), listOf("extract_scala_info"), false, false),
  Cpp("//aspects:rules/cpp/cpp_info.bzl", listOf("rules_cc"), listOf("extract_cpp_info", "extract_c_toolchain_info"), false, false),
  Kotlin("//aspects:rules/kt/kt_info.bzl", listOf("io_bazel_rules_kotlin", "rules_kotlin"), listOf("extract_kotlin_info"), true, false),
  Jvm("//aspects:rules/jvm/jvm_info.bzl", Java.ruleNames + Scala.ruleNames + Kotlin.ruleNames, listOf("extract_jvm_info"), true, true),
  Rust("//aspects:rules/rust/rust_info.bzl", listOf("rules_rust"), listOf("extract_rust_crate_info"), false, false),
  Android(
    "//aspects:rules/android/android_info.bzl",
    listOf("rules_android", "build_bazel_rules_android"),
    listOf("extract_android_info", "extract_android_aar_import_info"),
    true,
    false,
  ),
  Go("//aspects:rules/go/go_info.bzl", listOf("rules_go", "io_bazel_rules_go"), listOf("extract_go_info"), true, false),
  ;

  fun toLoadStatement(): String =
    this.functions.joinToString(
      prefix = """load("${this.fileName}", """,
      separator = ", ",
      postfix = ")",
    ) { "\"$it\"" }

  fun toAspectRelativePath(): String = fileName.substringAfter(":")

  fun toAspectTemplateRelativePath(): String = toAspectRelativePath() + Constants.TEMPLATE_EXTENSION
}

class BazelBspLanguageExtensionsGenerator(internalAspectsResolver: InternalAspectsResolver) {
  private val aspectsPath = Paths.get(internalAspectsResolver.bazelBspRoot, Constants.ASPECTS_ROOT)
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

  fun generateLanguageExtensions(ruleLanguages: List<RuleLanguage>, toolchains: Map<RuleLanguage, List<Label>>) {
    val fileContent = prepareFileContent(ruleLanguages, toolchains)
    createNewExtensionsFile(fileContent)
  }

  private fun prepareFileContent(ruleLanguages: List<RuleLanguage>, toolchains: Map<RuleLanguage, List<Label>>) =
    listOf(
      "# This is a generated file, do not edit it",
      createLoadStatementsString(ruleLanguages.map { it.language }),
      createExtensionListString(ruleLanguages.map { it.language }),
      createToolchainListString(ruleLanguages, toolchains),
    ).joinToString(
      separator = "\n",
      postfix = "\n",
    )

  private fun createLoadStatementsString(languages: List<Language>): String {
    val loadStatements = languages.map { it.toLoadStatement() }
    return loadStatements.joinToString(postfix = "\n", separator = "\n")
  }

  private fun createExtensionListString(languages: List<Language>): String {
    val functionNames = languages.flatMap { it.functions }
    return functionNames.joinToString(prefix = "EXTENSIONS = [\n", postfix = "\n]", separator = ",\n ") { "\t$it" }
  }

  private fun createToolchainListString(ruleLanguages: List<RuleLanguage>, toolchains: Map<RuleLanguage, List<Label>>): String =
    ruleLanguages
      .mapNotNull { toolchains[it] }
      .flatten()
      .joinToString(prefix = "TOOLCHAINS = [\n", postfix = "\n]", separator = ",\n ") { "\t\"$it\"" }

  private fun createNewExtensionsFile(fileContent: String) {
    val file = aspectsPath.resolve(Constants.EXTENSIONS_BZL)
    file.writeIfDifferent(fileContent)
  }
}
