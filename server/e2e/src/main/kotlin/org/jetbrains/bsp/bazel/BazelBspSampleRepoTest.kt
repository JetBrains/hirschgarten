package org.jetbrains.bsp.bazel

import kotlinx.coroutines.future.await
import org.jetbrains.bazel.commons.utils.OsFamily
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bsp.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetCapabilities
import org.jetbrains.bsp.protocol.BuildTargetIdentifier
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
import org.jetbrains.bsp.protocol.NonModuleTargetsResult
import org.jetbrains.bsp.protocol.ResourcesItem
import org.jetbrains.bsp.protocol.ResourcesParams
import org.jetbrains.bsp.protocol.ResourcesResult
import org.jetbrains.bsp.protocol.ScalaBuildTarget
import org.jetbrains.bsp.protocol.ScalaMainClass
import org.jetbrains.bsp.protocol.ScalaMainClassesItem
import org.jetbrains.bsp.protocol.ScalaMainClassesParams
import org.jetbrains.bsp.protocol.ScalaMainClassesResult
import org.jetbrains.bsp.protocol.ScalaPlatform
import org.jetbrains.bsp.protocol.ScalaTestClassesItem
import org.jetbrains.bsp.protocol.ScalaTestClassesParams
import org.jetbrains.bsp.protocol.ScalaTestClassesResult
import org.jetbrains.bsp.protocol.SourceItem
import org.jetbrains.bsp.protocol.SourceItemKind
import org.jetbrains.bsp.protocol.SourcesItem
import org.jetbrains.bsp.protocol.SourcesParams
import org.jetbrains.bsp.protocol.SourcesResult
import org.jetbrains.bsp.protocol.StatusCode
import org.jetbrains.bsp.protocol.TextDocumentIdentifier
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object BazelBspSampleRepoTest : BazelBspTestBaseScenario() {
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
    if (OsFamily.inferFromSystem() == OsFamily.MACOS) {
      "darwin_arm64"
    } else {
      "k8"
    }

  private val guavaClasspath =
    if (isBzlmod) {
      listOf(
        "file://\$BAZEL_OUTPUT_BASE_PATH/execroot/_main/bazel-out/$bazelArch-fastbuild/bin/external/rules_jvm_external${bzlmodRepoNameSeparator}${bzlmodRepoNameSeparator}maven${bzlmodRepoNameSeparator}maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/code/findbugs/jsr305/3.0.2/processed_jsr305-3.0.2.jar",
        "file://\$BAZEL_OUTPUT_BASE_PATH/execroot/_main/bazel-out/$bazelArch-fastbuild/bin/external/rules_jvm_external${bzlmodRepoNameSeparator}${bzlmodRepoNameSeparator}maven${bzlmodRepoNameSeparator}maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/errorprone/error_prone_annotations/2.7.1/processed_error_prone_annotations-2.7.1.jar",
        "file://\$BAZEL_OUTPUT_BASE_PATH/execroot/_main/bazel-out/$bazelArch-fastbuild/bin/external/rules_jvm_external${bzlmodRepoNameSeparator}${bzlmodRepoNameSeparator}maven${bzlmodRepoNameSeparator}maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/guava/failureaccess/1.0.1/processed_failureaccess-1.0.1.jar",
        "file://\$BAZEL_OUTPUT_BASE_PATH/execroot/_main/bazel-out/$bazelArch-fastbuild/bin/external/rules_jvm_external${bzlmodRepoNameSeparator}${bzlmodRepoNameSeparator}maven${bzlmodRepoNameSeparator}maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/guava/guava/31.0.1-jre/processed_guava-31.0.1-jre.jar",
        "file://\$BAZEL_OUTPUT_BASE_PATH/execroot/_main/bazel-out/$bazelArch-fastbuild/bin/external/rules_jvm_external${bzlmodRepoNameSeparator}${bzlmodRepoNameSeparator}maven${bzlmodRepoNameSeparator}maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/guava/listenablefuture/9999.0-empty-to-avoid-conflict-with-guava/processed_listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar",
        "file://\$BAZEL_OUTPUT_BASE_PATH/execroot/_main/bazel-out/$bazelArch-fastbuild/bin/external/rules_jvm_external${bzlmodRepoNameSeparator}${bzlmodRepoNameSeparator}maven${bzlmodRepoNameSeparator}maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/j2objc/j2objc-annotations/1.3/processed_j2objc-annotations-1.3.jar",
        "file://\$BAZEL_OUTPUT_BASE_PATH/execroot/_main/bazel-out/$bazelArch-fastbuild/bin/external/rules_jvm_external${bzlmodRepoNameSeparator}${bzlmodRepoNameSeparator}maven${bzlmodRepoNameSeparator}maven/v1/https/cache-redirector.jetbrains.com/maven-central/org/checkerframework/checker-qual/3.12.0/processed_checker-qual-3.12.0.jar",
      )
    } else {
      listOf("file://\$BAZEL_OUTPUT_BASE_PATH/external/guava/guava-28.0-jre.jar")
    }

  // TODO: https://youtrack.jetbrains.com/issue/BAZEL-95
  @JvmStatic
  fun main(args: Array<String>) = executeScenario()

  override fun scenarioSteps(): List<BazelBspTestScenarioStep> =
    listOf(
      resolveProject(),
      compareWorkspaceTargetsResults(),
      sourcesResults(),
      resourcesResults(),
      inverseSourcesResults(),
      dependencySourcesResults(),
      scalaMainClasses(),
      scalaTestClasses(),
      jvmRunEnvironment(),
      jvmTestEnvironment(),
      javacOptionsResult(),
      nonModuleTargets(),
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

  private fun sourcesResults(): BazelBspTestScenarioStep {
    val targetWithoutJvmFlagsExampleScala =
      SourceItem(
        "file://\$WORKSPACE/target_without_jvm_flags/Example.scala",
        SourceItemKind.FILE,
        false,
      )
    val targetWithoutJvmFlagsSources =
      SourcesItem(
        BuildTargetIdentifier("$targetPrefix//target_without_jvm_flags:binary"),
        listOf(targetWithoutJvmFlagsExampleScala),
        roots = listOf("file://\$WORKSPACE/"),
      )

    val targetWithoutArgsExampleScala =
      SourceItem(
        "file://\$WORKSPACE/target_without_args/Example.scala",
        SourceItemKind.FILE,
        false,
      )
    val targetWithoutArgsSources =
      SourcesItem(
        BuildTargetIdentifier("$targetPrefix//target_without_args:binary"),
        listOf(targetWithoutArgsExampleScala),
        roots = listOf("file://\$WORKSPACE/"),
      )

    val targetWithoutMainClassExampleScala =
      SourceItem(
        "file://\$WORKSPACE/target_without_main_class/Example.scala",
        SourceItemKind.FILE,
        false,
      )
    val targetWithoutMainClassSources =
      SourcesItem(
        BuildTargetIdentifier("$targetPrefix//target_without_main_class:library"),
        listOf(targetWithoutMainClassExampleScala),
        roots = listOf("file://\$WORKSPACE/"),
      )

    val targetWithResourcesJavaBinaryJava =
      SourceItem(
        "file://\$WORKSPACE/target_with_resources/JavaBinary.java",
        SourceItemKind.FILE,
        false,
      )
    val targetWithResourcesSources =
      SourcesItem(
        BuildTargetIdentifier("$targetPrefix//target_with_resources:java_binary"),
        listOf(targetWithResourcesJavaBinaryJava),
        roots = listOf("file://\$WORKSPACE/"),
      )

    val targetWithDependencyJavaBinaryJava =
      SourceItem(
        "file://\$WORKSPACE/target_with_dependency/JavaBinary.java",
        SourceItemKind.FILE,
        false,
      )
    val targetWithDependencySources =
      SourcesItem(
        BuildTargetIdentifier("$targetPrefix//target_with_dependency:java_binary"),
        listOf(targetWithDependencyJavaBinaryJava),
        roots = listOf("file://\$WORKSPACE/"),
      )

    val scalaTargetsScalaBinaryScala =
      SourceItem(
        "file://\$WORKSPACE/scala_targets/ScalaBinary.scala",
        SourceItemKind.FILE,
        false,
      )
    val scalaTargetsScalaBinarySources =
      SourcesItem(
        BuildTargetIdentifier("$targetPrefix//scala_targets:scala_binary"),
        listOf(scalaTargetsScalaBinaryScala),
        roots = listOf("file://\$WORKSPACE/"),
      )

    val scalaTargetsScalaTestScala =
      SourceItem(
        "file://\$WORKSPACE/scala_targets/ScalaTest.scala",
        SourceItemKind.FILE,
        false,
      )
    val scalaTargetsScalaTestSources =
      SourcesItem(
        BuildTargetIdentifier("$targetPrefix//scala_targets:scala_test"),
        listOf(scalaTargetsScalaTestScala),
        roots = listOf("file://\$WORKSPACE/"),
      )

    val javaTargetsJavaBinaryJava =
      SourceItem(
        "file://\$WORKSPACE/java_targets/JavaBinary.java",
        SourceItemKind.FILE,
        false,
      )
    val javaTargetsJavaBinarySources =
      SourcesItem(
        BuildTargetIdentifier("$targetPrefix//java_targets:java_binary"),
        listOf(javaTargetsJavaBinaryJava),
        roots = listOf("file://\$WORKSPACE/"),
      )

    val javaTargetsJavaBinaryWithFlagJava =
      SourceItem(
        "file://\$WORKSPACE/java_targets/JavaBinaryWithFlag.java",
        SourceItemKind.FILE,
        false,
      )
    val javaTargetsJavaBinaryWithFlagSources =
      SourcesItem(
        BuildTargetIdentifier("$targetPrefix//java_targets:java_binary_with_flag"),
        listOf(javaTargetsJavaBinaryWithFlagJava),
        roots = listOf("file://\$WORKSPACE/"),
      )

    val javaTargetsJavaLibraryJava =
      SourceItem(
        "file://\$WORKSPACE/java_targets/JavaLibrary.java",
        SourceItemKind.FILE,
        false,
      )
    val javaTargetsJavaLibrarySources =
      SourcesItem(
        BuildTargetIdentifier("$targetPrefix//java_targets:java_library"),
        listOf(javaTargetsJavaLibraryJava),
        roots = listOf("file://\$WORKSPACE/"),
      )

    val javaTargetsSubpackageJavaLibraryJava =
      SourceItem(
        "file://\$WORKSPACE/java_targets/subpackage/JavaLibrary2.java",
        SourceItemKind.FILE,
        false,
      )
    val javaTargetsSubpackageJavaLibrarySources =
      SourcesItem(
        BuildTargetIdentifier("$targetPrefix//java_targets/subpackage:java_library"),
        listOf(javaTargetsSubpackageJavaLibraryJava),
        roots = listOf("file://\$WORKSPACE/"),
      )

    val javaTargetsJavaLibraryExportedSources =
      SourcesItem(
        BuildTargetIdentifier("$targetPrefix//java_targets:java_library_exported"),
        emptyList(),
        roots = emptyList(),
      )

    val environmentVariablesJavaBinary =
      SourceItem("file://\$WORKSPACE/environment_variables/JavaEnv.java", SourceItemKind.FILE, false)
    val environmentVariablesJavaBinarySources =
      SourcesItem(
        BuildTargetIdentifier("$targetPrefix//environment_variables:java_binary"),
        listOf(environmentVariablesJavaBinary),
        roots = listOf("file://\$WORKSPACE/"),
      )

    val environmentVariablesJavaTest =
      SourceItem("file://\$WORKSPACE/environment_variables/JavaTest.java", SourceItemKind.FILE, false)
    val environmentVariablesJavaTestSources =
      SourcesItem(
        BuildTargetIdentifier("$targetPrefix//environment_variables:java_test"),
        listOf(environmentVariablesJavaTest),
        roots = listOf("file://\$WORKSPACE/"),
      )

    val targetWithJavacExportsJavaLibrary =
      SourceItem("file://\$WORKSPACE/target_with_javac_exports/JavaLibrary.java", SourceItemKind.FILE, false)
    val targetWithJavacExportsJavaLibrarySources =
      SourcesItem(
        BuildTargetIdentifier("$targetPrefix//target_with_javac_exports:java_library"),
        listOf(targetWithJavacExportsJavaLibrary),
        roots = listOf("file://\$WORKSPACE/"),
      )

    val sourcesParams = SourcesParams(expectedTargetIdentifiers())
    val expectedSourcesResult =
      SourcesResult(
        listOfNotNull(
          targetWithoutArgsSources,
          targetWithoutJvmFlagsSources,
          targetWithoutMainClassSources,
          targetWithResourcesSources,
          targetWithDependencySources,
          scalaTargetsScalaBinarySources,
          scalaTargetsScalaTestSources,
          javaTargetsJavaBinarySources,
          javaTargetsJavaBinaryWithFlagSources,
          javaTargetsJavaLibrarySources,
          javaTargetsSubpackageJavaLibrarySources,
          javaTargetsJavaLibraryExportedSources,
          environmentVariablesJavaBinarySources,
          environmentVariablesJavaTestSources,
          targetWithJavacExportsJavaLibrarySources.takeIf { majorBazelVersion > 5 },
        ),
      )
    return BazelBspTestScenarioStep("sources results") {
      testClient.testSources(30.seconds, sourcesParams, expectedSourcesResult)
    }
  }

  private fun resourcesResults(): BazelBspTestScenarioStep {
    val targetWithResources =
      ResourcesItem(
        BuildTargetIdentifier("$targetPrefix//target_with_resources:java_binary"),
        listOf(
          "file://\$WORKSPACE/target_with_resources/file1.txt",
          "file://\$WORKSPACE/target_with_resources/file2.txt",
        ),
      )
    val javaTargetsSubpackageJavaLibrary =
      ResourcesItem(
        BuildTargetIdentifier("$targetPrefix//java_targets/subpackage:java_library"),
        emptyList(),
      )
    val javaTargetsJavaBinary =
      ResourcesItem(BuildTargetIdentifier("$targetPrefix//java_targets:java_binary"), emptyList())
    val javaTargetsJavaBinaryWithFlag =
      ResourcesItem(BuildTargetIdentifier("$targetPrefix//java_targets:java_binary_with_flag"), emptyList())
    val javaTargetsJavaLibrary =
      ResourcesItem(BuildTargetIdentifier("$targetPrefix//java_targets:java_library"), emptyList())
    val javaTargetsJavaLibraryExported =
      ResourcesItem(
        BuildTargetIdentifier("$targetPrefix//java_targets:java_library_exported"),
        emptyList(),
      )
    val scalaTargetsScalaBinary =
      ResourcesItem(BuildTargetIdentifier("$targetPrefix//scala_targets:scala_binary"), emptyList())
    val scalaTargetsScalaTest =
      ResourcesItem(BuildTargetIdentifier("$targetPrefix//scala_targets:scala_test"), emptyList())
    val targetWithDependencyJavaBinary =
      ResourcesItem(
        BuildTargetIdentifier("$targetPrefix//target_with_dependency:java_binary"),
        emptyList(),
      )
    val targetWithoutArgsBinary =
      ResourcesItem(BuildTargetIdentifier("$targetPrefix//target_without_args:binary"), emptyList())
    val targetWithoutJvmFlagsBinary =
      ResourcesItem(
        BuildTargetIdentifier("$targetPrefix//target_without_jvm_flags:binary"),
        emptyList(),
      )
    val targetWithoutMainClassLibrary =
      ResourcesItem(
        BuildTargetIdentifier("$targetPrefix//target_without_main_class:library"),
        emptyList(),
      )
    val environmentVariablesJavaBinary =
      ResourcesItem(BuildTargetIdentifier("$targetPrefix//environment_variables:java_binary"), emptyList())
    val environmentVariablesJavaTest =
      ResourcesItem(BuildTargetIdentifier("$targetPrefix//environment_variables:java_test"), emptyList())
    val targetWithJavacExportsJavaLibrary =
      ResourcesItem(BuildTargetIdentifier("$targetPrefix//target_with_javac_exports:java_library"), emptyList())

    val expectedResourcesResult =
      ResourcesResult(
        listOfNotNull(
          targetWithResources,
          javaTargetsSubpackageJavaLibrary,
          javaTargetsJavaBinary,
          javaTargetsJavaBinaryWithFlag,
          javaTargetsJavaLibrary,
          javaTargetsJavaLibraryExported,
          scalaTargetsScalaBinary,
          scalaTargetsScalaTest,
          targetWithDependencyJavaBinary,
          targetWithoutArgsBinary,
          targetWithoutJvmFlagsBinary,
          targetWithoutMainClassLibrary,
          environmentVariablesJavaBinary,
          environmentVariablesJavaTest,
          targetWithJavacExportsJavaLibrary.takeIf { majorBazelVersion > 5 },
        ),
      )
    val resourcesParams = ResourcesParams(expectedTargetIdentifiers())
    return BazelBspTestScenarioStep(
      "resources results",
    ) {
      testClient.testResources(30.seconds, resourcesParams, expectedResourcesResult)
    }
  }

  private fun inverseSourcesResults(): BazelBspTestScenarioStep {
    val inverseSourcesDocument = TextDocumentIdentifier("file://\$WORKSPACE/java_targets/JavaBinary.java")
    val expectedInverseSourcesResult =
      InverseSourcesResult(listOf(BuildTargetIdentifier("$targetPrefix//java_targets:java_binary")))
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

  // FIXME: Is it even correct? Here when queried for *all* targets we return only the ones that
  //   are actually Scala ones. It kinda makes sense, but it seems to be inconsistent.
  private fun scalaMainClasses(): BazelBspTestScenarioStep {
    val scalaMainClassesParams = ScalaMainClassesParams(expectedTargetIdentifiers())
    val scalaTargetsScalaBinary =
      ScalaMainClassesItem(
        BuildTargetIdentifier("$targetPrefix//scala_targets:scala_binary"),
        listOf(
          ScalaMainClass(
            "example.Example",
            listOf("arg1", "arg2"),
            listOf("-Xms2G -Xmx5G"),
          ),
        ),
      )
    val targetWithoutJvmFlagsBinary =
      ScalaMainClassesItem(
        BuildTargetIdentifier("$targetPrefix//target_without_jvm_flags:binary"),
        listOf(ScalaMainClass("example.Example", listOf("arg1", "arg2"), emptyList())),
      )
    val targetWithoutArgsBinary =
      ScalaMainClassesItem(
        BuildTargetIdentifier("$targetPrefix//target_without_args:binary"),
        listOf(
          ScalaMainClass(
            "example.Example",
            emptyList(),
            listOf("-Xms2G -Xmx5G"),
          ),
        ),
      )

    // FIXME: I'd like to add a test case where target's environment variables field is non-null.
    //  But how can I even populate it?
    //  Apparently it is populated in JVM run environment?
    val expectedScalaMainClassesResult =
      ScalaMainClassesResult(
        listOf(scalaTargetsScalaBinary, targetWithoutJvmFlagsBinary, targetWithoutArgsBinary),
      )
    return BazelBspTestScenarioStep(
      "Scala main classes",
    ) {
      testClient.testScalaMainClasses(
        30.seconds,
        scalaMainClassesParams,
        expectedScalaMainClassesResult,
      )
    }
  }

  // FIXME: Re-add a spec2 test target. But that requires messing with the bazel toolchain
  //  See:
  // https://github.com/bazelbuild/rules_scala/tree/9b85affa2e08a350a4315882b602eda55b262356/examples/testing/multi_frameworks_toolchain
  //  See: https://github.com/JetBrains/bazel-bsp/issues/96
  private fun scalaTestClasses(): BazelBspTestScenarioStep {
    val scalaTestClassesParams = ScalaTestClassesParams(expectedTargetIdentifiers())
    val scalaTargetsScalaTest =
      ScalaTestClassesItem(
        BuildTargetIdentifier("$targetPrefix//scala_targets:scala_test"),
        listOf("io.bazel.rulesscala.scala_test.Runner"),
      )

    val expectedScalaTestClassesResult = ScalaTestClassesResult(listOf(scalaTargetsScalaTest))
    return BazelBspTestScenarioStep(
      "Scala test classes",
    ) {
      testClient.testScalaTestClasses(
        30.seconds,
        scalaTestClassesParams,
        expectedScalaTestClassesResult,
      )
    }
  }

  private fun dependencySourcesResults(): BazelBspTestScenarioStep {
    val javaTargetsJavaBinary =
      DependencySourcesItem(
        BuildTargetIdentifier("$targetPrefix//java_targets:java_binary"),
        emptyList(),
      )
    val javaTargetsJavaBinaryWithFlag =
      DependencySourcesItem(
        BuildTargetIdentifier("$targetPrefix//java_targets:java_binary_with_flag"),
        emptyList(),
      )
    val javaTargetsJavaLibrary =
      DependencySourcesItem(
        BuildTargetIdentifier("$targetPrefix//java_targets:java_library"),
        emptyList(),
      )
    val targetWithDependencyJavaBinary =
      DependencySourcesItem(
        BuildTargetIdentifier("$targetPrefix//target_with_dependency:java_binary"),
        emptyList(),
      )
    val javaTargetsSubpackageJavaLibrary =
      DependencySourcesItem(
        BuildTargetIdentifier("$targetPrefix//java_targets/subpackage:java_library"),
        emptyList(),
      )
    val scalaTargetsScalaBinary =
      DependencySourcesItem(
        BuildTargetIdentifier("$targetPrefix//scala_targets:scala_binary"),
        emptyList(),
      )
    val scalaTargetsScalaTest =
      DependencySourcesItem(
        BuildTargetIdentifier("$targetPrefix//scala_targets:scala_test"),
        emptyList(),
      )
    val targetWithResourcesJavaBinary =
      DependencySourcesItem(
        BuildTargetIdentifier("$targetPrefix//target_with_resources:java_binary"),
        emptyList(),
      )
    val targetWithoutArgsBinary =
      DependencySourcesItem(
        BuildTargetIdentifier("$targetPrefix//target_without_args:binary"),
        emptyList(),
      )
    val targetWithoutJvmFlagsBinary =
      DependencySourcesItem(
        BuildTargetIdentifier("$targetPrefix//target_without_jvm_flags:binary"),
        emptyList(),
      )
    val targetWithoutMainClassLibrary =
      DependencySourcesItem(
        BuildTargetIdentifier("$targetPrefix//target_without_main_class:library"),
        emptyList(),
      )
    val javaTargetsJavaLibraryExported =
      DependencySourcesItem(
        BuildTargetIdentifier("$targetPrefix//java_targets:java_library_exported"),
        emptyList(),
      )
    val environmentVariablesJavaBinary =
      DependencySourcesItem(
        BuildTargetIdentifier("$targetPrefix//environment_variables:java_binary"),
        emptyList(),
      )
    val environmentVariablesJavaTest =
      DependencySourcesItem(
        BuildTargetIdentifier("$targetPrefix//environment_variables:java_test"),
        emptyList(),
      )
    val targetWithJavacExportsJavaLibrary =
      DependencySourcesItem(
        BuildTargetIdentifier("$targetPrefix//target_with_javac_exports:java_library"),
        emptyList(),
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
        ),
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
            BuildTargetIdentifier("$targetPrefix//java_targets:java_binary"),
            listOf(),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
            listOf(JvmMainClass("java_targets.JavaBinary", emptyList())),
          ),
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//java_targets:java_binary_with_flag"),
            listOf(),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
            listOf(JvmMainClass("java_targets.JavaBinaryWithFlag", emptyList())),
          ),
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//java_targets:java_library"),
            listOf(),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
          ),
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//target_with_dependency:java_binary"),
            guavaClasspath,
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
            listOf(JvmMainClass("target_with_dependency.JavaBinary", emptyList())),
          ),
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//java_targets/subpackage:java_library"),
            listOf(),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
          ),
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//java_targets:java_library_exported"),
            listOf(),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
          ),
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//scala_targets:scala_binary"),
            listOf(
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_library$scalaRulesPathVersionSuffix/scala-library-$scalaRulesVersion.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_reflect$scalaRulesPathVersionSuffix/scala-reflect-$scalaRulesVersion.jar",
            ),
            listOf("-Xms2G -Xmx5G"),
            "\$WORKSPACE",
            mapOf(),
            listOf(JvmMainClass("example.Example", listOf("arg1", "arg2"))),
          ),
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//scala_targets:scala_test"),
            listOf(
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_library$scalaRulesPathVersionSuffix/scala-library-$scalaRulesVersion.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_reflect$scalaRulesPathVersionSuffix/scala-reflect-$scalaRulesVersion.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalactic$scalaRulesPathVersionSuffix/scalactic_2.13-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest$scalaRulesPathVersionSuffix/scalatest_2.13-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_compatible$scalaRulesPathVersionSuffix/scalatest-compatible-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_core$scalaRulesPathVersionSuffix/scalatest-core_2.13-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_featurespec$scalaRulesPathVersionSuffix/scalatest-featurespec_2.13-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_flatspec$scalaRulesPathVersionSuffix/scalatest-flatspec_2.13-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_freespec$scalaRulesPathVersionSuffix/scalatest-freespec_2.13-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_funspec$scalaRulesPathVersionSuffix/scalatest-funspec_2.13-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_funsuite$scalaRulesPathVersionSuffix/scalatest-funsuite_2.13-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_matchers_core$scalaRulesPathVersionSuffix/scalatest-matchers-core_2.13-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_mustmatchers$scalaRulesPathVersionSuffix/scalatest-mustmatchers_2.13-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_shouldmatchers$scalaRulesPathVersionSuffix/scalatest-shouldmatchers_2.13-3.2.9.jar",
            ),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
            listOf(JvmMainClass("io.bazel.rulesscala.scala_test.Runner", emptyList())),
          ),
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//target_with_resources:java_binary"),
            listOf(),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
            listOf(JvmMainClass("target_with_resources.JavaBinary", emptyList())),
          ),
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//target_without_args:binary"),
            listOf(
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_library$scalaRulesPathVersionSuffix/scala-library-$scalaRulesVersion.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_reflect$scalaRulesPathVersionSuffix/scala-reflect-$scalaRulesVersion.jar",
            ),
            listOf("-Xms2G -Xmx5G"),
            "\$WORKSPACE",
            mapOf(),
            listOf(JvmMainClass("example.Example", emptyList())),
          ),
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//target_without_jvm_flags:binary"),
            listOf(
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_library$scalaRulesPathVersionSuffix/scala-library-$scalaRulesVersion.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_reflect$scalaRulesPathVersionSuffix/scala-reflect-$scalaRulesVersion.jar",
            ),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
            listOf(JvmMainClass("example.Example", listOf("arg1", "arg2"))),
          ),
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//target_without_main_class:library"),
            listOf(
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_library$scalaRulesPathVersionSuffix/scala-library-$scalaRulesVersion.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_reflect$scalaRulesPathVersionSuffix/scala-reflect-$scalaRulesVersion.jar",
            ),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
          ),
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//environment_variables:java_binary"),
            listOf(),
            emptyList(),
            "\$WORKSPACE",
            mapOf("foo1" to "val1", "foo2" to "val2"),
            listOf(JvmMainClass("environment_variables.JavaEnv", emptyList())),
          ),
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//environment_variables:java_test"),
            listOf(
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/$remote_java_tools/java_tools/Runner_deploy.jar",
            ),
            emptyList(),
            "\$WORKSPACE",
            mapOf("foo1" to "val1", "foo2" to "val2", "foo3" to "val3", "foo4" to "val4"),
          ),
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//target_with_javac_exports:java_library"),
            emptyList(),
            emptyList(),
            "\$WORKSPACE",
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
            BuildTargetIdentifier("$targetPrefix//java_targets:java_binary"),
            listOf(),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
            listOf(JvmMainClass("java_targets.JavaBinary", emptyList())),
          ),
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//java_targets:java_binary_with_flag"),
            listOf(),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
            listOf(JvmMainClass("java_targets.JavaBinaryWithFlag", emptyList())),
          ),
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//java_targets:java_library"),
            listOf(),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
          ),
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//target_with_dependency:java_binary"),
            guavaClasspath,
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
            listOf(JvmMainClass("target_with_dependency.JavaBinary", emptyList())),
          ),
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//java_targets/subpackage:java_library"),
            listOf(),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
          ),
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//java_targets:java_library_exported"),
            listOf(),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
          ),
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//scala_targets:scala_binary"),
            listOf(
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_library$scalaRulesPathVersionSuffix/scala-library-$scalaRulesVersion.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_reflect$scalaRulesPathVersionSuffix/scala-reflect-$scalaRulesVersion.jar",
            ),
            listOf("-Xms2G -Xmx5G"),
            "\$WORKSPACE",
            mapOf(),
            listOf(JvmMainClass("example.Example", listOf("arg1", "arg2"))),
          ),
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//scala_targets:scala_test"),
            listOf(
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_library$scalaRulesPathVersionSuffix/scala-library-$scalaRulesVersion.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_reflect$scalaRulesPathVersionSuffix/scala-reflect-$scalaRulesVersion.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalactic$scalaRulesPathVersionSuffix/scalactic_2.13-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest$scalaRulesPathVersionSuffix/scalatest_2.13-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_compatible$scalaRulesPathVersionSuffix/scalatest-compatible-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_core$scalaRulesPathVersionSuffix/scalatest-core_2.13-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_featurespec$scalaRulesPathVersionSuffix/scalatest-featurespec_2.13-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_flatspec$scalaRulesPathVersionSuffix/scalatest-flatspec_2.13-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_freespec$scalaRulesPathVersionSuffix/scalatest-freespec_2.13-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_funspec$scalaRulesPathVersionSuffix/scalatest-funspec_2.13-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_funsuite$scalaRulesPathVersionSuffix/scalatest-funsuite_2.13-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_matchers_core$scalaRulesPathVersionSuffix/scalatest-matchers-core_2.13-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_mustmatchers$scalaRulesPathVersionSuffix/scalatest-mustmatchers_2.13-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scalatest_shouldmatchers$scalaRulesPathVersionSuffix/scalatest-shouldmatchers_2.13-3.2.9.jar",
            ),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
            listOf(JvmMainClass("io.bazel.rulesscala.scala_test.Runner", emptyList())),
          ),
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//target_with_resources:java_binary"),
            listOf(),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
            listOf(JvmMainClass("target_with_resources.JavaBinary", emptyList())),
          ),
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//target_without_args:binary"),
            listOf(
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_library$scalaRulesPathVersionSuffix/scala-library-$scalaRulesVersion.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_reflect$scalaRulesPathVersionSuffix/scala-reflect-$scalaRulesVersion.jar",
            ),
            listOf("-Xms2G -Xmx5G"),
            "\$WORKSPACE",
            mapOf(),
            listOf(JvmMainClass("example.Example", emptyList())),
          ),
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//target_without_jvm_flags:binary"),
            listOf(
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_library$scalaRulesPathVersionSuffix/scala-library-$scalaRulesVersion.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_reflect$scalaRulesPathVersionSuffix/scala-reflect-$scalaRulesVersion.jar",
            ),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
            listOf(JvmMainClass("example.Example", listOf("arg1", "arg2"))),
          ),
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//target_without_main_class:library"),
            listOf(
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_library$scalaRulesPathVersionSuffix/scala-library-$scalaRulesVersion.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_reflect$scalaRulesPathVersionSuffix/scala-reflect-$scalaRulesVersion.jar",
            ),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
          ),
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//environment_variables:java_binary"),
            listOf(),
            emptyList(),
            "\$WORKSPACE",
            mapOf("foo1" to "val1", "foo2" to "val2"),
            listOf(JvmMainClass("environment_variables.JavaEnv", emptyList())),
          ),
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//environment_variables:java_test"),
            listOf(
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/$remote_java_tools/java_tools/Runner_deploy.jar",
            ),
            emptyList(),
            "\$WORKSPACE",
            mapOf("foo1" to "val1", "foo2" to "val2", "foo3" to "val3", "foo4" to "val4"),
          ),
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//target_with_javac_exports:java_library"),
            emptyList(),
            emptyList(),
            "\$WORKSPACE",
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
    val targetWithJavacExportsIdentifier = BuildTargetIdentifier("$targetPrefix//target_with_javac_exports:java_library")
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
            emptyList(),
            if (isBzlmod) {
              "file://\$BAZEL_OUTPUT_BASE_PATH/execroot/_main/bazel-out/$bazelArch-fastbuild/bin/target_with_javac_exports/libjava_library.jar"
            } else {
              "file://\$BAZEL_OUTPUT_BASE_PATH/execroot/__main__/bazel-out/$bazelArch-fastbuild/bin/target_with_javac_exports/libjava_library.jar"
            },
          ),
        ),
      )
    return BazelBspTestScenarioStep(
      stepName,
    ) { testClient.testJavacOptions(30.seconds, params, expectedResult) }
  }

  private fun nonModuleTargets(): BazelBspTestScenarioStep {
    val expectedTargets =
      NonModuleTargetsResult(
        listOf(
          BuildTarget(
            BuildTargetIdentifier("@//genrule:foo"),
            listOf("application"),
            emptyList(),
            emptyList(),
            BuildTargetCapabilities(
              canCompile = true,
              canTest = false,
              canRun = true,
              canDebug = false,
            ),
            displayName = "@//genrule:foo",
            baseDirectory = "file://$workspaceDir/genrule/",
          ),
          BuildTarget(
            BuildTargetIdentifier("@//target_with_resources:resources"),
            listOf("library"),
            emptyList(),
            emptyList(),
            BuildTargetCapabilities(
              canCompile = true,
              canTest = false,
              canRun = false,
              canDebug = false,
            ),
            displayName = "@//target_with_resources:resources",
            baseDirectory = "file://$workspaceDir/target_with_resources/",
          ),
          BuildTarget(
            BuildTargetIdentifier("@//target_without_java_info:filegroup"),
            listOf("application"),
            listOf("java"),
            emptyList(),
            BuildTargetCapabilities(
              canCompile = true,
              canTest = false,
              canRun = true,
              canDebug = false,
            ),
            displayName = "@//target_without_java_info:filegroup",
            baseDirectory = "file://$workspaceDir/target_without_java_info/",
          ),
          BuildTarget(
            BuildTargetIdentifier("@//target_without_java_info:genrule"),
            listOf("application"),
            listOf("java", "kotlin"),
            emptyList(),
            BuildTargetCapabilities(
              canCompile = true,
              canTest = false,
              canRun = true,
              canDebug = false,
            ),
            displayName = "@//target_without_java_info:genrule",
            baseDirectory = "file://$workspaceDir/target_without_java_info/",
          ),
        ),
      )

    val client = createBazelClient()

    return BazelBspTestScenarioStep(
      "non module targets",
    ) {
      client.test(30.seconds) { session ->
        val targets = session.server.workspaceNonModuleTargets().await()
        client.assertJsonEquals(expectedTargets, targets)
      }
    }
  }

  private fun buildTargetWithOriginId(): BazelBspTestScenarioStep {
    val targetId = BuildTargetIdentifier("$targetPrefix//java_targets:java_library")
    val originId = "build-target-origin-id"
    return buildTarget(targetId, originId)
  }

  private fun buildTarget(targetId: BuildTargetIdentifier, originId: String): BazelBspTestScenarioStep {
    val params =
      CompileParams(listOf(targetId), originId = originId)

    val expectedResult =
      CompileResult(StatusCode.OK, originId = originId)

    return BazelBspTestScenarioStep("build ${targetId.uri} with origin id: $originId") {
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
    val javaHomeBazel5And6 = "file://\$BAZEL_OUTPUT_BASE_PATH/external/remotejdk11_\$OS$architecturePart/"
    val javaHomeBazel7 =
      "file://\$BAZEL_OUTPUT_BASE_PATH/external/rules_java${bzlmodRepoNameSeparator}$bzlmodRepoNameSeparator" +
        "toolchains${bzlmodRepoNameSeparator}remotejdk11_\$OS$architecturePart/"
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
        BuildTargetIdentifier("$targetPrefix//java_targets:java_binary"),
        listOf("application"),
        listOf("java"),
        emptyList(),
        BuildTargetCapabilities(
          canCompile = true,
          canTest = false,
          canRun = true,
          canDebug = false,
        ),
        displayName = "$targetPrefix//java_targets:java_binary",
        baseDirectory = "file://\$WORKSPACE/java_targets/",
        data = jvmBuildTarget,
      )
    val javaTargetsJavaBinaryWithFlag =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//java_targets:java_binary_with_flag"),
        listOf("application"),
        listOf("java"),
        emptyList(),
        BuildTargetCapabilities(
          canCompile = true,
          canTest = false,
          canRun = true,
          canDebug = false,
        ),
        displayName = "$targetPrefix//java_targets:java_binary_with_flag",
        baseDirectory = "file://\$WORKSPACE/java_targets/",
        data = jvmBuildTargetWithFlag,
      )
    val scalaBuildTarget =
      ScalaBuildTarget(
        "org.scala-lang",
        scalaRulesVersion,
        "2.13",
        ScalaPlatform.JVM,
        listOf(
          "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_compiler$scalaRulesPathVersionSuffix/scala-compiler-$scalaRulesVersion.jar",
          "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_library$scalaRulesPathVersionSuffix/scala-library-$scalaRulesVersion.jar",
          "file://\$BAZEL_OUTPUT_BASE_PATH/external/${scalaRulesPath}_scala_reflect$scalaRulesPathVersionSuffix/scala-reflect-$scalaRulesVersion.jar",
        ),
        jvmBuildTarget = jvmBuildTarget,
      )

    val scalaTargetsScalaBinary =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//scala_targets:scala_binary"),
        listOf("application"),
        listOf("scala"),
        listOf(
          BuildTargetIdentifier(Label.synthetic("scala-compiler-$scalaRulesVersion.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scala-library-$scalaRulesVersion.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scala-reflect-$scalaRulesVersion.jar").toString()),
        ),
        BuildTargetCapabilities(
          canCompile = true,
          canTest = false,
          canRun = true,
          canDebug = false,
        ),
        displayName = "$targetPrefix//scala_targets:scala_binary",
        baseDirectory = "file://\$WORKSPACE/scala_targets/",
        data = scalaBuildTarget,
      )
    val javaTargetsSubpackageSubpackage =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//java_targets/subpackage:java_library"),
        listOf("library"),
        listOf("java"),
        emptyList(),
        BuildTargetCapabilities(
          canCompile = true,
          canTest = false,
          canRun = false,
          canDebug = false,
        ),
        displayName = "$targetPrefix//java_targets/subpackage:java_library",
        baseDirectory = "file://\$WORKSPACE/java_targets/subpackage/",
        data = jvmBuildTarget,
      )
    val javaTargetsJavaLibrary =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//java_targets:java_library"),
        listOf("library"),
        listOf("java"),
        listOf(),
        BuildTargetCapabilities(
          canCompile = true,
          canTest = false,
          canRun = false,
          canDebug = false,
        ),
        displayName = "$targetPrefix//java_targets:java_library",
        baseDirectory = "file://\$WORKSPACE/java_targets/",
        data = jvmBuildTarget,
      )
    val targetWithoutJvmFlagsBinary =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//target_without_jvm_flags:binary"),
        listOf("application"),
        listOf("scala"),
        listOf(
          BuildTargetIdentifier(Label.synthetic("scala-compiler-$scalaRulesVersion.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scala-library-$scalaRulesVersion.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scala-reflect-$scalaRulesVersion.jar").toString()),
        ),
        BuildTargetCapabilities(
          canCompile = true,
          canTest = false,
          canRun = true,
          canDebug = false,
        ),
        displayName = "$targetPrefix//target_without_jvm_flags:binary",
        baseDirectory = "file://\$WORKSPACE/target_without_jvm_flags/",
        data = scalaBuildTarget,
      )
    val targetWithoutMainClassLibrary =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//target_without_main_class:library"),
        listOf("library"),
        listOf("scala"),
        listOf(
          BuildTargetIdentifier(Label.synthetic("scala-compiler-$scalaRulesVersion.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scala-library-$scalaRulesVersion.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scala-reflect-$scalaRulesVersion.jar").toString()),
        ),
        BuildTargetCapabilities(
          canCompile = true,
          canTest = false,
          canRun = false,
          canDebug = false,
        ),
        displayName = "$targetPrefix//target_without_main_class:library",
        baseDirectory = "file://\$WORKSPACE/target_without_main_class/",
        data = scalaBuildTarget,
      )
    val targetWithoutArgsBinary =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//target_without_args:binary"),
        listOf("application"),
        listOf("scala"),
        listOf(
          BuildTargetIdentifier(Label.synthetic("scala-compiler-$scalaRulesVersion.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scala-library-$scalaRulesVersion.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scala-reflect-$scalaRulesVersion.jar").toString()),
        ),
        BuildTargetCapabilities(
          canCompile = true,
          canTest = false,
          canRun = true,
          canDebug = false,
        ),
        displayName = "$targetPrefix//target_without_args:binary",
        baseDirectory = "file://\$WORKSPACE/target_without_args/",
        data = scalaBuildTarget,
      )
    val guavaDepBazel5And6 = "@guava//:guava"
    val guavaDepBazel7 =
      "@@rules_jvm_external${bzlmodRepoNameSeparator}$bzlmodRepoNameSeparator" +
        "maven${bzlmodRepoNameSeparator}maven//:com_google_guava_guava"
    val guavaDep = if (isBzlmod) guavaDepBazel7 else guavaDepBazel5And6
    val targetWithDependencyJavaBinary =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//target_with_dependency:java_binary"),
        listOf("application"),
        listOf("java"),
        listOf(
          BuildTargetIdentifier("$targetPrefix//java_targets:java_library_exported"),
          BuildTargetIdentifier(guavaDep),
        ),
        BuildTargetCapabilities(
          canCompile = true,
          canTest = false,
          canRun = true,
          canDebug = false,
        ),
        displayName = "$targetPrefix//target_with_dependency:java_binary",
        baseDirectory = "file://\$WORKSPACE/target_with_dependency/",
        data = jvmBuildTarget,
      )
    val scalaTargetsScalaTest =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//scala_targets:scala_test"),
        listOf("test"),
        listOf("scala"),
        listOf(
          BuildTargetIdentifier(Label.synthetic("scala-compiler-$scalaRulesVersion.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scala-library-$scalaRulesVersion.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scala-reflect-$scalaRulesVersion.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("librunner.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scalactic_2.13-3.2.9.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scalatest-compatible-3.2.9.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scalatest-core_2.13-3.2.9.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scalatest-featurespec_2.13-3.2.9.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scalatest-flatspec_2.13-3.2.9.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scalatest-freespec_2.13-3.2.9.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scalatest-funspec_2.13-3.2.9.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scalatest-funsuite_2.13-3.2.9.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scalatest-matchers-core_2.13-3.2.9.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scalatest-mustmatchers_2.13-3.2.9.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scalatest-shouldmatchers_2.13-3.2.9.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scalatest_2.13-3.2.9.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("test_reporter.jar").toString()),
        ),
        BuildTargetCapabilities(
          canCompile = true,
          canTest = true,
          canRun = false,
          canDebug = false,
        ),
        displayName = "$targetPrefix//scala_targets:scala_test",
        baseDirectory = "file://\$WORKSPACE/scala_targets/",
        data = scalaBuildTarget,
      )
    val targetWithResourcesJavaBinary =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//target_with_resources:java_binary"),
        listOf("application"),
        listOf("java"),
        emptyList(),
        BuildTargetCapabilities(
          canCompile = true,
          canTest = false,
          canRun = true,
          canDebug = false,
        ),
        displayName = "$targetPrefix//target_with_resources:java_binary",
        baseDirectory = "file://\$WORKSPACE/target_with_resources/",
        data = jvmBuildTarget,
      )
    val javaTargetsJavaLibraryExported =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//java_targets:java_library_exported"),
        listOf("library"),
        listOf("java"),
        listOf(
          BuildTargetIdentifier("$targetPrefix//java_targets/subpackage:java_library"),
          BuildTargetIdentifier("$targetPrefix//java_targets:java_library_exported_output_jars"),
        ),
        BuildTargetCapabilities(
          canCompile = true,
          canTest = false,
          canRun = false,
          canDebug = false,
        ),
        displayName = "$targetPrefix//java_targets:java_library_exported",
        baseDirectory = "file://\$WORKSPACE/java_targets/",
        data = jvmBuildTarget,
      )
    val environmentVariablesJavaLibrary =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//environment_variables:java_binary"),
        listOf("application"),
        listOf("java"),
        emptyList(),
        BuildTargetCapabilities(
          canCompile = true,
          canTest = false,
          canRun = true,
          canDebug = false,
        ),
        displayName = "$targetPrefix//environment_variables:java_binary",
        baseDirectory = "file://\$WORKSPACE/environment_variables/",
        data = jvmBuildTarget,
      )
    val environmentVariablesJavaTest =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//environment_variables:java_test"),
        listOf("test"),
        listOf("java"),
        emptyList(),
        BuildTargetCapabilities(
          canCompile = true,
          canTest = true,
          canRun = false,
          canDebug = false,
        ),
        displayName = "$targetPrefix//environment_variables:java_test",
        baseDirectory = "file://\$WORKSPACE/environment_variables/",
        data = jvmBuildTarget,
      )
    val targetWithJavacExportsJavaLibrary =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//target_with_javac_exports:java_library"),
        listOf("library"),
        listOf("java"),
        emptyList(),
        BuildTargetCapabilities(
          canCompile = true,
          canTest = false,
          canRun = false,
          canDebug = false,
        ),
        displayName = "$targetPrefix//target_with_javac_exports:java_library",
        baseDirectory = "file://\$WORKSPACE/target_with_javac_exports/",
        data = jvmBuildTarget,
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
      ),
    )
  }
}
