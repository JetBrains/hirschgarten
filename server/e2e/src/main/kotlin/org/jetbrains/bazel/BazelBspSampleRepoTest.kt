package org.jetbrains.bazel

import com.intellij.openapi.util.SystemInfo
import org.jetbrains.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.install.Install
import org.jetbrains.bazel.install.cli.CliOptions
import org.jetbrains.bazel.install.cli.ProjectViewCliOptions
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.CompileParams
import org.jetbrains.bsp.protocol.CompileResult
import org.jetbrains.bsp.protocol.InverseSourcesParams
import org.jetbrains.bsp.protocol.InverseSourcesResult
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.ScalaBuildTarget
import org.jetbrains.bsp.protocol.SourceItem
import org.jetbrains.bsp.protocol.TextDocumentIdentifier
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.seconds

object BazelBspSampleRepoTest : BazelBspTestBaseScenario() {
  override fun installServer() {
    Install.runInstall(
      CliOptions(
        workspaceDir = Path(workspaceDir),
        projectViewCliOptions =
          ProjectViewCliOptions(
            bazelBinary = Path(bazelBinary),
            targets = listOf("//..."),
            enabledRules = listOf("io_bazel_rules_scala", "rules_java", "rules_jvm"),
          ),
      ),
    )
  }

  private val testClient = createTestkitClient()

  private val scalaRulesPath =
    if (isBzlmod) {
      "rules_scala${bzlmodRepoNameSeparator}$bzlmodRepoNameSeparator" +
        "scala_deps${bzlmodRepoNameSeparator}io_bazel_rules_scala"
    } else {
      "io_bazel_rules_scala"
    }
  private val scalaRulesPathVersionSuffix = if (isBzlmod) "_2_13_14" else ""
  private val scalaRulesVersion = if (isBzlmod) "2.13.14" else "2.13.6"

  private val remote_java_tools =
    if (isBzlmod) {
      "rules_java${bzlmodRepoNameSeparator}$bzlmodRepoNameSeparator" +
        "toolchains${bzlmodRepoNameSeparator}remote_java_tools"
    } else {
      "remote_java_tools"
    }

  private val bazelArch =
    if (SystemInfo.isMac) {
      "darwin_arm64"
    } else {
      "k8"
    }

  private val guavaClasspath =
    if (isBzlmod) {
      listOf(
        Path(
          "\$BAZEL_OUTPUT_BASE_PATH/execroot/_main/bazel-out/$bazelArch-fastbuild/bin/external/rules_jvm_external${bzlmodRepoNameSeparator}${bzlmodRepoNameSeparator}maven${bzlmodRepoNameSeparator}maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/code/findbugs/jsr305/3.0.2/processed_jsr305-3.0.2.jar",
        ),
        Path(
          "\$BAZEL_OUTPUT_BASE_PATH/execroot/_main/bazel-out/$bazelArch-fastbuild/bin/external/rules_jvm_external${bzlmodRepoNameSeparator}${bzlmodRepoNameSeparator}maven${bzlmodRepoNameSeparator}maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/errorprone/error_prone_annotations/2.7.1/processed_error_prone_annotations-2.7.1.jar",
        ),
        Path(
          "\$BAZEL_OUTPUT_BASE_PATH/execroot/_main/bazel-out/$bazelArch-fastbuild/bin/external/rules_jvm_external${bzlmodRepoNameSeparator}${bzlmodRepoNameSeparator}maven${bzlmodRepoNameSeparator}maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/guava/failureaccess/1.0.1/processed_failureaccess-1.0.1.jar",
        ),
        Path(
          "\$BAZEL_OUTPUT_BASE_PATH/execroot/_main/bazel-out/$bazelArch-fastbuild/bin/external/rules_jvm_external${bzlmodRepoNameSeparator}${bzlmodRepoNameSeparator}maven${bzlmodRepoNameSeparator}maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/guava/guava/31.0.1-jre/processed_guava-31.0.1-jre.jar",
        ),
        Path(
          "\$BAZEL_OUTPUT_BASE_PATH/execroot/_main/bazel-out/$bazelArch-fastbuild/bin/external/rules_jvm_external${bzlmodRepoNameSeparator}${bzlmodRepoNameSeparator}maven${bzlmodRepoNameSeparator}maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/guava/listenablefuture/9999.0-empty-to-avoid-conflict-with-guava/processed_listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar",
        ),
        Path(
          "\$BAZEL_OUTPUT_BASE_PATH/execroot/_main/bazel-out/$bazelArch-fastbuild/bin/external/rules_jvm_external${bzlmodRepoNameSeparator}${bzlmodRepoNameSeparator}maven${bzlmodRepoNameSeparator}maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/j2objc/j2objc-annotations/1.3/processed_j2objc-annotations-1.3.jar",
        ),
        Path(
          "\$BAZEL_OUTPUT_BASE_PATH/execroot/_main/bazel-out/$bazelArch-fastbuild/bin/external/rules_jvm_external${bzlmodRepoNameSeparator}${bzlmodRepoNameSeparator}maven${bzlmodRepoNameSeparator}maven/v1/https/cache-redirector.jetbrains.com/maven-central/org/checkerframework/checker-qual/3.12.0/processed_checker-qual-3.12.0.jar",
        ),
      )
    } else {
      listOf(Path("\$BAZEL_OUTPUT_BASE_PATH/external/guava/guava-28.0-jre.jar"))
    }

  // TODO: https://youtrack.jetbrains.com/issue/BAZEL-95
  @JvmStatic
  fun main(args: Array<String>) = executeScenario()

  override fun scenarioSteps(): List<BazelBspTestScenarioStep> =
    listOf(
      compareWorkspaceTargetsResults(),
      inverseSourcesResults(),
      buildTargetWithOriginId(),
    )

  private fun compareWorkspaceTargetsResults(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep(
      "compare workspace targets results",
    ) { testClient.testWorkspaceTargets(30.seconds, expectedWorkspaceBuildTargetsResult()) }

  private fun inverseSourcesResults(): BazelBspTestScenarioStep {
    val inverseSourcesDocument = TextDocumentIdentifier(Path("\$WORKSPACE/java_targets/JavaBinary.java"))
    val expectedInverseSourcesResult =
      InverseSourcesResult(listOf(Label.parse("$targetPrefix//java_targets:java_binary")))
    val inverseSourcesParams = InverseSourcesParams(inverseSourcesDocument)
    return BazelBspTestScenarioStep(
      "inverse sources results",
    ) {
      testClient.testInverseSources(
        30.seconds,
        inverseSourcesParams,
        expectedInverseSourcesResult,
      )
    }
  }

  private fun buildTargetWithOriginId(): BazelBspTestScenarioStep {
    val targetId = Label.parse("$targetPrefix//java_targets:java_library")
    val originId = "build-target-origin-id"
    return buildTarget(targetId, originId)
  }

  private fun buildTarget(targetId: Label, originId: String): BazelBspTestScenarioStep {
    val params =
      CompileParams(listOf(targetId), originId = originId)

    val expectedResult =
      CompileResult(BazelStatus.SUCCESS)

    return BazelBspTestScenarioStep("build $targetId with origin id: $originId") {
      testClient.testCompile(
        20.seconds,
        params = params,
        expectedResult = expectedResult,
        expectedDiagnostics = emptyList(),
      )
    }
  }

  override fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult {
    val architecturePart = if (System.getProperty("os.arch") == "aarch64") "_aarch64" else ""
    val javaHomeBazel5And6 = Path("\$BAZEL_OUTPUT_BASE_PATH/external/remotejdk11_\$OS$architecturePart/")
    val javaHomeBazel7 =
      Path(
        "\$BAZEL_OUTPUT_BASE_PATH/external/rules_java${bzlmodRepoNameSeparator}${bzlmodRepoNameSeparator}toolchains${bzlmodRepoNameSeparator}remotejdk11_\$OS$architecturePart/",
      )
    val javaHome = if (isBzlmod) javaHomeBazel7 else javaHomeBazel5And6
    val jvmBuildTarget =
      JvmBuildTarget(
        javaHome = javaHome,
        javaVersion = "11",
      )

    val jvmBuildTargetWithFlag =
      JvmBuildTarget(
        javaHome = javaHome,
        javaVersion = if (isBzlmod) "17" else "8",
      )

    val javaTargetsJavaBinary =
      RawBuildTarget(
        Label.parse("$targetPrefix//java_targets:java_binary"),
        listOf(),
        emptyList(),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        baseDirectory = Path("\$WORKSPACE/java_targets/"),
        data = jvmBuildTarget,
        sources =
          listOf(
            SourceItem(
              path = Path("\$WORKSPACE/java_targets/JavaBinary.java"),
              generated = false,
              jvmPackagePrefix = "java_targets",
            ),
          ),
        resources = emptyList(),
      )
    val javaTargetsJavaBinaryWithFlag =
      RawBuildTarget(
        Label.parse("$targetPrefix//java_targets:java_binary_with_flag"),
        listOf(),
        emptyList(),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        baseDirectory = Path("\$WORKSPACE/java_targets/"),
        data = jvmBuildTargetWithFlag,
        sources =
          listOf(
            SourceItem(
              path = Path("\$WORKSPACE/java_targets/JavaBinaryWithFlag.java"),
              generated = false,
              jvmPackagePrefix = "java_targets",
            ),
          ),
        resources = emptyList(),
      )
    val scalaBuildTarget =
      ScalaBuildTarget(
        scalaRulesVersion,
        listOf(
          Path(
            "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_compiler$scalaRulesPathVersionSuffix/scala-compiler-$scalaRulesVersion.jar",
          ),
          Path(
            "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_library$scalaRulesPathVersionSuffix/scala-library-$scalaRulesVersion.jar",
          ),
          Path(
            "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_reflect$scalaRulesPathVersionSuffix/scala-reflect-$scalaRulesVersion.jar",
          ),
        ),
        jvmBuildTarget = jvmBuildTarget,
        scalacOptions = listOf(),
      )

    val scalaTargetsScalaBinary =
      RawBuildTarget(
        Label.parse("$targetPrefix//scala_targets:scala_binary"),
        listOf(),
        listOf(
          Label.synthetic("scala-compiler-$scalaRulesVersion.jar"),
          Label.synthetic("scala-library-$scalaRulesVersion.jar"),
          Label.synthetic("scala-reflect-$scalaRulesVersion.jar"),
        ),
        TargetKind(
          kindString = "scala_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.JAVA, LanguageClass.SCALA),
        ),
        baseDirectory = Path("\$WORKSPACE/scala_targets/"),
        data = scalaBuildTarget.copy(scalacOptions = listOf("-target:jvm-1.8")),
        sources =
          listOf(
            SourceItem(
              path = Path("\$WORKSPACE/scala_targets/ScalaBinary.scala"),
              generated = false,
              jvmPackagePrefix = "scala_targets",
            ),
          ),
        resources = emptyList(),
      )
    val javaTargetsSubpackageSubpackage =
      RawBuildTarget(
        Label.parse("$targetPrefix//java_targets/subpackage:java_library"),
        listOf(),
        emptyList(),
        TargetKind(
          kindString = "java_library",
          ruleType = RuleType.LIBRARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        baseDirectory = Path("\$WORKSPACE/java_targets/subpackage/"),
        data = jvmBuildTarget,
        sources =
          listOf(
            SourceItem(
              path = Path("\$WORKSPACE/java_targets/subpackage/JavaLibrary2.java"),
              generated = false,
              jvmPackagePrefix = "java_targets.subpackage",
            ),
          ),
        resources = emptyList(),
      )
    val javaTargetsJavaLibrary =
      RawBuildTarget(
        Label.parse("$targetPrefix//java_targets:java_library"),
        listOf(),
        listOf(),
        TargetKind(
          kindString = "java_library",
          ruleType = RuleType.LIBRARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        baseDirectory = Path("\$WORKSPACE/java_targets/"),
        data = jvmBuildTarget,
        sources =
          listOf(
            SourceItem(
              path = Path("\$WORKSPACE/java_targets/JavaLibrary.java"),
              generated = false,
              jvmPackagePrefix = "java_targets",
            ),
          ),
        resources = emptyList(),
      )
    val targetWithoutJvmFlagsBinary =
      RawBuildTarget(
        Label.parse("$targetPrefix//target_without_jvm_flags:binary"),
        listOf(),
        listOf(
          Label.synthetic("scala-compiler-$scalaRulesVersion.jar"),
          Label.synthetic("scala-library-$scalaRulesVersion.jar"),
          Label.synthetic("scala-reflect-$scalaRulesVersion.jar"),
        ),
        TargetKind(
          kindString = "scala_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.JAVA, LanguageClass.SCALA),
        ),
        baseDirectory = Path("\$WORKSPACE/target_without_jvm_flags/"),
        data = scalaBuildTarget,
        sources =
          listOf(
            SourceItem(
              path = Path("\$WORKSPACE/target_without_jvm_flags/Example.scala"),
              generated = false,
              jvmPackagePrefix = "target_without_jvm_flags",
            ),
          ),
        resources = emptyList(),
      )
    val targetWithoutMainClassLibrary =
      RawBuildTarget(
        Label.parse("$targetPrefix//target_without_main_class:library"),
        listOf(),
        listOf(
          Label.synthetic("scala-compiler-$scalaRulesVersion.jar"),
          Label.synthetic("scala-library-$scalaRulesVersion.jar"),
          Label.synthetic("scala-reflect-$scalaRulesVersion.jar"),
        ),
        TargetKind(
          kindString = "scala_library",
          ruleType = RuleType.LIBRARY,
          languageClasses = setOf(LanguageClass.JAVA, LanguageClass.SCALA),
        ),
        baseDirectory = Path("\$WORKSPACE/target_without_main_class/"),
        data = scalaBuildTarget,
        sources =
          listOf(
            SourceItem(
              path = Path("\$WORKSPACE/target_without_main_class/Example.scala"),
              generated = false,
              jvmPackagePrefix = "target_without_main_class",
            ),
          ),
        resources = emptyList(),
      )
    val targetWithoutArgsBinary =
      RawBuildTarget(
        Label.parse("$targetPrefix//target_without_args:binary"),
        listOf(),
        listOf(
          Label.synthetic("scala-compiler-$scalaRulesVersion.jar"),
          Label.synthetic("scala-library-$scalaRulesVersion.jar"),
          Label.synthetic("scala-reflect-$scalaRulesVersion.jar"),
        ),
        TargetKind(
          kindString = "scala_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.JAVA, LanguageClass.SCALA),
        ),
        baseDirectory = Path("\$WORKSPACE/target_without_args/"),
        data = scalaBuildTarget,
        sources =
          listOf(
            SourceItem(
              path = Path("\$WORKSPACE/target_without_args/Example.scala"),
              generated = false,
              jvmPackagePrefix = "target_without_args",
            ),
          ),
        resources = emptyList(),
      )
    val guavaDepBazel5And6 = "@guava//:guava"
    val guavaDepBazel7 =
      "@@rules_jvm_external${bzlmodRepoNameSeparator}$bzlmodRepoNameSeparator" +
        "maven${bzlmodRepoNameSeparator}maven//:com_google_guava_guava"
    val guavaDep = if (isBzlmod) guavaDepBazel7 else guavaDepBazel5And6
    val targetWithDependencyJavaBinary =
      RawBuildTarget(
        Label.parse("$targetPrefix//target_with_dependency:java_binary"),
        listOf(),
        listOf(
          Label.parse("$targetPrefix//java_targets:java_library_exported"),
          Label.parse(guavaDep),
        ),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        baseDirectory = Path("\$WORKSPACE/target_with_dependency/"),
        data = jvmBuildTarget,
        sources =
          listOf(
            SourceItem(
              path = Path("\$WORKSPACE/target_with_dependency/JavaBinary.java"),
              generated = false,
              jvmPackagePrefix = "target_with_dependency",
            ),
          ),
        resources = emptyList(),
      )
    val scalaTargetsScalaTest =
      RawBuildTarget(
        Label.parse("$targetPrefix//scala_targets:scala_test"),
        listOf(),
        listOf(
          Label.synthetic("scala-compiler-$scalaRulesVersion.jar"),
          Label.synthetic("scala-library-$scalaRulesVersion.jar"),
          Label.synthetic("scala-reflect-$scalaRulesVersion.jar"),
          Label.synthetic("librunner.jar"),
          Label.synthetic("scalactic_2.13-3.2.9.jar"),
          Label.synthetic("scalatest-compatible-3.2.9.jar"),
          Label.synthetic("scalatest-core_2.13-3.2.9.jar"),
          Label.synthetic("scalatest-featurespec_2.13-3.2.9.jar"),
          Label.synthetic("scalatest-flatspec_2.13-3.2.9.jar"),
          Label.synthetic("scalatest-freespec_2.13-3.2.9.jar"),
          Label.synthetic("scalatest-funspec_2.13-3.2.9.jar"),
          Label.synthetic("scalatest-funsuite_2.13-3.2.9.jar"),
          Label.synthetic("scalatest-matchers-core_2.13-3.2.9.jar"),
          Label.synthetic("scalatest-mustmatchers_2.13-3.2.9.jar"),
          Label.synthetic("scalatest-shouldmatchers_2.13-3.2.9.jar"),
          Label.synthetic("scalatest_2.13-3.2.9.jar"),
          Label.synthetic("test_reporter.jar"),
        ),
        TargetKind(
          kindString = "scala_test",
          ruleType = RuleType.TEST,
          languageClasses = setOf(LanguageClass.JAVA, LanguageClass.SCALA),
        ),
        baseDirectory = Path("\$WORKSPACE/scala_targets/"),
        data = scalaBuildTarget,
        sources =
          listOf(
            SourceItem(
              path = Path("\$WORKSPACE/scala_targets/ScalaTest.scala"),
              generated = false,
              jvmPackagePrefix = "scala_targets",
            ),
          ),
        resources = emptyList(),
      )
    val targetWithResourcesJavaBinary =
      RawBuildTarget(
        Label.parse("$targetPrefix//target_with_resources:java_binary"),
        listOf(),
        emptyList(),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        baseDirectory = Path("\$WORKSPACE/target_with_resources/"),
        data = jvmBuildTarget,
        sources =
          listOf(
            SourceItem(
              path = Path("\$WORKSPACE/target_with_resources/JavaBinary.java"),
              generated = false,
              jvmPackagePrefix = "target_with_resources",
            ),
          ),
        resources =
          listOf(
            Path("\$WORKSPACE/target_with_resources/file1.txt"),
            Path("\$WORKSPACE/target_with_resources/file2.txt"),
          ),
      )
    val javaTargetsJavaLibraryExported =
      RawBuildTarget(
        Label.parse("$targetPrefix//java_targets:java_library_exported"),
        listOf(),
        listOf(
          Label.parse("$targetPrefix//java_targets/subpackage:java_library"),
          Label.parse("$targetPrefix//java_targets:java_library_exported_output_jars"),
        ),
        TargetKind(
          kindString = "java_library",
          ruleType = RuleType.LIBRARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        baseDirectory = Path("\$WORKSPACE/java_targets/"),
        data = jvmBuildTarget,
        sources =
          listOf(), // TODO: why empty?
        resources = emptyList(),
      )
    val environmentVariablesJavaLibrary =
      RawBuildTarget(
        Label.parse("$targetPrefix//environment_variables:java_binary"),
        listOf(),
        emptyList(),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        baseDirectory = Path("\$WORKSPACE/environment_variables/"),
        data = jvmBuildTarget,
        sources =
          listOf(
            SourceItem(
              path = Path("\$WORKSPACE/environment_variables/JavaEnv.java"),
              generated = false,
              jvmPackagePrefix = "environment_variables",
            ),
          ),
        resources = emptyList(),
      )
    val environmentVariablesJavaTest =
      RawBuildTarget(
        Label.parse("$targetPrefix//environment_variables:java_test"),
        listOf(),
        emptyList(),
        TargetKind(
          kindString = "java_test",
          ruleType = RuleType.TEST,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        baseDirectory = Path("\$WORKSPACE/environment_variables/"),
        data = jvmBuildTarget,
        sources =
          listOf(
            SourceItem(
              path = Path("\$WORKSPACE/environment_variables/JavaTest.java"),
              generated = false,
              jvmPackagePrefix = "environment_variables",
            ),
          ),
        resources = emptyList(),
      )
    val targetWithJavacExportsJavaLibrary =
      RawBuildTarget(
        Label.parse("$targetPrefix//target_with_javac_exports:java_library"),
        listOf(),
        emptyList(),
        TargetKind(
          kindString = "java_library",
          ruleType = RuleType.LIBRARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        baseDirectory = Path("\$WORKSPACE/target_with_javac_exports/"),
        data = jvmBuildTarget,
        sources =
          listOf(
            SourceItem(
              path = Path("\$WORKSPACE/target_with_javac_exports/JavaLibrary.java"),
              generated = false,
              jvmPackagePrefix = "target_with_javac_exports",
            ),
          ),
        resources = emptyList(),
      )

    val nonModuleTargets =
      listOf(
        RawBuildTarget(
          Label.parse("@//genrule:foo"),
          listOf(),
          emptyList(),
          kind =
            TargetKind(
              ruleType = RuleType.BINARY,
              kindString = "genrule",
              languageClasses = setOf(),
            ),
          baseDirectory = Path("$workspaceDir/genrule/"),
          sources = emptyList(),
          resources = emptyList(),
        ),
        RawBuildTarget(
          Label.parse("@//target_without_java_info:filegroup"),
          listOf(),
          emptyList(),
          kind =
            TargetKind(
              ruleType = RuleType.BINARY,
              kindString = "filegroup",
              languageClasses = emptySet(),
            ),
          baseDirectory = Path("$workspaceDir/target_without_java_info/"),
          sources = emptyList(),
          resources = emptyList(),
        ),
        RawBuildTarget(
          Label.parse("@//target_without_java_info:genrule"),
          listOf(),
          emptyList(),
          kind =
            TargetKind(
              ruleType = RuleType.BINARY,
              kindString = "genrule",
              languageClasses = emptySet(),
            ),
          baseDirectory = Path("$workspaceDir/target_without_java_info/"),
          sources = emptyList(),
          resources = emptyList(),
        ),
      )
    return WorkspaceBuildTargetsResult(
      targets = mapOf(),
      rootTargets = setOf(),
    )
  }
}
