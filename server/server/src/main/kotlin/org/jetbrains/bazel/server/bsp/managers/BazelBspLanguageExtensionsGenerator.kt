package org.jetbrains.bazel.server.bsp.managers

import org.apache.velocity.app.VelocityEngine
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.server.bsp.utils.FileUtils.writeIfDifferent
import org.jetbrains.bazel.server.bsp.utils.InternalAspectsResolver
import java.nio.file.Paths
import java.util.Properties

enum class Language(
  private val fileName: String,
  val rulesetNames: List<String>,
  val functions: List<String>,
  val isTemplate: Boolean,
  val isBundled: Boolean,
  val autoloadHints: List<String> = emptyList(),
) {
  Java(
    "//aspects:rules/java/java_info.bzl",
    listOf("rules_java"),
    listOf("extract_java_toolchain", "extract_java_runtime"),
    true,
    true,
    listOf("JavaInfo", "java_common", "JavaPluginInfo", "java_binary", "java_library"),
  ),
  Python("//aspects:rules/python/python_info.bzl", listOf("rules_python"), listOf("extract_python_info"), true, true, listOf("PyInfo")),
  Scala("//aspects:rules/scala/scala_info.bzl", listOf("io_bazel_rules_scala", "rules_scala"), listOf("extract_scala_info"), true, false),
  Kotlin("//aspects:rules/kt/kt_info.bzl", listOf("io_bazel_rules_kotlin", "rules_kotlin"), listOf("extract_kotlin_info"), true, false),
  Jvm(
    "//aspects:rules/jvm/jvm_info.bzl",
    Java.rulesetNames + Scala.rulesetNames + Kotlin.rulesetNames,
    listOf("extract_jvm_info"),
    true,
    true,
    Java.autoloadHints + Scala.autoloadHints + Kotlin.autoloadHints,
  ),
  Go("//aspects:rules/go/go_info.bzl", listOf("rules_go", "io_bazel_rules_go"), listOf("extract_go_info"), true, false),
  Protobuf("//aspects:rules/protobuf/protobuf_info.bzl", listOf("rules_proto"), listOf("extract_protobuf_info"), false, false),
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

  fun generateLanguageExtensions(rulesetLanguages: List<RulesetLanguage>, toolchains: Map<RulesetLanguage, String?>) {
    val fileContent = prepareFileContent(rulesetLanguages, toolchains)
    createNewExtensionsFile(fileContent)
  }

  private fun prepareFileContent(rulesetLanguages: List<RulesetLanguage>, toolchains: Map<RulesetLanguage, String?>) =
    listOf(
      "# This is a generated file, do not edit it",
      createLoadStatementsString(rulesetLanguages.map { it.language }),
      createExtensionListString(rulesetLanguages.map { it.language }),
      createToolchainListString(rulesetLanguages, toolchains),
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

  private fun createToolchainListString(rulesetLanguages: List<RulesetLanguage>, toolchains: Map<RulesetLanguage, String?>): String =
    rulesetLanguages
      .mapNotNull { toolchains[it] }
      .joinToString(prefix = "TOOLCHAINS = [\n", postfix = "\n]", separator = ",\n ")

  private fun createNewExtensionsFile(fileContent: String) {
    val file = aspectsPath.resolve(Constants.EXTENSIONS_BZL)
    file.writeIfDifferent(fileContent)
  }
}
