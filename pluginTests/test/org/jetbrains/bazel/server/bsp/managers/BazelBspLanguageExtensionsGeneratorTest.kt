package org.jetbrains.bazel.server.bsp.managers

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.install.EnvironmentCreator
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

  private val noAutoloads : List<String> = listOf()
  private val fullAutoloads = listOf("rules_python", "rules_java", "rules_android")
  private val defaultRepoMapping = BzlmodRepoMapping(
    mapOf(),
    mapOf(
      "rules_java" to "rules_java+",
      "rules_jvm" to "rules_jvm+",
      "rules_python" to "rules_python+",
    ),
    mapOf(),
  )
  private val emptyRepoMapping = BzlmodRepoMapping(mapOf(), mapOf(), mapOf())
  private val goRepoMapping = BzlmodRepoMapping(mapOf(), mapOf("rules_to" to "rules_go+"), mapOf())
  private val fullRepoMapping =
    BzlmodRepoMapping(
      mapOf(),
      mapOf(
        "rules_java" to "rules_java+",
        "rules_jvm" to "rules_jvm+",
        "rules_python" to "rules_python+",
        "io_bazel_rules_go" to "io_bazel_rules_go+",
        "io_bazel_rules_scala" to "io_bazel_rules_scala+",
        "io_bazel_rules_kotlin" to "io_bazel_rules_kotlin+",
      ),
      mapOf(),
    )
  val kotlinCompletedRepoMapping =
    BzlmodRepoMapping(mapOf(), mapOf("io_bazel_rules_kotlin" to "rules_kotlin+", "rules_java" to "rules_java+"), mapOf())

  private val defaultFileContent =
    """ load("@@rules_java+//java/common:java_info.bzl", "JavaInfo")
            load("//.bazelbsp/aspects:rules/java/java_info.bzl","extract_java_toolchain","extract_java_runtime")
            load("//.bazelbsp/aspects:rules/jvm/jvm_info.bzl","extract_jvm_info")
            load("//.bazelbsp/aspects:rules/python/python_info.bzl","extract_python_info")
            EXTENSIONS=[extract_java_toolchain,extract_java_runtime,extract_jvm_info,extract_python_info]
            TOOLCHAINS=[config_common.toolchain_type("@bazel_tools//tools/jdk:runtime_toolchain_type", mandatory = False)]
            REQUIRED_ASPECT_PROVIDERS=[[JavaInfo]]
        """.replace(" ", "").replace("\n", "")
  private val autoloadFileContent =
    """ load("//.bazelbsp/aspects:rules/java/java_info.bzl","extract_java_toolchain","extract_java_runtime")
            load("//.bazelbsp/aspects:rules/jvm/jvm_info.bzl","extract_jvm_info")
            load("//.bazelbsp/aspects:rules/python/python_info.bzl","extract_python_info")
            EXTENSIONS=[extract_java_toolchain,extract_java_runtime,extract_jvm_info,extract_python_info]
            TOOLCHAINS=[config_common.toolchain_type("@bazel_tools//tools/jdk:runtime_toolchain_type", mandatory = False)]
            REQUIRED_ASPECT_PROVIDERS=[[JavaInfo]]
        """.replace(" ", "").replace("\n", "")
  private val goFileContent =
    """ load("//.bazelbsp/aspects:rules/java/java_info.bzl","extract_java_toolchain","extract_java_runtime")
            load("//.bazelbsp/aspects:rules/jvm/jvm_info.bzl","extract_jvm_info")
            load("//.bazelbsp/aspects:rules/python/python_info.bzl","extract_python_info")
            load("//.bazelbsp/aspects:rules/go/go_info.bzl","extract_go_info")
            EXTENSIONS=[extract_java_toolchain,extract_java_runtime,extract_jvm_info,extract_python_info,extract_go_info]
            TOOLCHAINS=[config_common.toolchain_type("@bazel_tools//tools/jdk:runtime_toolchain_type", mandatory = False),"@io_bazel_rules_go//go:toolchain"]
            REQUIRED_ASPECT_PROVIDERS=[]
        """.replace(" ", "").replace("\n", "")
  private val allExtensionsFileContent =
    """ load("@@rules_java+//java/common:java_info.bzl", "JavaInfo")
            load("//.bazelbsp/aspects:rules/java/java_info.bzl","extract_java_toolchain","extract_java_runtime")
            load("//.bazelbsp/aspects:rules/jvm/jvm_info.bzl","extract_jvm_info")
            load("//.bazelbsp/aspects:rules/python/python_info.bzl","extract_python_info")
            load("//.bazelbsp/aspects:rules/kt/kt_info.bzl","extract_kotlin_info")
            load("//.bazelbsp/aspects:rules/scala/scala_info.bzl","extract_scala_info")
            load("//.bazelbsp/aspects:rules/go/go_info.bzl","extract_go_info")
            EXTENSIONS=[extract_java_toolchain,extract_java_runtime,extract_jvm_info,extract_python_info,extract_kotlin_info,extract_scala_info,extract_go_info]
            TOOLCHAINS=[config_common.toolchain_type("@bazel_tools//tools/jdk:runtime_toolchain_type", mandatory = False),"@io_bazel_rules_kotlin//kotlin/internal:kt_toolchain_type","@io_bazel_rules_scala//scala:toolchain_type","@io_bazel_rules_go//go:toolchain"]
            REQUIRED_ASPECT_PROVIDERS=[[JavaInfo]]
        """.replace(" ", "").replace("\n", "")
  private val kotlinFileContent =
    """ load("@@rules_java+//java/common:java_info.bzl", "JavaInfo")
            load("//.bazelbsp/aspects:rules/java/java_info.bzl","extract_java_toolchain","extract_java_runtime")
            load("//.bazelbsp/aspects:rules/jvm/jvm_info.bzl","extract_jvm_info")
            load("//.bazelbsp/aspects:rules/kt/kt_info.bzl","extract_kotlin_info")
            EXTENSIONS=[extract_java_toolchain,extract_java_runtime,extract_jvm_info,extract_kotlin_info]
            TOOLCHAINS=[config_common.toolchain_type("@bazel_tools//tools/jdk:runtime_toolchain_type", mandatory = False),"@io_bazel_rules_kotlin//kotlin/internal:kt_toolchain_type"]
            REQUIRED_ASPECT_PROVIDERS=[[JavaInfo]]
    """.replace(" ", "").replace("\n", "")
  private val defaultRulesetLanguages =
    listOf(
      RulesetLanguage(null, Language.Java),
      RulesetLanguage(null, Language.Jvm),
      RulesetLanguage(null, Language.Python),
    )
  private val defaultToolchains =
    mapOf(
      RulesetLanguage(
        null,
        Language.Java,
      ) to """config_common.toolchain_type("@bazel_tools//tools/jdk:runtime_toolchain_type", mandatory = False)""",
      RulesetLanguage(ApparentRulesetName("io_bazel_rules_kotlin"), Language.Kotlin) to """"@io_bazel_rules_kotlin//kotlin/internal:kt_toolchain_type"""",
      RulesetLanguage(ApparentRulesetName("io_bazel_rules_scala"), Language.Scala) to """"@io_bazel_rules_scala//scala:toolchain_type"""",
      RulesetLanguage(ApparentRulesetName("io_bazel_rules_go"), Language.Go) to """"@io_bazel_rules_go//go:toolchain"""",
    )
  private lateinit var dotBazelBspAspectsPath: Path
  private lateinit var internalAspectsResolverMock: InternalAspectsResolver

  private fun getExtensionsFileContent(): String =
    dotBazelBspAspectsPath
      .resolve(Constants.EXTENSIONS_BZL)
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
    internalAspectsResolverMock = InternalAspectsResolver(BspInfoMock(dotBazelBspPath, tempRoot))
  }

  @Test
  fun `should create the extensions dot bzl file with default imports and default toolchains`() {
    // given
    val ruleLanguages = defaultRulesetLanguages
    val bazelBspLanguageExtensionsGenerator =
      BazelBspLanguageExtensionsGenerator(internalAspectsResolverMock)

    // when
    bazelBspLanguageExtensionsGenerator.generateLanguageExtensions(ruleLanguages, defaultToolchains, defaultRepoMapping, noAutoloads)

    // then
    val fileContent = getExtensionsFileContent()
    fileContent shouldBe defaultFileContent
  }

  @Test
  fun `should create the extensions dot bzl file with one import and one toolchain (go)`() {
    // given
    val rulesetLanguages =
      defaultRulesetLanguages +
      listOf(
        RulesetLanguage(ApparentRulesetName("io_bazel_rules_go"), Language.Go),
      )
    BazelExternalRulesetsQueryMock(listOf("io_bazel_rules_go"))
    val bazelBspLanguageExtensionsGenerator =
      BazelBspLanguageExtensionsGenerator(internalAspectsResolverMock)

    // when
    bazelBspLanguageExtensionsGenerator.generateLanguageExtensions(rulesetLanguages, defaultToolchains, goRepoMapping, noAutoloads)

    // then
    val fileContent = getExtensionsFileContent()
    fileContent shouldBe goFileContent
  }

  @Test
  fun `should create the extensions dot bzl file with all possible imports and toolchains`() {
    // given
    val rulesetLanguages =
      defaultRulesetLanguages +
      listOf(
        RulesetLanguage(ApparentRulesetName("io_bazel_rules_kotlin"), Language.Kotlin),
        RulesetLanguage(ApparentRulesetName("io_bazel_rules_scala"), Language.Scala),
        RulesetLanguage(ApparentRulesetName("io_bazel_rules_go"), Language.Go),
      )
    val bazelBspLanguageExtensionsGenerator =
      BazelBspLanguageExtensionsGenerator(internalAspectsResolverMock)

    // when
    bazelBspLanguageExtensionsGenerator.generateLanguageExtensions(rulesetLanguages, defaultToolchains, fullRepoMapping, noAutoloads)

    // then
    val fileContent = getExtensionsFileContent()
    fileContent shouldBe allExtensionsFileContent
  }

  @Test
  fun `should correctly overwrite the extensions dot bzl file`() {
    // given
    val rulesetLanguages =
      defaultRulesetLanguages +
      listOf(
        RulesetLanguage(ApparentRulesetName("io_bazel_rules_kotlin"), Language.Kotlin),
        RulesetLanguage(ApparentRulesetName("io_bazel_rules_scala"), Language.Scala),
        RulesetLanguage(ApparentRulesetName("io_bazel_rules_go"), Language.Go),
      )
    val emptyBazelBspLanguageExtensionsGenerator =
      BazelBspLanguageExtensionsGenerator(internalAspectsResolverMock)
    val allBazelBspLanguageExtensionsGenerator =
      BazelBspLanguageExtensionsGenerator(
        internalAspectsResolverMock,
      )

    // when
    allBazelBspLanguageExtensionsGenerator.generateLanguageExtensions(rulesetLanguages, defaultToolchains, fullRepoMapping, noAutoloads)
    var fileContent = getExtensionsFileContent()

    // then
    fileContent shouldBe allExtensionsFileContent

    // when
    emptyBazelBspLanguageExtensionsGenerator.generateLanguageExtensions(defaultRulesetLanguages, defaultToolchains, fullRepoMapping, noAutoloads)

    // then
    fileContent = getExtensionsFileContent()
    fileContent shouldBe defaultFileContent
  }

  @Test
  fun `should handle kotlin project correctly`() {
    val rulesetLanguages =
      listOf(
        RulesetLanguage(null, Language.Java),
        RulesetLanguage(null, Language.Jvm),
        RulesetLanguage(ApparentRulesetName("io_bazel_rules_kotlin"), Language.Kotlin),
      )
    val bazelBspLanguageExtensionsGenerator =
      BazelBspLanguageExtensionsGenerator(internalAspectsResolverMock)

    bazelBspLanguageExtensionsGenerator.generateLanguageExtensions(rulesetLanguages, defaultToolchains, kotlinCompletedRepoMapping, noAutoloads)

    val fileContent = getExtensionsFileContent()
    fileContent shouldBe kotlinFileContent
  }

  @Test
  fun `handles autoloads`() {
    val bazelBspLanguageExtensionsGenerator =
      BazelBspLanguageExtensionsGenerator(internalAspectsResolverMock)

    bazelBspLanguageExtensionsGenerator.generateLanguageExtensions(defaultRulesetLanguages, defaultToolchains, emptyRepoMapping, fullAutoloads)

    val fileContent = getExtensionsFileContent()
    fileContent shouldBe autoloadFileContent

  }
}
