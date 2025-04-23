package org.jetbrains.bazel

import com.intellij.openapi.util.SystemInfo
import org.jetbrains.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.install.Install
import org.jetbrains.bazel.install.cli.CliOptions
import org.jetbrains.bazel.install.cli.ProjectViewCliOptions
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.CompileParams
import org.jetbrains.bsp.protocol.CompileResult
import org.jetbrains.bsp.protocol.DependencySourcesItem
import org.jetbrains.bsp.protocol.DependencySourcesParams
import org.jetbrains.bsp.protocol.DependencySourcesResult
import org.jetbrains.bsp.protocol.InverseSourcesParams
import org.jetbrains.bsp.protocol.InverseSourcesResult
import org.jetbrains.bsp.protocol.JavacOptionsItem
import org.jetbrains.bsp.protocol.JavacOptionsParams
import org.jetbrains.bsp.protocol.JavacOptionsResult
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.JvmEnvironmentItem
import org.jetbrains.bsp.protocol.JvmMainClass
import org.jetbrains.bsp.protocol.JvmRunEnvironmentParams
import org.jetbrains.bsp.protocol.JvmRunEnvironmentResult
import org.jetbrains.bsp.protocol.JvmTestEnvironmentParams
import org.jetbrains.bsp.protocol.JvmTestEnvironmentResult
import org.jetbrains.bsp.protocol.ScalaBuildTarget
import org.jetbrains.bsp.protocol.ScalaPlatform
import org.jetbrains.bsp.protocol.SourceItem
import org.jetbrains.bsp.protocol.StatusCode
import org.jetbrains.bsp.protocol.TextDocumentIdentifier
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.minutes
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
      resolveProject(),
      compareWorkspaceTargetsResults(),
      inverseSourcesResults(),
      dependencySourcesResults(),
      jvmRunEnvironment(),
      jvmTestEnvironment(),
      javacOptionsResult(),
      buildTargetWithOriginId(),
    )

  private fun resolveProject(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep(
      "resolve project",
    ) { testClient.testResolveProject(3.minutes) }

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

  private fun dependencySourcesResults(): BazelBspTestScenarioStep {
    val javaTargetsJavaBinary =
      DependencySourcesItem(
        Label.parse("$targetPrefix//java_targets:java_binary"),
        emptyList(),
      )
    val javaTargetsJavaBinaryWithFlag =
      DependencySourcesItem(
        Label.parse("$targetPrefix//java_targets:java_binary_with_flag"),
        emptyList(),
      )
    val javaTargetsJavaLibrary =
      DependencySourcesItem(
        Label.parse("$targetPrefix//java_targets:java_library"),
        emptyList(),
      )
    val targetWithDependencyJavaBinary =
      DependencySourcesItem(
        Label.parse("$targetPrefix//target_with_dependency:java_binary"),
        emptyList(),
      )
    val javaTargetsSubpackageJavaLibrary =
      DependencySourcesItem(
        Label.parse("$targetPrefix//java_targets/subpackage:java_library"),
        emptyList(),
      )
    val scalaTargetsScalaBinary =
      DependencySourcesItem(
        Label.parse("$targetPrefix//scala_targets:scala_binary"),
        emptyList(),
      )
    val scalaTargetsScalaTest =
      DependencySourcesItem(
        Label.parse("$targetPrefix//scala_targets:scala_test"),
        emptyList(),
      )
    val targetWithResourcesJavaBinary =
      DependencySourcesItem(
        Label.parse("$targetPrefix//target_with_resources:java_binary"),
        emptyList(),
      )
    val targetWithoutArgsBinary =
      DependencySourcesItem(
        Label.parse("$targetPrefix//target_without_args:binary"),
        emptyList(),
      )
    val targetWithoutJvmFlagsBinary =
      DependencySourcesItem(
        Label.parse("$targetPrefix//target_without_jvm_flags:binary"),
        emptyList(),
      )
    val targetWithoutMainClassLibrary =
      DependencySourcesItem(
        Label.parse("$targetPrefix//target_without_main_class:library"),
        emptyList(),
      )
    val javaTargetsJavaLibraryExported =
      DependencySourcesItem(
        Label.parse("$targetPrefix//java_targets:java_library_exported"),
        emptyList(),
      )
    val environmentVariablesJavaBinary =
      DependencySourcesItem(
        Label.parse("$targetPrefix//environment_variables:java_binary"),
        emptyList(),
      )
    val environmentVariablesJavaTest =
      DependencySourcesItem(
        Label.parse("$targetPrefix//environment_variables:java_test"),
        emptyList(),
      )
    val targetWithJavacExportsJavaLibrary =
      DependencySourcesItem(
        Label.parse("$targetPrefix//target_with_javac_exports:java_library"),
        emptyList(),
      )

    val nonModuleTargets =
      listOf(
        DependencySourcesItem(
          Label.parse("//genrule:foo"),
          emptyList(),
        ),
        DependencySourcesItem(
          Label.parse("//target_without_java_info:genrule"),
          emptyList(),
        ),
        DependencySourcesItem(
          Label.parse("//target_without_java_info:filegroup"),
          emptyList(),
        ),
        DependencySourcesItem(
          Label.parse("//target_without_main_class:library"),
          emptyList(),
        ),
        DependencySourcesItem(
          Label.parse("//target_without_jvm_flags:binary"),
          emptyList(),
        ),
      )

    val expectedDependencies =
      DependencySourcesResult(
        listOfNotNull(
          javaTargetsJavaBinary,
          javaTargetsJavaBinaryWithFlag,
          javaTargetsJavaLibrary,
          targetWithDependencyJavaBinary,
          javaTargetsSubpackageJavaLibrary,
          scalaTargetsScalaBinary,
          scalaTargetsScalaTest,
          targetWithResourcesJavaBinary,
          targetWithoutArgsBinary,
          targetWithoutJvmFlagsBinary,
          targetWithoutMainClassLibrary,
          javaTargetsJavaLibraryExported,
          environmentVariablesJavaBinary,
          environmentVariablesJavaTest,
          targetWithJavacExportsJavaLibrary.takeIf { majorBazelVersion > 5 },
        ) + nonModuleTargets,
      )
    val dependencySourcesParams = DependencySourcesParams(expectedTargetIdentifiers())
    return BazelBspTestScenarioStep(
      "dependency sources results",
    ) {
      testClient.testDependencySources(
        30.seconds,
        dependencySourcesParams,
        expectedDependencies,
      )
    }
  }

  // FIXME: Environment is always empty for now until we figure out how to handle it.
  // FIXME: Working directory is not an URI???
  // FIXME: Should this return only targets which are runnable?
  private fun jvmRunEnvironment(): BazelBspTestScenarioStep {
    val params = JvmRunEnvironmentParams(expectedTargetIdentifiers())
    val expectedResult =
      JvmRunEnvironmentResult(
        listOfNotNull(
          JvmEnvironmentItem(
            Label.parse("$targetPrefix//java_targets:java_binary"),
            listOf(),
            listOf("--some_flag=bazel-out/$bazelArch-fastbuild/bin/java_targets/libjava_library.jar"),
            Path("\$WORKSPACE"),
            mapOf(),
            listOf(JvmMainClass("java_targets.JavaBinary", emptyList())),
          ),
          JvmEnvironmentItem(
            Label.parse("$targetPrefix//java_targets:java_binary_with_flag"),
            listOf(),
            emptyList(),
            Path("\$WORKSPACE"),
            mapOf(),
            listOf(JvmMainClass("java_targets.JavaBinaryWithFlag", emptyList())),
          ),
          JvmEnvironmentItem(
            Label.parse("$targetPrefix//java_targets:java_library"),
            listOf(),
            emptyList(),
            Path("\$WORKSPACE"),
            mapOf(),
          ),
          JvmEnvironmentItem(
            Label.parse("$targetPrefix//target_with_dependency:java_binary"),
            guavaClasspath,
            emptyList(),
            Path("\$WORKSPACE"),
            mapOf(),
            listOf(JvmMainClass("target_with_dependency.JavaBinary", emptyList())),
          ),
          JvmEnvironmentItem(
            Label.parse("$targetPrefix//java_targets/subpackage:java_library"),
            listOf(),
            emptyList(),
            Path("\$WORKSPACE"),
            mapOf(),
          ),
          JvmEnvironmentItem(
            Label.parse("$targetPrefix//java_targets:java_library_exported"),
            listOf(),
            emptyList(),
            Path("\$WORKSPACE"),
            mapOf(),
          ),
          JvmEnvironmentItem(
            Label.parse("$targetPrefix//scala_targets:scala_binary"),
            listOf(
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_library$scalaRulesPathVersionSuffix/scala-library-$scalaRulesVersion.jar",
              ),
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_reflect$scalaRulesPathVersionSuffix/scala-reflect-$scalaRulesVersion.jar",
              ),
            ),
            listOf("-Xms2G -Xmx5G"),
            Path("\$WORKSPACE"),
            mapOf(),
            listOf(JvmMainClass("example.Example", listOf("arg1", "arg2"))),
          ),
          JvmEnvironmentItem(
            Label.parse("$targetPrefix//scala_targets:scala_test"),
            listOf(
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_library$scalaRulesPathVersionSuffix/scala-library-$scalaRulesVersion.jar",
              ),
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_reflect$scalaRulesPathVersionSuffix/scala-reflect-$scalaRulesVersion.jar",
              ),
              Path("\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalactic$scalaRulesPathVersionSuffix/scalactic_2.13-3.2.9.jar"),
              Path("\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest$scalaRulesPathVersionSuffix/scalatest_2.13-3.2.9.jar"),
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_compatible$scalaRulesPathVersionSuffix/scalatest-compatible-3.2.9.jar",
              ),
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_core$scalaRulesPathVersionSuffix/scalatest-core_2.13-3.2.9.jar",
              ),
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_featurespec$scalaRulesPathVersionSuffix/scalatest-featurespec_2.13-3.2.9.jar",
              ),
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_flatspec$scalaRulesPathVersionSuffix/scalatest-flatspec_2.13-3.2.9.jar",
              ),
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_freespec$scalaRulesPathVersionSuffix/scalatest-freespec_2.13-3.2.9.jar",
              ),
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_funspec$scalaRulesPathVersionSuffix/scalatest-funspec_2.13-3.2.9.jar",
              ),
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_funsuite$scalaRulesPathVersionSuffix/scalatest-funsuite_2.13-3.2.9.jar",
              ),
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_matchers_core$scalaRulesPathVersionSuffix/scalatest-matchers-core_2.13-3.2.9.jar",
              ),
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_mustmatchers$scalaRulesPathVersionSuffix/scalatest-mustmatchers_2.13-3.2.9.jar",
              ),
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_shouldmatchers$scalaRulesPathVersionSuffix/scalatest-shouldmatchers_2.13-3.2.9.jar",
              ),
            ),
            emptyList(),
            Path("\$WORKSPACE"),
            mapOf(),
            listOf(JvmMainClass("io.bazel.rulesscala.scala_test.Runner", emptyList())),
          ),
          JvmEnvironmentItem(
            Label.parse("$targetPrefix//target_with_resources:java_binary"),
            listOf(),
            emptyList(),
            Path("\$WORKSPACE"),
            mapOf(),
            listOf(JvmMainClass("target_with_resources.JavaBinary", emptyList())),
          ),
          JvmEnvironmentItem(
            Label.parse("$targetPrefix//target_without_args:binary"),
            listOf(
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_library$scalaRulesPathVersionSuffix/scala-library-$scalaRulesVersion.jar",
              ),
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_reflect$scalaRulesPathVersionSuffix/scala-reflect-$scalaRulesVersion.jar",
              ),
            ),
            listOf("-Xms2G -Xmx5G"),
            Path("\$WORKSPACE"),
            mapOf(),
            listOf(JvmMainClass("example.Example", emptyList())),
          ),
          JvmEnvironmentItem(
            Label.parse("$targetPrefix//target_without_jvm_flags:binary"),
            listOf(
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_library$scalaRulesPathVersionSuffix/scala-library-$scalaRulesVersion.jar",
              ),
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_reflect$scalaRulesPathVersionSuffix/scala-reflect-$scalaRulesVersion.jar",
              ),
            ),
            emptyList(),
            Path("\$WORKSPACE"),
            mapOf(),
            listOf(JvmMainClass("example.Example", listOf("arg1", "arg2"))),
          ),
          JvmEnvironmentItem(
            Label.parse("$targetPrefix//target_without_main_class:library"),
            listOf(
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_library$scalaRulesPathVersionSuffix/scala-library-$scalaRulesVersion.jar",
              ),
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_reflect$scalaRulesPathVersionSuffix/scala-reflect-$scalaRulesVersion.jar",
              ),
            ),
            emptyList(),
            Path("\$WORKSPACE"),
            mapOf(),
          ),
          JvmEnvironmentItem(
            Label.parse("$targetPrefix//environment_variables:java_binary"),
            listOf(),
            emptyList(),
            Path("\$WORKSPACE"),
            mapOf("foo1" to "val1", "foo2" to "val2"),
            listOf(JvmMainClass("environment_variables.JavaEnv", emptyList())),
          ),
          JvmEnvironmentItem(
            Label.parse("$targetPrefix//environment_variables:java_test"),
            listOf(
              Path("\$BAZEL_OUTPUT_BASE_PATH/external/$remote_java_tools/java_tools/Runner_deploy.jar"),
            ),
            emptyList(),
            Path("\$WORKSPACE"),
            mapOf(
              "foo1" to "val1",
              "foo2" to "val2",
              "foo3" to "val3",
              "foo4" to "val4",
              "target_location" to
                "bazel-out/$bazelArch-fastbuild/bin/environment_variables/java_binary bazel-out/$bazelArch-fastbuild/bin/environment_variables/java_binary.jar",
            ),
          ),
          JvmEnvironmentItem(
            Label.parse("$targetPrefix//target_with_javac_exports:java_library"),
            emptyList(),
            emptyList(),
            Path("\$WORKSPACE"),
            mapOf(),
          ).takeIf { majorBazelVersion > 5 },
        ),
      )
    return BazelBspTestScenarioStep(
      "jvm run environment results",
    ) { testClient.testJvmRunEnvironment(30.seconds, params, expectedResult) }
  }

  // FIXME: Should this return only targets that are actually testable?
  private fun jvmTestEnvironment(): BazelBspTestScenarioStep {
    val params = JvmTestEnvironmentParams(expectedTargetIdentifiers())
    val expectedResult =
      JvmTestEnvironmentResult(
        listOfNotNull(
          JvmEnvironmentItem(
            Label.parse("$targetPrefix//java_targets:java_binary"),
            listOf(),
            listOf("--some_flag=bazel-out/$bazelArch-fastbuild/bin/java_targets/libjava_library.jar"),
            Path("\$WORKSPACE"),
            mapOf(),
            listOf(JvmMainClass("java_targets.JavaBinary", emptyList())),
          ),
          JvmEnvironmentItem(
            Label.parse("$targetPrefix//java_targets:java_binary_with_flag"),
            listOf(),
            emptyList(),
            Path("\$WORKSPACE"),
            mapOf(),
            listOf(JvmMainClass("java_targets.JavaBinaryWithFlag", emptyList())),
          ),
          JvmEnvironmentItem(
            Label.parse("$targetPrefix//java_targets:java_library"),
            listOf(),
            emptyList(),
            Path("\$WORKSPACE"),
            mapOf(),
          ),
          JvmEnvironmentItem(
            Label.parse("$targetPrefix//target_with_dependency:java_binary"),
            guavaClasspath,
            emptyList(),
            Path("\$WORKSPACE"),
            mapOf(),
            listOf(JvmMainClass("target_with_dependency.JavaBinary", emptyList())),
          ),
          JvmEnvironmentItem(
            Label.parse("$targetPrefix//java_targets/subpackage:java_library"),
            listOf(),
            emptyList(),
            Path("\$WORKSPACE"),
            mapOf(),
          ),
          JvmEnvironmentItem(
            Label.parse("$targetPrefix//java_targets:java_library_exported"),
            listOf(),
            emptyList(),
            Path("\$WORKSPACE"),
            mapOf(),
          ),
          JvmEnvironmentItem(
            Label.parse("$targetPrefix//scala_targets:scala_binary"),
            listOf(
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_library$scalaRulesPathVersionSuffix/scala-library-$scalaRulesVersion.jar",
              ),
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_reflect$scalaRulesPathVersionSuffix/scala-reflect-$scalaRulesVersion.jar",
              ),
            ),
            listOf("-Xms2G -Xmx5G"),
            Path("\$WORKSPACE"),
            mapOf(),
            listOf(JvmMainClass("example.Example", listOf("arg1", "arg2"))),
          ),
          JvmEnvironmentItem(
            Label.parse("$targetPrefix//scala_targets:scala_test"),
            listOf(
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_library$scalaRulesPathVersionSuffix/scala-library-$scalaRulesVersion.jar",
              ),
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_reflect$scalaRulesPathVersionSuffix/scala-reflect-$scalaRulesVersion.jar",
              ),
              Path("\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalactic$scalaRulesPathVersionSuffix/scalactic_2.13-3.2.9.jar"),
              Path("\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest$scalaRulesPathVersionSuffix/scalatest_2.13-3.2.9.jar"),
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_compatible$scalaRulesPathVersionSuffix/scalatest-compatible-3.2.9.jar",
              ),
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_core$scalaRulesPathVersionSuffix/scalatest-core_2.13-3.2.9.jar",
              ),
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_featurespec$scalaRulesPathVersionSuffix/scalatest-featurespec_2.13-3.2.9.jar",
              ),
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_flatspec$scalaRulesPathVersionSuffix/scalatest-flatspec_2.13-3.2.9.jar",
              ),
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_freespec$scalaRulesPathVersionSuffix/scalatest-freespec_2.13-3.2.9.jar",
              ),
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_funspec$scalaRulesPathVersionSuffix/scalatest-funspec_2.13-3.2.9.jar",
              ),
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_funsuite$scalaRulesPathVersionSuffix/scalatest-funsuite_2.13-3.2.9.jar",
              ),
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_matchers_core$scalaRulesPathVersionSuffix/scalatest-matchers-core_2.13-3.2.9.jar",
              ),
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_mustmatchers$scalaRulesPathVersionSuffix/scalatest-mustmatchers_2.13-3.2.9.jar",
              ),
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_shouldmatchers$scalaRulesPathVersionSuffix/scalatest-shouldmatchers_2.13-3.2.9.jar",
              ),
            ),
            emptyList(),
            Path("\$WORKSPACE"),
            mapOf(),
            listOf(JvmMainClass("io.bazel.rulesscala.scala_test.Runner", emptyList())),
          ),
          JvmEnvironmentItem(
            Label.parse("$targetPrefix//target_with_resources:java_binary"),
            listOf(),
            emptyList(),
            Path("\$WORKSPACE"),
            mapOf(),
            listOf(JvmMainClass("target_with_resources.JavaBinary", emptyList())),
          ),
          JvmEnvironmentItem(
            Label.parse("$targetPrefix//target_without_args:binary"),
            listOf(
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_library$scalaRulesPathVersionSuffix/scala-library-$scalaRulesVersion.jar",
              ),
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_reflect$scalaRulesPathVersionSuffix/scala-reflect-$scalaRulesVersion.jar",
              ),
            ),
            listOf("-Xms2G -Xmx5G"),
            Path("\$WORKSPACE"),
            mapOf(),
            listOf(JvmMainClass("example.Example", emptyList())),
          ),
          JvmEnvironmentItem(
            Label.parse("$targetPrefix//target_without_jvm_flags:binary"),
            listOf(
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_library$scalaRulesPathVersionSuffix/scala-library-$scalaRulesVersion.jar",
              ),
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_reflect$scalaRulesPathVersionSuffix/scala-reflect-$scalaRulesVersion.jar",
              ),
            ),
            emptyList(),
            Path("\$WORKSPACE"),
            mapOf(),
            listOf(JvmMainClass("example.Example", listOf("arg1", "arg2"))),
          ),
          JvmEnvironmentItem(
            Label.parse("$targetPrefix//target_without_main_class:library"),
            listOf(
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_library$scalaRulesPathVersionSuffix/scala-library-$scalaRulesVersion.jar",
              ),
              Path(
                "\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_reflect$scalaRulesPathVersionSuffix/scala-reflect-$scalaRulesVersion.jar",
              ),
            ),
            emptyList(),
            Path("\$WORKSPACE"),
            mapOf(),
          ),
          JvmEnvironmentItem(
            Label.parse("$targetPrefix//environment_variables:java_binary"),
            listOf(),
            emptyList(),
            Path("\$WORKSPACE"),
            mapOf("foo1" to "val1", "foo2" to "val2"),
            listOf(JvmMainClass("environment_variables.JavaEnv", emptyList())),
          ),
          JvmEnvironmentItem(
            Label.parse("$targetPrefix//environment_variables:java_test"),
            listOf(
              Path("\$BAZEL_OUTPUT_BASE_PATH/external/$remote_java_tools/java_tools/Runner_deploy.jar"),
            ),
            emptyList(),
            Path("\$WORKSPACE"),
            mapOf(
              "foo1" to "val1",
              "foo2" to "val2",
              "foo3" to "val3",
              "foo4" to "val4",
              "target_location" to
                "bazel-out/$bazelArch-fastbuild/bin/environment_variables/java_binary bazel-out/$bazelArch-fastbuild/bin/environment_variables/java_binary.jar",
            ),
          ),
          JvmEnvironmentItem(
            Label.parse("$targetPrefix//target_with_javac_exports:java_library"),
            emptyList(),
            emptyList(),
            Path("\$WORKSPACE"),
            mapOf(),
          ).takeIf { majorBazelVersion > 5 },
        ),
      )
    return BazelBspTestScenarioStep(
      "jvm test environment results",
    ) { testClient.testJvmTestEnvironment(30.seconds, params, expectedResult) }
  }

  private fun javacOptionsResult(): BazelBspTestScenarioStep {
    val stepName = "javac options results"
    if (majorBazelVersion == 5) return BazelBspTestScenarioStep(stepName) {}
    val targetWithJavacExportsIdentifier = Label.parse("$targetPrefix//target_with_javac_exports:java_library")
    val params = JavacOptionsParams(listOf(targetWithJavacExportsIdentifier))

    val expectedResult =
      JavacOptionsResult(
        listOf(
          JavacOptionsItem(
            targetWithJavacExportsIdentifier,
            listOf(
              "-XepDisableAllChecks",
              "--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED",
              "--add-exports=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED",
              "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
            ),
          ),
        ),
      )
    return BazelBspTestScenarioStep(
      stepName,
    ) { testClient.testJavacOptions(30.seconds, params, expectedResult) }
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
      CompileResult(StatusCode.OK)

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
      BuildTarget(
        Label.parse("$targetPrefix//java_targets:java_binary"),
        listOf("application"),
        listOf("java"),
        emptyList(),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
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
      BuildTarget(
        Label.parse("$targetPrefix//java_targets:java_binary_with_flag"),
        listOf("application"),
        listOf("java"),
        emptyList(),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
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
        "org.scala-lang",
        scalaRulesVersion,
        "2.13",
        ScalaPlatform.JVM,
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
      )

    val scalaTargetsScalaBinary =
      BuildTarget(
        Label.parse("$targetPrefix//scala_targets:scala_binary"),
        listOf("application"),
        listOf("scala"),
        listOf(
          Label.synthetic("scala-compiler-$scalaRulesVersion.jar"),
          Label.synthetic("scala-library-$scalaRulesVersion.jar"),
          Label.synthetic("scala-reflect-$scalaRulesVersion.jar"),
        ),
        TargetKind(
          kindString = "scala_binary",
          ruleType = RuleType.BINARY,
        ),
        baseDirectory = Path("\$WORKSPACE/scala_targets/"),
        data = scalaBuildTarget,
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
      BuildTarget(
        Label.parse("$targetPrefix//java_targets/subpackage:java_library"),
        listOf("library"),
        listOf("java"),
        emptyList(),
        TargetKind(
          kindString = "java_library",
          ruleType = RuleType.LIBRARY,
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
      BuildTarget(
        Label.parse("$targetPrefix//java_targets:java_library"),
        listOf("library"),
        listOf("java"),
        listOf(),
        TargetKind(
          kindString = "java_library",
          ruleType = RuleType.LIBRARY,
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
      BuildTarget(
        Label.parse("$targetPrefix//target_without_jvm_flags:binary"),
        listOf("application"),
        listOf("scala"),
        listOf(
          Label.synthetic("scala-compiler-$scalaRulesVersion.jar"),
          Label.synthetic("scala-library-$scalaRulesVersion.jar"),
          Label.synthetic("scala-reflect-$scalaRulesVersion.jar"),
        ),
        TargetKind(
          kindString = "scala_binary",
          ruleType = RuleType.BINARY,
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
      BuildTarget(
        Label.parse("$targetPrefix//target_without_main_class:library"),
        listOf("library"),
        listOf("scala"),
        listOf(
          Label.synthetic("scala-compiler-$scalaRulesVersion.jar"),
          Label.synthetic("scala-library-$scalaRulesVersion.jar"),
          Label.synthetic("scala-reflect-$scalaRulesVersion.jar"),
        ),
        TargetKind(
          kindString = "scala_library",
          ruleType = RuleType.LIBRARY,
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
      BuildTarget(
        Label.parse("$targetPrefix//target_without_args:binary"),
        listOf("application"),
        listOf("scala"),
        listOf(
          Label.synthetic("scala-compiler-$scalaRulesVersion.jar"),
          Label.synthetic("scala-library-$scalaRulesVersion.jar"),
          Label.synthetic("scala-reflect-$scalaRulesVersion.jar"),
        ),
        TargetKind(
          kindString = "scala_binary",
          ruleType = RuleType.BINARY,
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
      BuildTarget(
        Label.parse("$targetPrefix//target_with_dependency:java_binary"),
        listOf("application"),
        listOf("java"),
        listOf(
          Label.parse("$targetPrefix//java_targets:java_library_exported"),
          Label.parse(guavaDep),
        ),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
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
      BuildTarget(
        Label.parse("$targetPrefix//scala_targets:scala_test"),
        listOf("test"),
        listOf("scala"),
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
      BuildTarget(
        Label.parse("$targetPrefix//target_with_resources:java_binary"),
        listOf("application"),
        listOf("java"),
        emptyList(),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
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
      BuildTarget(
        Label.parse("$targetPrefix//java_targets:java_library_exported"),
        listOf("library"),
        listOf("java"),
        listOf(
          Label.parse("$targetPrefix//java_targets/subpackage:java_library"),
          Label.parse("$targetPrefix//java_targets:java_library_exported_output_jars"),
        ),
        TargetKind(
          kindString = "java_library",
          ruleType = RuleType.LIBRARY,
        ),
        baseDirectory = Path("\$WORKSPACE/java_targets/"),
        data = jvmBuildTarget,
        sources =
          listOf(), // TODO: why empty?
        resources = emptyList(),
      )
    val environmentVariablesJavaLibrary =
      BuildTarget(
        Label.parse("$targetPrefix//environment_variables:java_binary"),
        listOf("application"),
        listOf("java"),
        emptyList(),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
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
      BuildTarget(
        Label.parse("$targetPrefix//environment_variables:java_test"),
        listOf("test"),
        listOf("java"),
        emptyList(),
        TargetKind(
          kindString = "java_test",
          ruleType = RuleType.TEST,
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
      BuildTarget(
        Label.parse("$targetPrefix//target_with_javac_exports:java_library"),
        listOf("library"),
        listOf("java"),
        emptyList(),
        TargetKind(
          kindString = "java_library",
          ruleType = RuleType.LIBRARY,
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
        BuildTarget(
          Label.parse("@//genrule:foo"),
          listOf("application"),
          emptyList(),
          emptyList(),
          kind =
            TargetKind(
              ruleType = RuleType.BINARY,
              kindString = "genrule",
            ),
          baseDirectory = Path("$workspaceDir/genrule/"),
          sources = emptyList(),
          resources = emptyList(),
        ),
        BuildTarget(
          Label.parse("@//target_without_java_info:filegroup"),
          listOf("application"),
          listOf("java"),
          emptyList(),
          kind =
            TargetKind(
              ruleType = RuleType.BINARY,
              kindString = "filegroup",
            ),
          baseDirectory = Path("$workspaceDir/target_without_java_info/"),
          sources = emptyList(),
          resources = emptyList(),
        ),
        BuildTarget(
          Label.parse("@//target_without_java_info:genrule"),
          listOf("application"),
          listOf("java", "kotlin"),
          emptyList(),
          kind =
            TargetKind(
              ruleType = RuleType.BINARY,
              kindString = "genrule",
            ),
          baseDirectory = Path("$workspaceDir/target_without_java_info/"),
          sources = emptyList(),
          resources = emptyList(),
        ),
      )
    return WorkspaceBuildTargetsResult(
      listOfNotNull(
        javaTargetsJavaBinary,
        javaTargetsJavaBinaryWithFlag,
        scalaTargetsScalaBinary,
        javaTargetsSubpackageSubpackage,
        javaTargetsJavaLibrary,
        targetWithoutJvmFlagsBinary,
        targetWithoutMainClassLibrary,
        targetWithoutArgsBinary,
        targetWithDependencyJavaBinary,
        scalaTargetsScalaTest,
        targetWithResourcesJavaBinary,
        javaTargetsJavaLibraryExported,
        environmentVariablesJavaLibrary,
        environmentVariablesJavaTest,
        targetWithJavacExportsJavaLibrary.takeIf { majorBazelVersion > 5 },
      ) + nonModuleTargets,
    )
  }
}
