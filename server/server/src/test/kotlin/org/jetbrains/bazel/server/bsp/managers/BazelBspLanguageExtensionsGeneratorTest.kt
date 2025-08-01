package org.jetbrains.bazel.server.bsp.managers

import io.kotest.matchers.equals.shouldBeEqual
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.install.EnvironmentCreator
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.bsp.info.BspInfo
import org.jetbrains.bazel.server.bsp.utils.InternalAspectsResolver
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.nio.file.Path
import kotlin.io.path.createTempDirectory

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BazelBspLanguageExtensionsGeneratorTest {
  internal class BspInfoMock(private val dotBazelBspPath: Path, projectRootPath: Path) : BspInfo(projectRootPath) {
    override val bazelBspDir: Path
      get() = dotBazelBspPath
  }

  internal class BazelExternalRulesetsQueryMock(private val rulesetNames: List<String>) : BazelExternalRulesetsQuery {
    override suspend fun fetchExternalRulesetNames(): List<String> = rulesetNames
  }

  private val defaultFileContent =
    """ load("//aspects:rules/java/java_info.bzl","extract_java_toolchain","extract_java_runtime")
            load("//aspects:rules/jvm/jvm_info.bzl","extract_jvm_info")
            load("//aspects:rules/python/python_info.bzl","extract_python_info")
            EXTENSIONS=[extract_java_toolchain,extract_java_runtime,extract_jvm_info,extract_python_info]
            TOOLCHAINS=["@bazel_tools//tools/jdk:runtime_toolchain_type"]
        """.replace(" ", "").replace("\n", "")
  private val cppFileContent =
    """ load("//aspects:rules/java/java_info.bzl","extract_java_toolchain","extract_java_runtime")
            load("//aspects:rules/jvm/jvm_info.bzl","extract_jvm_info")
            load("//aspects:rules/python/python_info.bzl","extract_python_info")
            load("//aspects:rules/cpp/cpp_info.bzl","extract_cpp_info","extract_c_toolchain_info")
            EXTENSIONS=[extract_java_toolchain,extract_java_runtime,extract_jvm_info,extract_python_info,extract_cpp_info,extract_c_toolchain_info]
            TOOLCHAINS=["@bazel_tools//tools/jdk:runtime_toolchain_type"]
        """.replace(" ", "").replace("\n", "")
  private val goFileContent =
    """ load("//aspects:rules/java/java_info.bzl","extract_java_toolchain","extract_java_runtime")
            load("//aspects:rules/jvm/jvm_info.bzl","extract_jvm_info")
            load("//aspects:rules/python/python_info.bzl","extract_python_info")
            load("//aspects:rules/go/go_info.bzl","extract_go_info")
            EXTENSIONS=[extract_java_toolchain,extract_java_runtime,extract_jvm_info,extract_python_info,extract_go_info]
            TOOLCHAINS=["@bazel_tools//tools/jdk:runtime_toolchain_type","@io_bazel_rules_go//go:toolchain"]
        """.replace(" ", "").replace("\n", "")
  private val allExtensionsFileContent =
    """ load("//aspects:rules/java/java_info.bzl","extract_java_toolchain","extract_java_runtime")
            load("//aspects:rules/jvm/jvm_info.bzl","extract_jvm_info")
            load("//aspects:rules/python/python_info.bzl","extract_python_info")
            load("//aspects:rules/cpp/cpp_info.bzl","extract_cpp_info","extract_c_toolchain_info")
            load("//aspects:rules/kt/kt_info.bzl","extract_kotlin_info")
            load("//aspects:rules/scala/scala_info.bzl","extract_scala_info")
            load("//aspects:rules/go/go_info.bzl","extract_go_info")
            EXTENSIONS=[extract_java_toolchain,extract_java_runtime,extract_jvm_info,extract_python_info,extract_cpp_info,extract_c_toolchain_info,extract_kotlin_info,extract_scala_info,extract_go_info]
            TOOLCHAINS=["@bazel_tools//tools/jdk:runtime_toolchain_type","@io_bazel_rules_kotlin//kotlin/internal:kt_toolchain_type","@io_bazel_rules_scala//scala:toolchain_type","@io_bazel_rules_go//go:toolchain"]
        """.replace(" ", "").replace("\n", "")
  private val defaultRulesetLanguages =
    listOf(
      RulesetLanguage(null, Language.Java),
      RulesetLanguage(null, Language.Jvm),
      RulesetLanguage(null, Language.Python),
    )
  private val defaultToolchains =
    mapOf(
      RulesetLanguage(null, Language.Java) to Label.parse("@bazel_tools//tools/jdk:runtime_toolchain_type"),
      RulesetLanguage("io_bazel_rules_kotlin", Language.Kotlin) to Label.parse("@io_bazel_rules_kotlin//kotlin/internal:kt_toolchain_type"),
      RulesetLanguage("io_bazel_rules_scala", Language.Scala) to Label.parse("@io_bazel_rules_scala//scala:toolchain_type"),
      RulesetLanguage("io_bazel_rules_go", Language.Go) to Label.parse("@io_bazel_rules_go//go:toolchain"),
    )
  private lateinit var dotBazelBspAspectsPath: Path
  private lateinit var internalAspectsResolverMock: InternalAspectsResolver
  private val bazelRelease = BazelRelease(5)

  private fun getExtensionsFileContent(): String =
    dotBazelBspAspectsPath
      .resolve("extensions.bzl")
      .toFile()
      .readLines()
      .filterNot { it.startsWith('#') }
      .joinToString(separator = "")
      .filterNot { it.isWhitespace() }

  @BeforeAll
  fun before() {
    val tempRoot = createTempDirectory("test-workspace-root")
    tempRoot.toFile().deleteOnExit()
    val dotBazelBspPath = EnvironmentCreator(tempRoot).create()

    dotBazelBspAspectsPath = dotBazelBspPath.resolve("aspects")
    internalAspectsResolverMock = InternalAspectsResolver(BspInfoMock(dotBazelBspPath, tempRoot), bazelRelease, false)
  }

  @Test
  fun `should create the extensions dot bzl file with default imports and default toolchains`() {
    // given
    val ruleLanguages = defaultRulesetLanguages
    val bazelBspLanguageExtensionsGenerator =
      BazelBspLanguageExtensionsGenerator(internalAspectsResolverMock)

    // when
    bazelBspLanguageExtensionsGenerator.generateLanguageExtensions(ruleLanguages, defaultToolchains)

    // then
    val fileContent = getExtensionsFileContent()
    fileContent shouldBeEqual defaultFileContent
  }

  @Test
  fun `should create the extensions dot bzl file with one import and one toolchain (cpp)`() {
    // given
    val rulesetLanguages =
      defaultRulesetLanguages +
        listOf(
          RulesetLanguage("rules_cc", Language.Cpp),
        )
    BazelExternalRulesetsQueryMock(listOf("rules_cc"))
    val bazelBspLanguageExtensionsGenerator =
      BazelBspLanguageExtensionsGenerator(internalAspectsResolverMock)

    // when
    bazelBspLanguageExtensionsGenerator.generateLanguageExtensions(rulesetLanguages, defaultToolchains)

    // then
    val fileContent = getExtensionsFileContent()
    fileContent shouldBeEqual cppFileContent
  }

  @Test
  fun `should create the extensions dot bzl file with one import and one toolchain (go)`() {
    // given
    val rulesetLanguages =
      defaultRulesetLanguages +
        listOf(
          RulesetLanguage("io_bazel_rules_go", Language.Go),
        )
    BazelExternalRulesetsQueryMock(listOf("io_bazel_rules_go"))
    val bazelBspLanguageExtensionsGenerator =
      BazelBspLanguageExtensionsGenerator(internalAspectsResolverMock)

    // when
    bazelBspLanguageExtensionsGenerator.generateLanguageExtensions(rulesetLanguages, defaultToolchains)

    // then
    val fileContent = getExtensionsFileContent()
    fileContent shouldBeEqual goFileContent
  }

  @Test
  fun `should create the extensions dot bzl file with all possible imports and toolchains`() {
    // given
    val rulesetLanguages =
      defaultRulesetLanguages +
        listOf(
          RulesetLanguage("rules_cc", Language.Cpp),
          RulesetLanguage("io_bazel_rules_kotlin", Language.Kotlin),
          RulesetLanguage("io_bazel_rules_scala", Language.Scala),
          RulesetLanguage("io_bazel_rules_go", Language.Go),
        )
    val bazelBspLanguageExtensionsGenerator =
      BazelBspLanguageExtensionsGenerator(internalAspectsResolverMock)

    // when
    bazelBspLanguageExtensionsGenerator.generateLanguageExtensions(rulesetLanguages, defaultToolchains)

    // then
    val fileContent = getExtensionsFileContent()
    fileContent shouldBeEqual allExtensionsFileContent
  }

  @Test
  fun `should correctly overwrite the extensions dot bzl file`() {
    // given
    val rulesetLanguages =
      defaultRulesetLanguages +
        listOf(
          RulesetLanguage("rules_cc", Language.Cpp),
          RulesetLanguage("io_bazel_rules_kotlin", Language.Kotlin),
          RulesetLanguage("io_bazel_rules_scala", Language.Scala),
          RulesetLanguage("io_bazel_rules_go", Language.Go),
        )
    val emptyBazelBspLanguageExtensionsGenerator =
      BazelBspLanguageExtensionsGenerator(internalAspectsResolverMock)
    val allBazelBspLanguageExtensionsGenerator =
      BazelBspLanguageExtensionsGenerator(
        internalAspectsResolverMock,
      )

    // when
    allBazelBspLanguageExtensionsGenerator.generateLanguageExtensions(rulesetLanguages, defaultToolchains)
    var fileContent = getExtensionsFileContent()

    // then
    fileContent shouldBeEqual allExtensionsFileContent

    // when
    emptyBazelBspLanguageExtensionsGenerator.generateLanguageExtensions(defaultRulesetLanguages, defaultToolchains)

    // then
    fileContent = getExtensionsFileContent()
    fileContent shouldBeEqual defaultFileContent
  }
}
