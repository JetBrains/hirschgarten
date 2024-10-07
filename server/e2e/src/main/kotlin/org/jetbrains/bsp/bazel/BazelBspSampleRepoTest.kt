package org.jetbrains.bsp.bazel

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.CompileParams
import ch.epfl.scala.bsp4j.CompileResult
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.DependencySourcesParams
import ch.epfl.scala.bsp4j.DependencySourcesResult
import ch.epfl.scala.bsp4j.InverseSourcesParams
import ch.epfl.scala.bsp4j.InverseSourcesResult
import ch.epfl.scala.bsp4j.JavacOptionsItem
import ch.epfl.scala.bsp4j.JavacOptionsParams
import ch.epfl.scala.bsp4j.JavacOptionsResult
import ch.epfl.scala.bsp4j.JvmBuildTarget
import ch.epfl.scala.bsp4j.JvmEnvironmentItem
import ch.epfl.scala.bsp4j.JvmMainClass
import ch.epfl.scala.bsp4j.JvmRunEnvironmentParams
import ch.epfl.scala.bsp4j.JvmRunEnvironmentResult
import ch.epfl.scala.bsp4j.JvmTestEnvironmentParams
import ch.epfl.scala.bsp4j.JvmTestEnvironmentResult
import ch.epfl.scala.bsp4j.ResourcesItem
import ch.epfl.scala.bsp4j.ResourcesParams
import ch.epfl.scala.bsp4j.ResourcesResult
import ch.epfl.scala.bsp4j.ScalaBuildTarget
import ch.epfl.scala.bsp4j.ScalaMainClass
import ch.epfl.scala.bsp4j.ScalaMainClassesItem
import ch.epfl.scala.bsp4j.ScalaMainClassesParams
import ch.epfl.scala.bsp4j.ScalaMainClassesResult
import ch.epfl.scala.bsp4j.ScalaPlatform
import ch.epfl.scala.bsp4j.ScalaTestClassesItem
import ch.epfl.scala.bsp4j.ScalaTestClassesParams
import ch.epfl.scala.bsp4j.ScalaTestClassesResult
import ch.epfl.scala.bsp4j.SourceItem
import ch.epfl.scala.bsp4j.SourceItemKind
import ch.epfl.scala.bsp4j.SourcesItem
import ch.epfl.scala.bsp4j.SourcesParams
import ch.epfl.scala.bsp4j.SourcesResult
import ch.epfl.scala.bsp4j.StatusCode
import ch.epfl.scala.bsp4j.TextDocumentIdentifier
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import kotlinx.coroutines.future.await
import org.jetbrains.bsp.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bsp.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bsp.protocol.NonModuleTargetsResult
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object BazelBspSampleRepoTest : BazelBspTestBaseScenario() {
  private val testClient = createTestkitClient(jvmClasspathReceiver = true)

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
      buildTargetWithoutOriginId(),
    )

  private fun resolveProject(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep(
      "resolve project",
    ) { testClient.testResolveProject(2.minutes) }

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
      )
    targetWithoutJvmFlagsSources.roots = listOf("file://\$WORKSPACE/")

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
      )
    targetWithoutArgsSources.roots = listOf("file://\$WORKSPACE/")

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
      )
    targetWithoutMainClassSources.roots = listOf("file://\$WORKSPACE/")

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
      )
    targetWithResourcesSources.roots = listOf("file://\$WORKSPACE/")

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
      )
    targetWithDependencySources.roots = listOf("file://\$WORKSPACE/")

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
      )
    scalaTargetsScalaBinarySources.roots = listOf("file://\$WORKSPACE/")

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
      )
    scalaTargetsScalaTestSources.roots = listOf("file://\$WORKSPACE/")

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
      )
    javaTargetsJavaBinarySources.roots = listOf("file://\$WORKSPACE/")

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
      )
    javaTargetsJavaBinaryWithFlagSources.roots = listOf("file://\$WORKSPACE/")

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
      )
    javaTargetsJavaLibrarySources.roots = listOf("file://\$WORKSPACE/")

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
      )
    javaTargetsSubpackageJavaLibrarySources.roots = listOf("file://\$WORKSPACE/")

    val javaTargetsJavaLibraryExportedSources =
      SourcesItem(
        BuildTargetIdentifier("$targetPrefix//java_targets:java_library_exported"),
        emptyList(),
      )
    javaTargetsJavaLibraryExportedSources.roots = emptyList()

    val environmentVariablesJavaBinary =
      SourceItem("file://\$WORKSPACE/environment_variables/JavaEnv.java", SourceItemKind.FILE, false)
    val environmentVariablesJavaBinarySources =
      SourcesItem(
        BuildTargetIdentifier("$targetPrefix//environment_variables:java_binary"),
        listOf(environmentVariablesJavaBinary),
      )
    environmentVariablesJavaBinarySources.roots = listOf("file://\$WORKSPACE/")

    val environmentVariablesJavaTest =
      SourceItem("file://\$WORKSPACE/environment_variables/JavaTest.java", SourceItemKind.FILE, false)
    val environmentVariablesJavaTestSources =
      SourcesItem(
        BuildTargetIdentifier("$targetPrefix//environment_variables:java_test"),
        listOf(environmentVariablesJavaTest),
      )
    environmentVariablesJavaTestSources.roots = listOf("file://\$WORKSPACE/")

    val targetWithJavacExportsJavaLibrary =
      SourceItem("file://\$WORKSPACE/target_with_javac_exports/JavaLibrary.java", SourceItemKind.FILE, false)
    val targetWithJavacExportsJavaLibrarySources =
      SourcesItem(
        BuildTargetIdentifier("$targetPrefix//target_with_javac_exports:java_library"),
        listOf(targetWithJavacExportsJavaLibrary),
      )
    targetWithJavacExportsJavaLibrarySources.roots = listOf("file://\$WORKSPACE/")

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
          ).apply {
            mainClasses = listOf(JvmMainClass("java_targets.JavaBinary", emptyList()))
          },
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//java_targets:java_binary_with_flag"),
            listOf(),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
          ).apply {
            mainClasses = listOf(JvmMainClass("java_targets.JavaBinaryWithFlag", emptyList()))
          },
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//java_targets:java_library"),
            listOf(),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
          ).apply {
            mainClasses = emptyList()
          },
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//target_with_dependency:java_binary"),
            listOf(
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/guava/guava-28.0-jre.jar",
            ),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
          ).apply {
            mainClasses = listOf(JvmMainClass("target_with_dependency.JavaBinary", emptyList()))
          },
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//java_targets/subpackage:java_library"),
            listOf(),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
          ).apply {
            mainClasses = emptyList()
          },
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//java_targets:java_library_exported"),
            listOf(),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
          ).apply {
            mainClasses = emptyList()
          },
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//scala_targets:scala_binary"),
            listOf(
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scala_library/scala-library-2.12.14.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scala_reflect/scala-reflect-2.12.14.jar",
            ),
            listOf("-Xms2G -Xmx5G"),
            "\$WORKSPACE",
            mapOf(),
          ).apply {
            mainClasses = listOf(JvmMainClass("example.Example", listOf("arg1", "arg2")))
          },
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//scala_targets:scala_test"),
            listOf(
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scala_library/scala-library-2.12.14.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scala_reflect/scala-reflect-2.12.14.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scalactic/scalactic_2.12-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scalatest/scalatest_2.12-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scalatest_compatible/scalatest-compatible-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scalatest_core/scalatest-core_2.12-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scalatest_featurespec/scalatest-featurespec_2.12-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scalatest_flatspec/scalatest-flatspec_2.12-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scalatest_freespec/scalatest-freespec_2.12-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scalatest_funspec/scalatest-funspec_2.12-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scalatest_funsuite/scalatest-funsuite_2.12-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scalatest_matchers_core/scalatest-matchers-core_2.12-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scalatest_mustmatchers/scalatest-mustmatchers_2.12-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scalatest_shouldmatchers/scalatest-shouldmatchers_2.12-3.2.9.jar",
            ),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
          ).apply {
            mainClasses = listOf(JvmMainClass("io.bazel.rulesscala.scala_test.Runner", emptyList()))
          },
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//target_with_resources:java_binary"),
            listOf(),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
          ).apply {
            mainClasses = listOf(JvmMainClass("target_with_resources.JavaBinary", emptyList()))
          },
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//target_without_args:binary"),
            listOf(
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scala_library/scala-library-2.12.14.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scala_reflect/scala-reflect-2.12.14.jar",
            ),
            listOf("-Xms2G -Xmx5G"),
            "\$WORKSPACE",
            mapOf(),
          ).apply {
            mainClasses = listOf(JvmMainClass("example.Example", emptyList()))
          },
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//target_without_jvm_flags:binary"),
            listOf(
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scala_library/scala-library-2.12.14.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scala_reflect/scala-reflect-2.12.14.jar",
            ),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
          ).apply {
            mainClasses = listOf(JvmMainClass("example.Example", listOf("arg1", "arg2")))
          },
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//target_without_main_class:library"),
            listOf(
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scala_library/scala-library-2.12.14.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scala_reflect/scala-reflect-2.12.14.jar",
            ),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
          ).apply {
            mainClasses = emptyList()
          },
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//environment_variables:java_binary"),
            listOf(),
            emptyList(),
            "\$WORKSPACE",
            mapOf("foo1" to "val1", "foo2" to "val2"),
          ).apply {
            mainClasses = listOf(JvmMainClass("environment_variables.JavaEnv", emptyList()))
          },
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//environment_variables:java_test"),
            listOf(
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/remote_java_tools/java_tools/Runner_deploy.jar",
            ),
            emptyList(),
            "\$WORKSPACE",
            mapOf("foo1" to "val1", "foo2" to "val2", "foo3" to "val3", "foo4" to "val4"),
          ).apply {
            mainClasses = emptyList()
          },
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//target_with_javac_exports:java_library"),
            emptyList(),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
          ).apply {
            mainClasses = emptyList()
          }.takeIf { majorBazelVersion > 5 },
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
          ).apply {
            mainClasses = listOf(JvmMainClass("java_targets.JavaBinary", emptyList()))
          },
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//java_targets:java_binary_with_flag"),
            listOf(),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
          ).apply {
            mainClasses = listOf(JvmMainClass("java_targets.JavaBinaryWithFlag", emptyList()))
          },
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//java_targets:java_library"),
            listOf(),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
          ).apply {
            mainClasses = emptyList()
          },
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//target_with_dependency:java_binary"),
            listOf(
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/guava/guava-28.0-jre.jar",
            ),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
          ).apply {
            mainClasses = listOf(JvmMainClass("target_with_dependency.JavaBinary", emptyList()))
          },
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//java_targets/subpackage:java_library"),
            listOf(),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
          ).apply {
            mainClasses = emptyList()
          },
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//java_targets:java_library_exported"),
            listOf(),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
          ).apply {
            mainClasses = emptyList()
          },
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//scala_targets:scala_binary"),
            listOf(
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scala_library/scala-library-2.12.14.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scala_reflect/scala-reflect-2.12.14.jar",
            ),
            listOf("-Xms2G -Xmx5G"),
            "\$WORKSPACE",
            mapOf(),
          ).apply {
            mainClasses = listOf(JvmMainClass("example.Example", listOf("arg1", "arg2")))
          },
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//scala_targets:scala_test"),
            listOf(
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scala_library/scala-library-2.12.14.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scala_reflect/scala-reflect-2.12.14.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scalactic/scalactic_2.12-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scalatest/scalatest_2.12-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scalatest_compatible/scalatest-compatible-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scalatest_core/scalatest-core_2.12-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scalatest_featurespec/scalatest-featurespec_2.12-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scalatest_flatspec/scalatest-flatspec_2.12-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scalatest_freespec/scalatest-freespec_2.12-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scalatest_funspec/scalatest-funspec_2.12-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scalatest_funsuite/scalatest-funsuite_2.12-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scalatest_matchers_core/scalatest-matchers-core_2.12-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scalatest_mustmatchers/scalatest-mustmatchers_2.12-3.2.9.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scalatest_shouldmatchers/scalatest-shouldmatchers_2.12-3.2.9.jar",
            ),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
          ).apply {
            mainClasses = listOf(JvmMainClass("io.bazel.rulesscala.scala_test.Runner", emptyList()))
          },
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//target_with_resources:java_binary"),
            listOf(),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
          ).apply {
            mainClasses = listOf(JvmMainClass("target_with_resources.JavaBinary", emptyList()))
          },
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//target_without_args:binary"),
            listOf(
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scala_library/scala-library-2.12.14.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scala_reflect/scala-reflect-2.12.14.jar",
            ),
            listOf("-Xms2G -Xmx5G"),
            "\$WORKSPACE",
            mapOf(),
          ).apply {
            mainClasses = listOf(JvmMainClass("example.Example", emptyList()))
          },
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//target_without_jvm_flags:binary"),
            listOf(
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scala_library/scala-library-2.12.14.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scala_reflect/scala-reflect-2.12.14.jar",
            ),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
          ).apply {
            mainClasses = listOf(JvmMainClass("example.Example", listOf("arg1", "arg2")))
          },
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//target_without_main_class:library"),
            listOf(
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scala_library/scala-library-2.12.14.jar",
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scala_reflect/scala-reflect-2.12.14.jar",
            ),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
          ).apply {
            mainClasses = emptyList()
          },
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//environment_variables:java_binary"),
            listOf(),
            emptyList(),
            "\$WORKSPACE",
            mapOf("foo1" to "val1", "foo2" to "val2"),
          ).apply {
            mainClasses = listOf(JvmMainClass("environment_variables.JavaEnv", emptyList()))
          },
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//environment_variables:java_test"),
            listOf(
              "file://\$BAZEL_OUTPUT_BASE_PATH/external/remote_java_tools/java_tools/Runner_deploy.jar",
            ),
            emptyList(),
            "\$WORKSPACE",
            mapOf("foo1" to "val1", "foo2" to "val2", "foo3" to "val3", "foo4" to "val4"),
          ).apply {
            mainClasses = emptyList()
          },
          JvmEnvironmentItem(
            BuildTargetIdentifier("$targetPrefix//target_with_javac_exports:java_library"),
            emptyList(),
            emptyList(),
            "\$WORKSPACE",
            mapOf(),
          ).apply {
            mainClasses = emptyList()
          }.takeIf { majorBazelVersion > 5 },
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
    val bazelArch =
      if (System.getProperty("os.name").lowercase().startsWith("mac")) {
        "darwin_arm64"
      } else {
        "k8"
      }

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
            "file://\$BAZEL_OUTPUT_BASE_PATH/execroot/__main__/bazel-out/$bazelArch-fastbuild/bin/target_with_javac_exports/libjava_library.jar",
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
            BuildTargetCapabilities().also {
              it.canCompile = true
              it.canTest = false
              it.canRun = true
              it.canDebug = false
            },
          ).also {
            it.displayName = "@//genrule:foo"
            it.baseDirectory = "file://$workspaceDir/genrule/"
          },
          BuildTarget(
            BuildTargetIdentifier("@//target_with_resources:resources"),
            emptyList(),
            emptyList(),
            emptyList(),
            BuildTargetCapabilities().also {
              it.canCompile = true
              it.canTest = false
              it.canRun = false
              it.canDebug = false
            },
          ).also {
            it.displayName = "@//target_with_resources:resources"
            it.baseDirectory = "file://$workspaceDir/target_with_resources/"
          },
          BuildTarget(
            BuildTargetIdentifier("@//target_without_java_info:filegroup"),
            listOf("application"),
            listOf("java"),
            emptyList(),
            BuildTargetCapabilities().also {
              it.canCompile = true
              it.canTest = false
              it.canRun = true
              it.canDebug = false
            },
          ).also {
            it.displayName = "@//target_without_java_info:filegroup"
            it.baseDirectory = "file://$workspaceDir/target_without_java_info/"
          },
          BuildTarget(
            BuildTargetIdentifier("@//target_without_java_info:genrule"),
            listOf("application"),
            listOf("java", "kotlin"),
            emptyList(),
            BuildTargetCapabilities().also {
              it.canCompile = true
              it.canTest = false
              it.canRun = true
              it.canDebug = false
            },
          ).also {
            it.displayName = "@//target_without_java_info:genrule"
            it.baseDirectory = "file://$workspaceDir/target_without_java_info/"
          },
        ),
      )

    val client = createBazelClient()

    return BazelBspTestScenarioStep(
      "non module targets",
    ) {
      client.test(30.seconds) { session, _ ->
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

  private fun buildTargetWithoutOriginId(): BazelBspTestScenarioStep {
    val targetId = BuildTargetIdentifier("$targetPrefix//java_targets:java_library")
    return buildTarget(targetId, null)
  }

  private fun buildTarget(targetId: BuildTargetIdentifier, originId: String?): BazelBspTestScenarioStep {
    val params =
      CompileParams(listOf(targetId))
        .apply { this.originId = originId }

    val expectedResult =
      CompileResult(StatusCode.OK)
        .apply { this.originId = originId }

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
    val javaHome = "file://\$BAZEL_OUTPUT_BASE_PATH/external/remotejdk11_\$OS$architecturePart/"
    val jvmBuildTarget =
      JvmBuildTarget().also {
        it.javaHome = javaHome
        it.javaVersion = "11"
      }

    val jvmBuildTargetWithFlag =
      JvmBuildTarget().also {
        it.javaHome = javaHome
        it.javaVersion = "8"
      }

    val javaTargetsJavaBinary =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//java_targets:java_binary"),
        listOf("application"),
        listOf("java"),
        emptyList(),
        BuildTargetCapabilities().also {
          it.canCompile = true
          it.canTest = false
          it.canRun = true
          it.canDebug = false
        },
      )
    javaTargetsJavaBinary.displayName = "$targetPrefix//java_targets:java_binary"
    javaTargetsJavaBinary.baseDirectory = "file://\$WORKSPACE/java_targets/"
    javaTargetsJavaBinary.dataKind = "jvm"
    javaTargetsJavaBinary.data = jvmBuildTarget

    val javaTargetsJavaBinaryWithFlag =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//java_targets:java_binary_with_flag"),
        listOf("application"),
        listOf("java"),
        emptyList(),
        BuildTargetCapabilities().also {
          it.canCompile = true
          it.canTest = false
          it.canRun = true
          it.canDebug = false
        },
      )
    javaTargetsJavaBinaryWithFlag.displayName = "$targetPrefix//java_targets:java_binary_with_flag"
    javaTargetsJavaBinaryWithFlag.baseDirectory = "file://\$WORKSPACE/java_targets/"
    javaTargetsJavaBinaryWithFlag.dataKind = "jvm"
    javaTargetsJavaBinaryWithFlag.data = jvmBuildTargetWithFlag

    val scalaBuildTarget =
      ScalaBuildTarget(
        "org.scala-lang",
        "2.12.14",
        "2.12",
        ScalaPlatform.JVM,
        listOf(
          "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scala_compiler/scala-compiler-2.12.14.jar",
          "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scala_library/scala-library-2.12.14.jar",
          "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scala_reflect/scala-reflect-2.12.14.jar",
        ),
      )
    scalaBuildTarget.jvmBuildTarget = jvmBuildTarget

    val scalaTargetsScalaBinary =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//scala_targets:scala_binary"),
        listOf("application"),
        listOf("scala"),
        listOf(
          BuildTargetIdentifier("scala-compiler-2.12.14.jar"),
          BuildTargetIdentifier("scala-library-2.12.14.jar"),
          BuildTargetIdentifier("scala-reflect-2.12.14.jar"),
        ),
        BuildTargetCapabilities().also {
          it.canCompile = true
          it.canTest = false
          it.canRun = true
          it.canDebug = false
        },
      )
    scalaTargetsScalaBinary.displayName = "$targetPrefix//scala_targets:scala_binary"
    scalaTargetsScalaBinary.baseDirectory = "file://\$WORKSPACE/scala_targets/"
    scalaTargetsScalaBinary.dataKind = "scala"
    scalaTargetsScalaBinary.data = scalaBuildTarget

    val javaTargetsSubpackageSubpackage =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//java_targets/subpackage:java_library"),
        listOf("library"),
        listOf("java"),
        emptyList(),
        BuildTargetCapabilities().also {
          it.canCompile = true
          it.canTest = false
          it.canRun = false
          it.canDebug = false
        },
      )
    javaTargetsSubpackageSubpackage.displayName = "$targetPrefix//java_targets/subpackage:java_library"
    javaTargetsSubpackageSubpackage.baseDirectory = "file://\$WORKSPACE/java_targets/subpackage/"
    javaTargetsSubpackageSubpackage.dataKind = "jvm"
    javaTargetsSubpackageSubpackage.data = jvmBuildTarget

    val javaTargetsJavaLibrary =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//java_targets:java_library"),
        listOf("library"),
        listOf("java"),
        listOf(),
        BuildTargetCapabilities().also {
          it.canCompile = true
          it.canTest = false
          it.canRun = false
          it.canDebug = false
        },
      )
    javaTargetsJavaLibrary.displayName = "$targetPrefix//java_targets:java_library"
    javaTargetsJavaLibrary.baseDirectory = "file://\$WORKSPACE/java_targets/"
    javaTargetsJavaLibrary.dataKind = "jvm"
    javaTargetsJavaLibrary.data = jvmBuildTarget

    val targetWithoutJvmFlagsBinary =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//target_without_jvm_flags:binary"),
        listOf("application"),
        listOf("scala"),
        listOf(
          BuildTargetIdentifier("scala-compiler-2.12.14.jar"),
          BuildTargetIdentifier("scala-library-2.12.14.jar"),
          BuildTargetIdentifier("scala-reflect-2.12.14.jar"),
        ),
        BuildTargetCapabilities().also {
          it.canCompile = true
          it.canTest = false
          it.canRun = true
          it.canDebug = false
        },
      )
    targetWithoutJvmFlagsBinary.displayName = "$targetPrefix//target_without_jvm_flags:binary"
    targetWithoutJvmFlagsBinary.baseDirectory = "file://\$WORKSPACE/target_without_jvm_flags/"
    targetWithoutJvmFlagsBinary.dataKind = "scala"
    targetWithoutJvmFlagsBinary.data = scalaBuildTarget

    val targetWithoutMainClassLibrary =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//target_without_main_class:library"),
        listOf("library"),
        listOf("scala"),
        listOf(
          BuildTargetIdentifier("scala-compiler-2.12.14.jar"),
          BuildTargetIdentifier("scala-library-2.12.14.jar"),
          BuildTargetIdentifier("scala-reflect-2.12.14.jar"),
        ),
        BuildTargetCapabilities().also {
          it.canCompile = true
          it.canTest = false
          it.canRun = false
          it.canDebug = false
        },
      )
    targetWithoutMainClassLibrary.displayName = "$targetPrefix//target_without_main_class:library"
    targetWithoutMainClassLibrary.baseDirectory = "file://\$WORKSPACE/target_without_main_class/"
    targetWithoutMainClassLibrary.dataKind = "scala"
    targetWithoutMainClassLibrary.data = scalaBuildTarget

    val targetWithoutArgsBinary =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//target_without_args:binary"),
        listOf("application"),
        listOf("scala"),
        listOf(
          BuildTargetIdentifier("scala-compiler-2.12.14.jar"),
          BuildTargetIdentifier("scala-library-2.12.14.jar"),
          BuildTargetIdentifier("scala-reflect-2.12.14.jar"),
        ),
        BuildTargetCapabilities().also {
          it.canCompile = true
          it.canTest = false
          it.canRun = true
          it.canDebug = false
        },
      )
    targetWithoutArgsBinary.displayName = "$targetPrefix//target_without_args:binary"
    targetWithoutArgsBinary.baseDirectory = "file://\$WORKSPACE/target_without_args/"
    targetWithoutArgsBinary.dataKind = "scala"
    targetWithoutArgsBinary.data = scalaBuildTarget

    val targetWithDependencyJavaBinary =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//target_with_dependency:java_binary"),
        listOf("application"),
        listOf("java"),
        listOf(
          BuildTargetIdentifier("$targetPrefix//java_targets:java_library_exported"),
          BuildTargetIdentifier("@guava//:guava"),
          BuildTargetIdentifier("$targetPrefix//java_targets/subpackage:java_library"),
        ),
        BuildTargetCapabilities().also {
          it.canCompile = true
          it.canTest = false
          it.canRun = true
          it.canDebug = false
        },
      )
    targetWithDependencyJavaBinary.displayName = "$targetPrefix//target_with_dependency:java_binary"
    targetWithDependencyJavaBinary.baseDirectory = "file://\$WORKSPACE/target_with_dependency/"
    targetWithDependencyJavaBinary.dataKind = "jvm"
    targetWithDependencyJavaBinary.data = jvmBuildTarget

    val scalaTargetsScalaTest =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//scala_targets:scala_test"),
        listOf("test"),
        listOf("scala"),
        listOf(
          BuildTargetIdentifier("scala-compiler-2.12.14.jar"),
          BuildTargetIdentifier("scala-library-2.12.14.jar"),
          BuildTargetIdentifier("scala-reflect-2.12.14.jar"),
          BuildTargetIdentifier("librunner.jar"),
          BuildTargetIdentifier("scalactic_2.12-3.2.9.jar"),
          BuildTargetIdentifier("scalatest-compatible-3.2.9.jar"),
          BuildTargetIdentifier("scalatest-core_2.12-3.2.9.jar"),
          BuildTargetIdentifier("scalatest-featurespec_2.12-3.2.9.jar"),
          BuildTargetIdentifier("scalatest-flatspec_2.12-3.2.9.jar"),
          BuildTargetIdentifier("scalatest-freespec_2.12-3.2.9.jar"),
          BuildTargetIdentifier("scalatest-funspec_2.12-3.2.9.jar"),
          BuildTargetIdentifier("scalatest-funsuite_2.12-3.2.9.jar"),
          BuildTargetIdentifier("scalatest-matchers-core_2.12-3.2.9.jar"),
          BuildTargetIdentifier("scalatest-mustmatchers_2.12-3.2.9.jar"),
          BuildTargetIdentifier("scalatest-shouldmatchers_2.12-3.2.9.jar"),
          BuildTargetIdentifier("scalatest_2.12-3.2.9.jar"),
          BuildTargetIdentifier("test_reporter.jar"),
        ),
        BuildTargetCapabilities().also {
          it.canCompile = true
          it.canTest = true
          it.canRun = false
          it.canDebug = false
        },
      )
    scalaTargetsScalaTest.displayName = "$targetPrefix//scala_targets:scala_test"
    scalaTargetsScalaTest.baseDirectory = "file://\$WORKSPACE/scala_targets/"
    scalaTargetsScalaTest.dataKind = "scala"
    scalaTargetsScalaTest.data = scalaBuildTarget

    val targetWithResourcesJavaBinary =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//target_with_resources:java_binary"),
        listOf("application"),
        listOf("java"),
        emptyList(),
        BuildTargetCapabilities().also {
          it.canCompile = true
          it.canTest = false
          it.canRun = true
          it.canDebug = false
        },
      )
    targetWithResourcesJavaBinary.displayName = "$targetPrefix//target_with_resources:java_binary"
    targetWithResourcesJavaBinary.baseDirectory = "file://\$WORKSPACE/target_with_resources/"
    targetWithResourcesJavaBinary.dataKind = "jvm"
    targetWithResourcesJavaBinary.data = jvmBuildTarget

    val javaTargetsJavaLibraryExported =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//java_targets:java_library_exported"),
        listOf("library"),
        listOf("java"),
        listOf(
          BuildTargetIdentifier("$targetPrefix//java_targets/subpackage:java_library"),
          BuildTargetIdentifier("$targetPrefix//java_targets:java_library_exported_output_jars"),
        ),
        BuildTargetCapabilities().also {
          it.canCompile = true
          it.canTest = false
          it.canRun = false
          it.canDebug = false
        },
      )
    javaTargetsJavaLibraryExported.displayName = "$targetPrefix//java_targets:java_library_exported"
    javaTargetsJavaLibraryExported.baseDirectory = "file://\$WORKSPACE/java_targets/"
    javaTargetsJavaLibraryExported.dataKind = "jvm"
    javaTargetsJavaLibraryExported.data = jvmBuildTarget

    val environmentVariablesJavaLibrary =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//environment_variables:java_binary"),
        listOf("application"),
        listOf("java"),
        emptyList(),
        BuildTargetCapabilities().also {
          it.canCompile = true
          it.canTest = false
          it.canRun = true
          it.canDebug = false
        },
      )
    environmentVariablesJavaLibrary.displayName = "$targetPrefix//environment_variables:java_binary"
    environmentVariablesJavaLibrary.baseDirectory = "file://\$WORKSPACE/environment_variables/"
    environmentVariablesJavaLibrary.dataKind = "jvm"
    environmentVariablesJavaLibrary.data = jvmBuildTarget

    val environmentVariablesJavaTest =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//environment_variables:java_test"),
        listOf("test"),
        listOf("java"),
        emptyList(),
        BuildTargetCapabilities().also {
          it.canCompile = true
          it.canTest = true
          it.canRun = false
          it.canDebug = false
        },
      )
    environmentVariablesJavaTest.displayName = "$targetPrefix//environment_variables:java_test"
    environmentVariablesJavaTest.baseDirectory = "file://\$WORKSPACE/environment_variables/"
    environmentVariablesJavaTest.dataKind = "jvm"
    environmentVariablesJavaTest.data = jvmBuildTarget

    val targetWithJavacExportsJavaLibrary =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//target_with_javac_exports:java_library"),
        listOf("library"),
        listOf("java"),
        emptyList(),
        BuildTargetCapabilities().also {
          it.canCompile = true
          it.canTest = false
          it.canRun = false
          it.canDebug = false
        },
      )
    targetWithJavacExportsJavaLibrary.displayName = "$targetPrefix//target_with_javac_exports:java_library"
    targetWithJavacExportsJavaLibrary.baseDirectory = "file://\$WORKSPACE/target_with_javac_exports/"
    targetWithJavacExportsJavaLibrary.dataKind = "jvm"
    targetWithJavacExportsJavaLibrary.data = jvmBuildTarget

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
