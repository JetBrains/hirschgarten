package org.jetbrains.bsp.bazel.server.bsp.managers

import io.kotest.matchers.equals.shouldBeEqual
import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.jetbrains.bazel.commons.label.Label
import org.jetbrains.bsp.bazel.bazelrunner.utils.BazelRelease
import org.jetbrains.bsp.bazel.install.EnvironmentCreator
import org.jetbrains.bsp.bazel.server.bsp.info.BspInfo
import org.jetbrains.bsp.bazel.server.bsp.utils.InternalAspectsResolver
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.nio.file.Path
import kotlin.io.path.createTempDirectory

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BazelBspLanguageExtensionsGeneratorTest {
  class MockEnvironmentCreator(projectRootDir: Path) : EnvironmentCreator(projectRootDir) {
    override fun create(): Unit = Unit

    fun testCreateDotBazelBsp() = createDotBazelBsp()
  }

  internal class BspInfoMock(private val dotBazelBspPath: Path, projectRootPath: Path) : BspInfo(projectRootPath) {
    override fun bazelBspDir(): Path = dotBazelBspPath
  }

  internal class BazelExternalRulesQueryMock(private val ruleNames: List<String>) : BazelExternalRulesQuery {
    override fun fetchExternalRuleNames(cancelChecker: CancelChecker): List<String> = ruleNames
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
            TOOLCHAINS=["@bazel_tools//tools/jdk:runtime_toolchain_type","@io_bazel_rules_kotlin//kotlin/internal:kt_toolchain_type","@io_bazel_rules_scala//scala:toolchain_type","@rules_scala_annex//rules/scala:toolchain_type","@io_bazel_rules_go//go:toolchain"]
        """.replace(" ", "").replace("\n", "")
  private val defaultRuleLanguages =
    listOf(
      RuleLanguage(null, Language.Java),
      RuleLanguage(null, Language.Jvm),
      RuleLanguage(null, Language.Python),
    )
  private val defaultToolchains =
    mapOf(
      RuleLanguage(null, Language.Java) to listOf(Label.parse("@bazel_tools//tools/jdk:runtime_toolchain_type")),
      RuleLanguage("io_bazel_rules_kotlin", Language.Kotlin) to listOf(Label.parse("@io_bazel_rules_kotlin//kotlin/internal:kt_toolchain_type")),
      RuleLanguage("io_bazel_rules_scala", Language.Scala) to listOf(Label.parse("@io_bazel_rules_scala//scala:toolchain_type"), Label.parse("@rules_scala_annex//rules/scala:toolchain_type")),
      RuleLanguage("io_bazel_rules_go", Language.Go) to listOf(Label.parse("@io_bazel_rules_go//go:toolchain")),
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
    val dotBazelBspPath = MockEnvironmentCreator(tempRoot).testCreateDotBazelBsp()

    dotBazelBspAspectsPath = dotBazelBspPath.resolve("aspects")
    internalAspectsResolverMock = InternalAspectsResolver(BspInfoMock(dotBazelBspPath, tempRoot), bazelRelease, false)
  }

  @Test
  fun `should create the extensions dot bzl file with default imports and default toolchains`() {
    // given
    val ruleLanguages = defaultRuleLanguages
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
    val ruleLanguages =
      defaultRuleLanguages +
        listOf(
          RuleLanguage("rules_cc", Language.Cpp),
        )
    BazelExternalRulesQueryMock(listOf("rules_cc"))
    val bazelBspLanguageExtensionsGenerator =
      BazelBspLanguageExtensionsGenerator(internalAspectsResolverMock)

    // when
    bazelBspLanguageExtensionsGenerator.generateLanguageExtensions(ruleLanguages, defaultToolchains)

    // then
    val fileContent = getExtensionsFileContent()
    fileContent shouldBeEqual cppFileContent
  }

  @Test
  fun `should create the extensions dot bzl file with one import and one toolchain (go)`() {
    // given
    val ruleLanguages =
      defaultRuleLanguages +
        listOf(
          RuleLanguage("io_bazel_rules_go", Language.Go),
        )
    BazelExternalRulesQueryMock(listOf("io_bazel_rules_go"))
    val bazelBspLanguageExtensionsGenerator =
      BazelBspLanguageExtensionsGenerator(internalAspectsResolverMock)

    // when
    bazelBspLanguageExtensionsGenerator.generateLanguageExtensions(ruleLanguages, defaultToolchains)

    // then
    val fileContent = getExtensionsFileContent()
    fileContent shouldBeEqual goFileContent
  }

  @Test
  fun `should create the extensions dot bzl file with all possible imports and toolchains`() {
    // given
    val ruleLanguages =
      defaultRuleLanguages +
        listOf(
          RuleLanguage("rules_cc", Language.Cpp),
          RuleLanguage("io_bazel_rules_kotlin", Language.Kotlin),
          RuleLanguage("io_bazel_rules_scala", Language.Scala),
          RuleLanguage("io_bazel_rules_go", Language.Go),
        )
    val bazelBspLanguageExtensionsGenerator =
      BazelBspLanguageExtensionsGenerator(internalAspectsResolverMock)

    // when
    bazelBspLanguageExtensionsGenerator.generateLanguageExtensions(ruleLanguages, defaultToolchains)

    // then
    val fileContent = getExtensionsFileContent()
    fileContent shouldBeEqual allExtensionsFileContent
  }

  @Test
  fun `should correctly overwrite the extensions dot bzl file`() {
    // given
    val ruleLanguages =
      defaultRuleLanguages +
        listOf(
          RuleLanguage("rules_cc", Language.Cpp),
          RuleLanguage("io_bazel_rules_kotlin", Language.Kotlin),
          RuleLanguage("io_bazel_rules_scala", Language.Scala),
          RuleLanguage("io_bazel_rules_go", Language.Go),
        )
    val emptyBazelBspLanguageExtensionsGenerator =
      BazelBspLanguageExtensionsGenerator(internalAspectsResolverMock)
    val allBazelBspLanguageExtensionsGenerator =
      BazelBspLanguageExtensionsGenerator(
        internalAspectsResolverMock,
      )

    // when
    allBazelBspLanguageExtensionsGenerator.generateLanguageExtensions(ruleLanguages, defaultToolchains)
    var fileContent = getExtensionsFileContent()

    // then
    fileContent shouldBeEqual allExtensionsFileContent

    // when
    emptyBazelBspLanguageExtensionsGenerator.generateLanguageExtensions(defaultRuleLanguages, defaultToolchains)

    // then
    fileContent = getExtensionsFileContent()
    fileContent shouldBeEqual defaultFileContent
  }
}
