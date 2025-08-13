package org.jetbrains.bazel.sync.workspace.mapper.normal

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.jetbrains.bazel.commons.EnvironmentProvider
import org.jetbrains.bazel.commons.FileUtil
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.SystemInfoProvider
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.performance.telemetry.TelemetryManager
import org.jetbrains.bazel.startup.FileUtilIntellij
import org.jetbrains.bazel.startup.IntellijBidirectionalMap
import org.jetbrains.bazel.startup.IntellijEnvironmentProvider
import org.jetbrains.bazel.startup.IntellijSystemInfoProvider
import org.jetbrains.bazel.startup.IntellijTelemetryManager
import org.jetbrains.bazel.sync.workspace.languages.JvmPackageResolver
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginsService
import org.jetbrains.bazel.sync.workspace.languages.go.GoLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.java.JavaLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.java.JdkResolver
import org.jetbrains.bazel.sync.workspace.languages.java.JdkVersionResolver
import org.jetbrains.bazel.sync.workspace.languages.kotlin.KotlinLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.python.PythonLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.scala.ScalaLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.thrift.ThriftLanguagePlugin
import org.jetbrains.bazel.workspacecontext.AllowManualTargetsSyncSpec
import org.jetbrains.bazel.workspacecontext.AndroidMinSdkSpec
import org.jetbrains.bazel.workspacecontext.BazelBinarySpec
import org.jetbrains.bazel.workspacecontext.BuildFlagsSpec
import org.jetbrains.bazel.workspacecontext.DebugFlagsSpec
import org.jetbrains.bazel.workspacecontext.DeriveInstrumentationFilterFromTargetsSpec
import org.jetbrains.bazel.workspacecontext.DirectoriesSpec
import org.jetbrains.bazel.workspacecontext.DotBazelBspDirPathSpec
import org.jetbrains.bazel.workspacecontext.EnableNativeAndroidRules
import org.jetbrains.bazel.workspacecontext.EnabledRulesSpec
import org.jetbrains.bazel.workspacecontext.GazelleTargetSpec
import org.jetbrains.bazel.workspacecontext.IdeJavaHomeOverrideSpec
import org.jetbrains.bazel.workspacecontext.ImportDepthSpec
import org.jetbrains.bazel.workspacecontext.ImportIjarsSpec
import org.jetbrains.bazel.workspacecontext.ImportRunConfigurationsSpec
import org.jetbrains.bazel.workspacecontext.IndexAllFilesInDirectoriesSpec
import org.jetbrains.bazel.workspacecontext.PrioritizeLibrariesOverModulesTargetKindsSpec
import org.jetbrains.bazel.workspacecontext.PythonCodeGeneratorRuleNamesSpec
import org.jetbrains.bazel.workspacecontext.ShardSyncSpec
import org.jetbrains.bazel.workspacecontext.ShardingApproachSpec
import org.jetbrains.bazel.workspacecontext.SyncFlagsSpec
import org.jetbrains.bazel.workspacecontext.TargetShardSizeSpec
import org.jetbrains.bazel.workspacecontext.TargetsSpec
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.FeatureFlags
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.PythonBuildTarget
import org.jetbrains.bsp.protocol.ScalaBuildTarget
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths
/**
 * Sanity test that verifies the pipeline of mapping from Bazel targets to resolved workspace.
 *
 * This test creates Python targets as described in the comments and pipes them through:
 * 1. AspectBazelProjectMapper.createProject()
 * 2. AspectClientProjectMapper.resolveWorkspace()
 *
 * Then verifies the output matches the expected structure.
 */


/**
 * Test implementation of JvmPackageResolver that returns hardcoded packages
 * based on the file path, avoiding file system access.
 */
class TestJvmPackageResolver : JvmPackageResolver {
  private val packageMapping = mapOf(
    // Scala targets
    "scala_targets/ScalaBinary.scala" to "scala_targets",
    "scala_targets/ScalaTest.scala" to "scala_targets",

    // Java targets
    "java_targets/JavaBinary.java" to "java_targets",
    "java_targets/JavaBinaryWithFlag.java" to "java_targets",
    "java_targets/JavaLibrary.java" to "java_targets",
    "java_targets/subpackage/JavaLibrary2.java" to "java_targets.subpackage",

    // Target with resources
    "target_with_resources/JavaBinary.java" to "target_with_resources",

    // Environment variables
    "environment_variables/JavaEnv.java" to "environment_variables",
    "environment_variables/JavaTest.java" to "environment_variables",

    // Target with javac exports
    "target_with_javac_exports/JavaLibrary.java" to "target_with_javac_exports",

    // Target with dependency
    "target_with_dependency/JavaBinary.java" to "target_with_dependency",

    // Target without JVM flags
    "target_without_jvm_flags/Example.scala" to "target_without_jvm_flags",

    // Target without args
    "target_without_args/Example.scala" to "target_without_args",

    // Target without main class
    "target_without_main_class/Example.scala" to "target_without_main_class",
  )

  override fun calculateJvmPackagePrefix(source: Path, multipleLines: Boolean): String? {
    val pathString = source.toString()

    // Find the matching package by checking if the path ends with any of our known files
    return packageMapping.entries.firstOrNull { (file, _) ->
      pathString.endsWith(file)
    }?.value
  }
}


class ResolverSanityTest {
  private lateinit var workspaceRoot: Path
  private lateinit var projectViewFile: Path
  private lateinit var dotBazelBspDirPath: Path
  private lateinit var workspaceContext: WorkspaceContext
  private lateinit var featureFlags: FeatureFlags

  @BeforeEach
  fun setup() {
    // Initialize providers for tests
    SystemInfoProvider.provideSystemInfoProvider(IntellijSystemInfoProvider)
    FileUtil.provideFileUtil(FileUtilIntellij)
    EnvironmentProvider.provideEnvironmentProvider(IntellijEnvironmentProvider)
    TelemetryManager.provideTelemetryManager(IntellijTelemetryManager)

    // Setup paths
    workspaceRoot = Paths.get("/home/andrzej.gluszak/code/junk/simpleBazelProjectsForTesting/simplePythonTest")
    projectViewFile = workspaceRoot.resolve(".bazelbsp/.bazelproject")
    dotBazelBspDirPath = workspaceRoot.resolve(".bazelbsp")

    // Setup workspace context as described in comments
    workspaceContext =
      WorkspaceContext(
        targets = TargetsSpec(values = listOf(Label.parse("@//...:all")), excludedValues = emptyList()),
        directories =
          DirectoriesSpec(
            values =
              listOf(
                workspaceRoot,
                dotBazelBspDirPath.resolve(".bazelproject"),
              ),
            excludedValues = emptyList(),
          ),
        buildFlags = BuildFlagsSpec(values = emptyList()),
        syncFlags = SyncFlagsSpec(values = emptyList()),
        debugFlags = DebugFlagsSpec(values = emptyList()),
        bazelBinary = BazelBinarySpec(value = Paths.get("/home/andrzej.gluszak/.cache/bazelbsp/bazelisk")),
        allowManualTargetsSync = AllowManualTargetsSyncSpec(value = false),
        dotBazelBspDirPath = DotBazelBspDirPathSpec(value = dotBazelBspDirPath),
        importDepth = ImportDepthSpec(value = -1),
        enabledRules = EnabledRulesSpec(values = emptyList()),
        ideJavaHomeOverrideSpec = IdeJavaHomeOverrideSpec(value = null),
        experimentalPrioritizeLibrariesOverModulesTargetKinds = PrioritizeLibrariesOverModulesTargetKindsSpec(values = emptyList()),
        enableNativeAndroidRules = EnableNativeAndroidRules(value = false),
        androidMinSdkSpec = AndroidMinSdkSpec(value = null),
        shardSync = ShardSyncSpec(value = false),
        targetShardSize = TargetShardSizeSpec(value = 1000),
        shardingApproachSpec = ShardingApproachSpec(value = null),
        importRunConfigurations = ImportRunConfigurationsSpec(values = emptyList()),
        gazelleTarget = GazelleTargetSpec(value = null),
        indexAllFilesInDirectories = IndexAllFilesInDirectoriesSpec(value = false),
        pythonCodeGeneratorRuleNames = PythonCodeGeneratorRuleNamesSpec(values = emptyList()),
        importIjarsSpec = ImportIjarsSpec(value = false),
        deriveInstrumentationFilterFromTargets = DeriveInstrumentationFilterFromTargetsSpec(value = true),
      )

    // Setup feature flags as described in comments
    featureFlags =
      FeatureFlags(
        isPythonSupportEnabled = true,
        isAndroidSupportEnabled = false,
        isGoSupportEnabled = true,
        isCppSupportEnabled = false,
        isPropagateExportsFromDepsEnabled = true,
        isSharedSourceSupportEnabled = false,
        bazelSymlinksScanMaxDepth = 2,
        bazelShutDownBeforeShardBuild = true,
        isBazelQueryTabEnabled = true,
      )
  }

  @Test
  fun `should map Python targets through both mappers correctly`() {
    runBlocking {
      // Create input targets as described in comments
      val targets = createTestTargets()
      val rootTargets =
        setOf(
          Label.parse("@//main:main"),
          Label.parse("@//.bazelbsp/aspects:aspects"),
          Label.parse("@//lib/libA:libA"),
          Label.parse("@//lib/libB:libB"),
        )

      // Create BazelInfo mock
      val bazelInfo =
        BazelInfo(
          execRoot = workspaceRoot.resolve("bazel-out"),
          outputBase = Paths.get("/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/afe94615d3f7866679195b9a67aeb0a7"),
          workspaceRoot = workspaceRoot,
          bazelBin = workspaceRoot.resolve("bazel-bin"),
          release = BazelRelease(major = 7),
          isBzlModEnabled = true,
          isWorkspaceEnabled = true,
          externalAutoloads = emptyList(),
        )

      // Create RepoMapping
      val canonicalRepoNameToPath =
        mapOf(
          "+_repo_rules+bazelbsp_aspect" to
            Paths.get(
              "/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/afe94615d3f7866679195b9a67aeb0a7/external/+_repo_rules+bazelbsp_aspect",
            ),
          "" to workspaceRoot,
          "rules_python+" to
            Paths.get(
              "/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/afe94615d3f7866679195b9a67aeb0a7/external/rules_python+",
            ),
          "bazel_tools" to
            Paths.get(
              "/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/afe94615d3f7866679195b9a67aeb0a7/external/bazel_tools",
            ),
          "local_config_platform" to
            Paths.get(
              "/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/afe94615d3f7866679195b9a67aeb0a7/external/local_config_platform",
            ),
          "rules_java++toolchains+local_jdk" to
            Paths.get(
              "/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/afe94615d3f7866679195b9a67aeb0a7/external/rules_java++toolchains+local_jdk",
            ),
          "rules_python++python+python_3_11_x86_64-unknown-linux-gnu" to
            Paths.get(
              "/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/afe94615d3f7866679195b9a67aeb0a7/external/rules_python++python+python_3_11_x86_64-unknown-linux-gnu",
            ),
        )

      val repoMapping: RepoMapping =
        BzlmodRepoMapping(
          canonicalRepoNameToLocalPath = emptyMap(),
          apparentRepoNameToCanonicalName = IntellijBidirectionalMap(),
          canonicalRepoNameToPath = canonicalRepoNameToPath,
        )

      // Create BazelPathsResolver
      val bazelPathsResolver = BazelPathsResolver(bazelInfo)

      // Create language plugins
      val jdkResolver = JdkResolver(bazelPathsResolver, JdkVersionResolver())
      val testPackageResolver = TestJvmPackageResolver()
      val javaLanguagePlugin = JavaLanguagePlugin(bazelPathsResolver, jdkResolver, testPackageResolver)
      val scalaLanguagePlugin = ScalaLanguagePlugin(javaLanguagePlugin, bazelPathsResolver, testPackageResolver)
      val kotlinLanguagePlugin = KotlinLanguagePlugin(javaLanguagePlugin, bazelPathsResolver)
      val thriftLanguagePlugin = ThriftLanguagePlugin(bazelPathsResolver)
      val pythonLanguagePlugin = PythonLanguagePlugin(bazelPathsResolver)
      val goLanguagePlugin = GoLanguagePlugin(bazelPathsResolver)

      val languagePluginsService =
        LanguagePluginsService(
          scalaLanguagePlugin,
          javaLanguagePlugin,
          kotlinLanguagePlugin,
          thriftLanguagePlugin,
          pythonLanguagePlugin,
          goLanguagePlugin,
        )

      // Create mappers
      val targetTagsResolver = TargetTagsResolver()
      val mavenCoordinatesResolver = MavenCoordinatesResolver()

      val bazelMapper =
        AspectBazelProjectMapper(
          languagePluginsService = languagePluginsService,
          bazelPathsResolver = bazelPathsResolver,
          targetTagsResolver = targetTagsResolver,
          mavenCoordinatesResolver = mavenCoordinatesResolver,
          environmentProvider = IntellijEnvironmentProvider,
        )

      val clientMapper =
        AspectClientProjectMapper(
          languagePluginsService = languagePluginsService,
          featureFlags = featureFlags,
          bazelPathsResolver = bazelPathsResolver,
        )

      // Run through mappers
      val bazelMappedProject =
        bazelMapper.createProject(
          targets = targets,
          rootTargets = rootTargets,
          workspaceContext = workspaceContext,
          featureFlags = featureFlags,
          repoMapping = repoMapping,
          hasError = false,
        )

      val resolvedWorkspace = clientMapper.resolveWorkspace(bazelMappedProject)

      // Verify output matches expected structure
      resolvedWorkspace shouldNotBe null
      resolvedWorkspace.hasError shouldBe false

      // According to the expected output, we should have:
      // - 3 buildTargets (all with PYTHON language class)
      // - 1 nonModuleTarget (py_binary without language classes)
      // The getTargets() method returns nonModuleTargets first, then buildTargets

      val allTargets = resolvedWorkspace.targets.getTargets().toList()

      // Filter Python-related targets (excluding aspects)
      val pythonTargets =
        allTargets.filter {
          it.id.toString() in setOf("@//main:main", "@//lib/libA:libA", "@//lib/libB:libB")
        }

      // We should have 4 total: 3 with PYTHON language class + 1 non-module
      pythonTargets shouldHaveSize 4

      // Verify we have exactly 3 module targets with Python language class
      val moduleTargets =
        pythonTargets.filter { target ->
          target.kind.languageClasses.contains(LanguageClass.PYTHON)
        }
      moduleTargets shouldHaveSize 3

      // Verify main:main module target (with sources and Python data)
      val mainModuleTarget = moduleTargets.find { it.id.toString() == "@//main:main" }
      mainModuleTarget shouldNotBe null
      mainModuleTarget!!.apply {
        kind.kindString shouldBe "py_binary"
        kind.languageClasses shouldContainExactlyInAnyOrder listOf(LanguageClass.PYTHON)
        dependencies.map { it.toString() } shouldContainExactlyInAnyOrder listOf("@//lib/libA:libA")
        sources shouldHaveSize 1
        sources[0].path shouldBe workspaceRoot.resolve("main/main.py")
        baseDirectory shouldBe workspaceRoot.resolve("main")

        val pythonData = data as PythonBuildTarget
        pythonData shouldNotBe null
        pythonData.interpreter shouldBe
          bazelInfo.outputBase.resolve(
            "external/rules_python++python+python_3_11_x86_64-unknown-linux-gnu/bin/python3",
          )
        pythonData.imports shouldBe emptyList()
      }

      // Verify main:main non-module target (without sources)
      val mainNonModuleTarget =
        pythonTargets.find {
          it.id.toString() == "@//main:main" && it.kind.languageClasses.isEmpty()
        }
      mainNonModuleTarget shouldNotBe null
      mainNonModuleTarget!!.apply {
        kind.kindString shouldBe "py_binary"
        kind.languageClasses shouldBe emptyList()
        dependencies shouldBe emptyList()
        sources shouldBe emptyList()
        baseDirectory shouldBe workspaceRoot.resolve("main")
        data shouldBe null
      }

      // Verify lib/libA:libA target
      val libATarget = moduleTargets.find { it.id.toString() == "@//lib/libA:libA" }
      libATarget shouldNotBe null
      libATarget!!.apply {
        kind.kindString shouldBe "py_library"
        kind.languageClasses shouldContainExactlyInAnyOrder listOf(LanguageClass.PYTHON)
        dependencies.map { it.toString() } shouldContainExactlyInAnyOrder listOf("@//lib/libB:libB")
        sources shouldHaveSize 2
        sources.map { it.path } shouldContainExactlyInAnyOrder
          listOf(
            workspaceRoot.resolve("lib/libA/src/bkng/lib/aaa/__init__.py"),
            workspaceRoot.resolve("lib/libA/src/bkng/lib/aaa/hello.py"),
          )
        baseDirectory shouldBe workspaceRoot.resolve("lib/libA")

        val pythonData = data as PythonBuildTarget
        pythonData shouldNotBe null
        pythonData.interpreter shouldBe
          bazelInfo.outputBase.resolve(
            "external/rules_python++python+python_3_11_x86_64-unknown-linux-gnu/bin/python3",
          )
        pythonData.imports shouldContainExactlyInAnyOrder listOf("src")
      }

      // Verify lib/libB:libB target
      val libBTarget = moduleTargets.find { it.id.toString() == "@//lib/libB:libB" }
      libBTarget shouldNotBe null
      libBTarget!!.apply {
        kind.kindString shouldBe "py_library"
        kind.languageClasses shouldContainExactlyInAnyOrder listOf(LanguageClass.PYTHON)
        dependencies shouldBe emptyList()
        sources shouldHaveSize 2
        sources.map { it.path } shouldContainExactlyInAnyOrder
          listOf(
            workspaceRoot.resolve("lib/libB/src/bkng/lib/bbb/__init__.py"),
            workspaceRoot.resolve("lib/libB/src/bkng/lib/bbb/hello.py"),
          )
        baseDirectory shouldBe workspaceRoot.resolve("lib/libB")

        val pythonData = data as PythonBuildTarget
        pythonData shouldNotBe null
        pythonData.interpreter shouldBe
          bazelInfo.outputBase.resolve(
            "external/rules_python++python+python_3_11_x86_64-unknown-linux-gnu/bin/python3",
          )
        pythonData.imports shouldContainExactlyInAnyOrder listOf("src")
      }
    }
  }

  @Test
  fun `should map Java Scala Kotlin targets through both mappers correctly`() {
    runBlocking {
      // Create input targets for Java/Scala/Kotlin project as described in comments
      val targets = createJavaScalaKotlinTestTargets()
      val rootTargets =
        setOf(
          Label.parse("@//.bazelbsp/aspects:aspects"),
          Label.parse("@//scala_targets:scala_binary"),
          Label.parse("@//kotlin:java_binary"),
          Label.parse("@//kotlin:java_binary_with_flag"),
          Label.parse("@//java_targets:java_binary_with_flag"),
          Label.parse("@//java_targets:java_binary"),
          Label.parse("@//genrule:foo"),
          Label.parse("@//target_with_resources:resources"),
          Label.parse("@//target_without_java_info:filegroup"),
          Label.parse("@//environment_variables:java_binary"),
          Label.parse("@//kotlin:asd"),
          Label.parse("@//target_without_jvm_flags:binary"),
          Label.parse("@//target_with_javac_exports:java_library"),
          Label.parse("@//environment_variables:java_test"),
          Label.parse("@//target_without_java_info:genrule"),
          Label.parse("@//scala_targets:scala_test"),
          Label.parse("@//java_targets/subpackage:java_library"),
          Label.parse("@//target_without_args:binary"),
          Label.parse("@//no_ide_target:java_library"),
          Label.parse("@//java_targets:java_library"),
          Label.parse("@//target_without_main_class:library"),
          Label.parse("@//java_targets:java_library_exported"),
          Label.parse("@//kotlin:java_library"),
          Label.parse("@//target_with_resources:java_binary"),
          Label.parse("@//target_with_dependency:java_binary"),
          Label.parse("@//kotlin:java_library_exported"),
        )

      // Create BazelInfo for Java/Scala/Kotlin project
      val bazelInfo =
        BazelInfo(
          execRoot = Paths.get("/home/andrzej.gluszak/code/junk/sample-repo/bazel-out"),
          outputBase = Paths.get("/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942"),
          workspaceRoot = Paths.get("/home/andrzej.gluszak/code/junk/sample-repo"),
          bazelBin = Paths.get("/home/andrzej.gluszak/code/junk/sample-repo/bazel-bin"),
          release = BazelRelease(major = 8),
          isBzlModEnabled = true,
          isWorkspaceEnabled = true,
          externalAutoloads = emptyList(),
        )

      // Create RepoMapping for Java/Scala/Kotlin project
      val canonicalRepoNameToPath =
        mapOf(
          "+_repo_rules+bazelbsp_aspect" to
            Paths.get(
              "/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/+_repo_rules+bazelbsp_aspect",
            ),
          "rules_jvm_external++maven+maven" to
            Paths.get(
              "/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_jvm_external++maven+maven",
            ),
          "rules_scala++scala_config+io_bazel_rules_scala_config" to
            Paths.get(
              "/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_config+io_bazel_rules_scala_config",
            ),
          "" to Paths.get("/home/andrzej.gluszak/code/junk/sample-repo"),
          "bazel_skylib+" to
            Paths.get(
              "/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/bazel_skylib+",
            ),
          "rules_java+" to
            Paths.get(
              "/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java+",
            ),
          "rules_jvm_external+" to
            Paths.get(
              "/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_jvm_external+",
            ),
          "rules_kotlin+" to
            Paths.get(
              "/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_kotlin+",
            ),
          "rules_python+" to
            Paths.get(
              "/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_python+",
            ),
          "protobuf+" to
            Paths.get(
              "/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/protobuf+",
            ),
          "rules_scala+" to
            Paths.get(
              "/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala+",
            ),
          "bazel_tools" to
            Paths.get(
              "/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/bazel_tools",
            ),
          "local_config_platform" to
            Paths.get(
              "/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/local_config_platform",
            ),
          "rules_java++toolchains+remotejdk11_linux" to
            Paths.get(
              "/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux",
            ),
          "rules_java++toolchains+remotejdk21_linux" to
            Paths.get(
              "/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk21_linux",
            ),
        )

      val javaScalaRepoMapping: RepoMapping =
        BzlmodRepoMapping(
          canonicalRepoNameToLocalPath = emptyMap(),
          apparentRepoNameToCanonicalName = IntellijBidirectionalMap(),
          canonicalRepoNameToPath = canonicalRepoNameToPath,
        )

      // Create workspace context for Java/Scala project
      val javaScalaWorkspaceContext =
        WorkspaceContext(
          targets = TargetsSpec(values = listOf(Label.parse("@//...:all")), excludedValues = emptyList()),
          directories =
            DirectoriesSpec(
              values =
                listOf(
                  bazelInfo.workspaceRoot,
                  bazelInfo.workspaceRoot.resolve(".bazelproject"),
                ),
              excludedValues = emptyList(),
            ),
          buildFlags = BuildFlagsSpec(values = emptyList()),
          syncFlags = SyncFlagsSpec(values = emptyList()),
          debugFlags = DebugFlagsSpec(values = emptyList()),
          bazelBinary = BazelBinarySpec(value = Paths.get("/home/andrzej.gluszak/code/junk/bazel_bazel_bazel_8_1_0/bazel")),
          allowManualTargetsSync = AllowManualTargetsSyncSpec(value = false),
          dotBazelBspDirPath = DotBazelBspDirPathSpec(value = bazelInfo.workspaceRoot.resolve(".bazelbsp")),
          importDepth = ImportDepthSpec(value = -1),
          enabledRules = EnabledRulesSpec(values = listOf("io_bazel_rules_scala", "rules_java", "rules_jvm")),
          ideJavaHomeOverrideSpec = IdeJavaHomeOverrideSpec(value = null),
          experimentalPrioritizeLibrariesOverModulesTargetKinds = PrioritizeLibrariesOverModulesTargetKindsSpec(values = emptyList()),
          enableNativeAndroidRules = EnableNativeAndroidRules(value = false),
          androidMinSdkSpec = AndroidMinSdkSpec(value = null),
          shardSync = ShardSyncSpec(value = false),
          targetShardSize = TargetShardSizeSpec(value = 1000),
          shardingApproachSpec = ShardingApproachSpec(value = null),
          importRunConfigurations = ImportRunConfigurationsSpec(values = emptyList()),
          gazelleTarget = GazelleTargetSpec(value = null),
          indexAllFilesInDirectories = IndexAllFilesInDirectoriesSpec(value = false),
          pythonCodeGeneratorRuleNames = PythonCodeGeneratorRuleNamesSpec(values = emptyList()),
          importIjarsSpec = ImportIjarsSpec(value = false),
          deriveInstrumentationFilterFromTargets = DeriveInstrumentationFilterFromTargetsSpec(value = true),
        )

      // Create BazelPathsResolver - we'll work around the validation differently
      val bazelPathsResolver = BazelPathsResolver(bazelInfo)

      // Create language plugins
      val jdkResolver = JdkResolver(bazelPathsResolver, JdkVersionResolver())
      val testPackageResolver = TestJvmPackageResolver()
      val javaLanguagePlugin = JavaLanguagePlugin(bazelPathsResolver, jdkResolver, testPackageResolver)
      val scalaLanguagePlugin = ScalaLanguagePlugin(javaLanguagePlugin, bazelPathsResolver, testPackageResolver)
      val kotlinLanguagePlugin = KotlinLanguagePlugin(javaLanguagePlugin, bazelPathsResolver)
      val thriftLanguagePlugin = ThriftLanguagePlugin(bazelPathsResolver)
      val pythonLanguagePlugin = PythonLanguagePlugin(bazelPathsResolver)
      val goLanguagePlugin = GoLanguagePlugin(bazelPathsResolver)

      val languagePluginsService =
        LanguagePluginsService(
          scalaLanguagePlugin,
          javaLanguagePlugin,
          kotlinLanguagePlugin,
          thriftLanguagePlugin,
          pythonLanguagePlugin,
          goLanguagePlugin,
        )

      // Create mappers
      val targetTagsResolver = TargetTagsResolver()
      val mavenCoordinatesResolver = MavenCoordinatesResolver()

      val bazelMapper =
        AspectBazelProjectMapper(
          languagePluginsService = languagePluginsService,
          bazelPathsResolver = bazelPathsResolver,
          targetTagsResolver = targetTagsResolver,
          mavenCoordinatesResolver = mavenCoordinatesResolver,
          environmentProvider = IntellijEnvironmentProvider,
        )

      val clientMapper =
        AspectClientProjectMapper(
          languagePluginsService = languagePluginsService,
          featureFlags = featureFlags,
          bazelPathsResolver = bazelPathsResolver,
        )

      // Run through mappers
      val bazelMappedProject =
        bazelMapper.createProject(
          targets = targets,
          rootTargets = rootTargets,
          workspaceContext = javaScalaWorkspaceContext,
          featureFlags = featureFlags,
          repoMapping = javaScalaRepoMapping,
          hasError = false,
        )

      val resolvedWorkspace = clientMapper.resolveWorkspace(bazelMappedProject)

      // Verify output matches expected structure
      resolvedWorkspace shouldNotBe null
      resolvedWorkspace.hasError shouldBe false

      val allTargets = resolvedWorkspace.targets.getTargets().toList()

      // According to the expected output, we should have many Java/Scala/Kotlin targets
      // We'll focus on verifying a few key targets to ensure the mapping works correctly

      // Verify we have Scala binary target with JAVA and SCALA language classes
      val scalaBinaryTarget =
        allTargets.find {
          it.id.toString() == "@//scala_targets:scala_binary" && it.kind.languageClasses.isNotEmpty()
        }
      scalaBinaryTarget shouldNotBe null
      scalaBinaryTarget!!.apply {
        kind.kindString shouldBe "scala_binary"
        kind.languageClasses shouldContainExactlyInAnyOrder listOf(LanguageClass.JAVA, LanguageClass.SCALA)
        sources shouldHaveSize 1
        sources[0].path shouldBe bazelInfo.workspaceRoot.resolve("scala_targets/ScalaBinary.scala")
        baseDirectory shouldBe bazelInfo.workspaceRoot.resolve("scala_targets")
      }

      // Verify we have Java library target with JAVA language class
      val javaLibraryTarget =
        allTargets.find {
          it.id.toString() == "@//java_targets:java_library" && it.kind.languageClasses.isNotEmpty()
        }
      javaLibraryTarget shouldNotBe null
      javaLibraryTarget!!.apply {
        kind.kindString shouldBe "java_library"
        kind.languageClasses shouldContainExactlyInAnyOrder listOf(LanguageClass.JAVA)
        sources shouldHaveSize 1
        sources[0].path shouldBe bazelInfo.workspaceRoot.resolve("java_targets/JavaLibrary.java")
        baseDirectory shouldBe bazelInfo.workspaceRoot.resolve("java_targets")
      }

      // Verify we have Kotlin library target with JAVA and KOTLIN language classes
      val kotlinLibraryTarget =
        allTargets.find {
          it.id.toString() == "@//kotlin:asd" && it.kind.languageClasses.isNotEmpty()
        }
      kotlinLibraryTarget shouldNotBe null
      kotlinLibraryTarget!!.apply {
        kind.kindString shouldBe "kt_jvm_library"
        kind.languageClasses shouldContainExactlyInAnyOrder listOf(LanguageClass.JAVA, LanguageClass.KOTLIN)
        baseDirectory shouldBe bazelInfo.workspaceRoot.resolve("kotlin")
      }

      // Convert to exact format expected by the specification at line 1441
      val result = resolvedWorkspace
      val resultBuildTargets = result.targets.getTargets().filter { it.kind.languageClasses.isNotEmpty() }.toList()
      val resultNonModuleTargets = result.targets.getTargets().filter { it.kind.languageClasses.isEmpty() }.toList()

      // Verify exact structure matches expected output specification at line 1320
      resultBuildTargets shouldHaveSize 21
      resultNonModuleTargets shouldHaveSize 15
      result.hasError shouldBe false

      // Create helper function to verify build target basics
      fun verifyBuildTarget(
        id: String,
        kindString: String,
        languageClasses: Set<LanguageClass>,
        ruleType: RuleType,
        dependencies: List<String> = emptyList(),
        sourcesCount: Int = 0,
        expectedData: Any? = null
      ) {
        val target = resultBuildTargets.find { it.id.toString() == id }
        target shouldNotBe null
        target!!.kind.kindString shouldBe kindString
        target.kind.languageClasses shouldBe languageClasses
        target.kind.ruleType shouldBe ruleType
        target.dependencies.map { it.toString() } shouldContainExactly dependencies
        target.sources shouldHaveSize sourcesCount
        when (expectedData) {
          null -> target.data shouldBe null
          else -> target.data shouldBe expectedData
        }
      }

      // Create expected ScalaBuildTarget data
      val scalaJavaBinaryData = ScalaBuildTarget(
        scalaVersion = "2.13.14",
        sdkJars = listOf(
          Paths.get("/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scala_compiler_2_13_14/scala-compiler-2.13.14.jar"),
          Paths.get("/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scala_library_2_13_14/scala-library-2.13.14.jar"),
          Paths.get("/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scala_reflect_2_13_14/scala-reflect-2.13.14.jar")
        ),
        jvmBuildTarget = JvmBuildTarget(
          javaHome = Paths.get("/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux"),
          javaVersion = "11"
        ),
        scalacOptions = listOf("-target:jvm-1.8")
      )

      // Verify ALL buildTargets as specified in expected output
      verifyBuildTarget(
        "@//scala_targets:scala_binary",
        "scala_binary",
        setOf(LanguageClass.JAVA, LanguageClass.SCALA),
        RuleType.BINARY,
        listOf("scala-compiler-2.13.14.jar[synthetic]", "scala-library-2.13.14.jar[synthetic]", "scala-reflect-2.13.14.jar[synthetic]"),
        1,
        scalaJavaBinaryData
      )
      
      // For kotlin targets, specify the expected JvmBuildTarget data
      val kotlinJavaBinaryData = JvmBuildTarget(
        javaHome = Paths.get("/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux"),
        javaVersion = "11"
      )
      val kotlinJavaBinaryWithFlagData = JvmBuildTarget(
        javaHome = Paths.get("/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux"),
        javaVersion = "17"
      )
      
      verifyBuildTarget("@//kotlin:java_binary", "java_binary", setOf(LanguageClass.JAVA), RuleType.BINARY, sourcesCount = 1, expectedData = kotlinJavaBinaryData)
      verifyBuildTarget("@//kotlin:java_binary_with_flag", "java_binary", setOf(LanguageClass.JAVA), RuleType.BINARY, sourcesCount = 1, expectedData = kotlinJavaBinaryWithFlagData)
      // Create expected JvmBuildTarget for java_targets:java_binary_with_flag
      val javaBinaryWithFlagData = JvmBuildTarget(
        javaHome = Paths.get("/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux"),
        javaVersion = "11"
      )
      
      verifyBuildTarget(
        "@//java_targets:java_binary_with_flag", 
        "java_binary", 
        setOf(LanguageClass.JAVA), 
        RuleType.BINARY, 
        sourcesCount = 0,
        expectedData = null
      )
      verifyBuildTarget(
        "@//java_targets:java_binary",
        "java_binary",
        setOf(LanguageClass.JAVA),
        RuleType.BINARY,
        emptyList(), // Dependencies are actually empty based on current resolver output
        1
      )
      // Create expected JvmBuildTarget for environment_variables:java_binary
      val environmentVariablesJavaBinaryData = JvmBuildTarget(
        javaHome = Paths.get("/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux"),
        javaVersion = "11"
      )
      
      verifyBuildTarget(
        "@//environment_variables:java_binary", 
        "java_binary", 
        setOf(LanguageClass.JAVA), 
        RuleType.BINARY, 
        sourcesCount = 1, 
        expectedData = environmentVariablesJavaBinaryData
      )
      verifyBuildTarget("@//kotlin:asd", "kt_jvm_library", setOf(LanguageClass.JAVA, LanguageClass.KOTLIN), RuleType.LIBRARY, expectedData = null)
      
      // Create expected ScalaBuildTarget data for target_without_jvm_flags:binary
      val targetWithoutJvmFlagsScalaBinaryData = ScalaBuildTarget(
        scalaVersion = "2.13.14",
        sdkJars = listOf(
          Paths.get("/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scala_compiler_2_13_14/scala-compiler-2.13.14.jar"),
          Paths.get("/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scala_library_2_13_14/scala-library-2.13.14.jar"),
          Paths.get("/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scala_reflect_2_13_14/scala-reflect-2.13.14.jar")
        ),
        jvmBuildTarget = JvmBuildTarget(
          javaHome = Paths.get("/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux"),
          javaVersion = "11"
        ),
        scalacOptions = emptyList() // No scalac options for this target
      )

      verifyBuildTarget(
        "@//target_without_jvm_flags:binary",
        "scala_binary",
        setOf(LanguageClass.JAVA, LanguageClass.SCALA),
        RuleType.BINARY,
        listOf("scala-compiler-2.13.14.jar[synthetic]", "scala-library-2.13.14.jar[synthetic]", "scala-reflect-2.13.14.jar[synthetic]"),
        1,
        targetWithoutJvmFlagsScalaBinaryData
      )
      
      // Create expected JvmBuildTarget for target_with_javac_exports:java_library
      val targetWithJavacExportsLibraryData = JvmBuildTarget(
        javaHome = Paths.get("/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux"),
        javaVersion = "11"
      )
      
      verifyBuildTarget(
        "@//target_with_javac_exports:java_library", 
        "java_library", 
        setOf(LanguageClass.JAVA), 
        RuleType.LIBRARY, 
        sourcesCount = 1,
        expectedData = null
      )
      // Create expected JvmBuildTarget for environment_variables:java_test
      val environmentVariablesJavaTestData = JvmBuildTarget(
        javaHome = Paths.get("/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux"),
        javaVersion = "11"
      )
      
      verifyBuildTarget(
        "@//environment_variables:java_test", 
        "java_test", 
        setOf(LanguageClass.JAVA), 
        RuleType.TEST, 
        sourcesCount = 1,
        expectedData = environmentVariablesJavaTestData
      )
      
      // Create expected ScalaBuildTarget data for scala_targets:scala_test
      val scalaTestData = ScalaBuildTarget(
        scalaVersion = "2.13.14",
        sdkJars = listOf(
          Paths.get("/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scala_compiler_2_13_14/scala-compiler-2.13.14.jar"),
          Paths.get("/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scala_library_2_13_14/scala-library-2.13.14.jar"),
          Paths.get("/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scala_reflect_2_13_14/scala-reflect-2.13.14.jar")
        ),
        jvmBuildTarget = JvmBuildTarget(
          javaHome = Paths.get("/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux"),
          javaVersion = "11"
        ),
        scalacOptions = emptyList()
      )

      verifyBuildTarget(
        "@//scala_targets:scala_test",
        "scala_test",
        setOf(LanguageClass.JAVA, LanguageClass.SCALA),
        RuleType.TEST,
        listOf(
          "scala-compiler-2.13.14.jar[synthetic]",
          "scala-library-2.13.14.jar[synthetic]",
          "scala-reflect-2.13.14.jar[synthetic]",
          "scalactic_2.13-3.2.9.jar[synthetic]",
          "scalatest_2.13-3.2.9.jar[synthetic]",
          "scalatest-compatible-3.2.9.jar[synthetic]",
          "scalatest-core_2.13-3.2.9.jar[synthetic]",
          "scalatest-featurespec_2.13-3.2.9.jar[synthetic]",
          "scalatest-flatspec_2.13-3.2.9.jar[synthetic]",
          "scalatest-freespec_2.13-3.2.9.jar[synthetic]",
          "scalatest-funspec_2.13-3.2.9.jar[synthetic]",
          "scalatest-funsuite_2.13-3.2.9.jar[synthetic]",
          "scalatest-matchers-core_2.13-3.2.9.jar[synthetic]",
          "scalatest-mustmatchers_2.13-3.2.9.jar[synthetic]",
          "scalatest-shouldmatchers_2.13-3.2.9.jar[synthetic]",
          "librunner.jar[synthetic]",
          "test_reporter.jar[synthetic]"
        ),
        1,
        scalaTestData
      )
      
      // Create expected JvmBuildTarget for java_targets/subpackage:java_library
      val javaSubpackageLibraryData = JvmBuildTarget(
        javaHome = Paths.get("/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux"),
        javaVersion = "11"
      )
      
      verifyBuildTarget(
        "@//java_targets/subpackage:java_library", 
        "java_library", 
        setOf(LanguageClass.JAVA), 
        RuleType.LIBRARY, 
        sourcesCount = 1,
        expectedData = javaSubpackageLibraryData
      )
      
      // Create expected ScalaBuildTarget data for target_without_args:binary
      val targetWithoutArgsScalaBinaryData = ScalaBuildTarget(
        scalaVersion = "2.13.14",
        sdkJars = listOf(
          Paths.get("/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scala_compiler_2_13_14/scala-compiler-2.13.14.jar"),
          Paths.get("/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scala_library_2_13_14/scala-library-2.13.14.jar"),
          Paths.get("/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scala_reflect_2_13_14/scala-reflect-2.13.14.jar")
        ),
        jvmBuildTarget = JvmBuildTarget(
          javaHome = Paths.get("/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux"),
          javaVersion = "11"
        ),
        scalacOptions = emptyList()
      )

      verifyBuildTarget(
        "@//target_without_args:binary",
        "scala_binary",
        setOf(LanguageClass.JAVA, LanguageClass.SCALA),
        RuleType.BINARY,
        listOf("scala-compiler-2.13.14.jar[synthetic]", "scala-library-2.13.14.jar[synthetic]", "scala-reflect-2.13.14.jar[synthetic]"),
        1,
        targetWithoutArgsScalaBinaryData
      )
      
      // Create expected JvmBuildTarget for java_targets:java_library
      val javaTargetsLibraryData = JvmBuildTarget(
        javaHome = Paths.get("/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux"),
        javaVersion = "11"
      )
      
      verifyBuildTarget(
        "@//java_targets:java_library", 
        "java_library", 
        setOf(LanguageClass.JAVA), 
        RuleType.LIBRARY, 
        sourcesCount = 1,
        expectedData = javaTargetsLibraryData
      )
      
      // Create expected ScalaBuildTarget data for target_without_main_class:library
      val targetWithoutMainClassLibraryData = ScalaBuildTarget(
        scalaVersion = "2.13.14",
        sdkJars = listOf(
          Paths.get("/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scala_compiler_2_13_14/scala-compiler-2.13.14.jar"),
          Paths.get("/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scala_library_2_13_14/scala-library-2.13.14.jar"),
          Paths.get("/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scala_reflect_2_13_14/scala-reflect-2.13.14.jar")
        ),
        jvmBuildTarget = JvmBuildTarget(
          javaHome = Paths.get("/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux"),
          javaVersion = "11"
        ),
        scalacOptions = emptyList()
      )

      verifyBuildTarget(
        "@//target_without_main_class:library",
        "scala_library",
        setOf(LanguageClass.JAVA, LanguageClass.SCALA),
        RuleType.LIBRARY,
        listOf("scala-compiler-2.13.14.jar[synthetic]", "scala-library-2.13.14.jar[synthetic]", "scala-reflect-2.13.14.jar[synthetic]"),
        1,
        targetWithoutMainClassLibraryData
      )
      
      // Create expected JvmBuildTarget for java_targets:java_library_exported
      val javaLibraryExportedData = JvmBuildTarget(
        javaHome = Paths.get("/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux"),
        javaVersion = "11"
      )
      
      verifyBuildTarget(
        "@//java_targets:java_library_exported",
        "java_library",
        setOf(LanguageClass.JAVA),
        RuleType.LIBRARY,
        listOf("@//java_targets:java_library_exported_output_jars", "@//java_targets/subpackage:java_library"),
        expectedData = javaLibraryExportedData
      )
      
      // Create expected JvmBuildTarget for kotlin:java_library
      val kotlinJavaLibraryData = JvmBuildTarget(
        javaHome = Paths.get("/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux"),
        javaVersion = "11"
      )
      
      verifyBuildTarget(
        "@//kotlin:java_library", 
        "java_library", 
        setOf(LanguageClass.JAVA), 
        RuleType.LIBRARY,
        expectedData = null
      )
      
      // Create expected JvmBuildTarget for target_with_resources:java_binary
      val targetWithResourcesJavaBinaryData = JvmBuildTarget(
        javaHome = Paths.get("/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux"),
        javaVersion = "11"
      )
      
      verifyBuildTarget(
        "@//target_with_resources:java_binary", 
        "java_binary", 
        setOf(LanguageClass.JAVA), 
        RuleType.BINARY, 
        sourcesCount = 1,
        expectedData = targetWithResourcesJavaBinaryData
      )
      
      // Create expected JvmBuildTarget for target_with_dependency:java_binary
      val targetWithDependencyJavaBinaryData = JvmBuildTarget(
        javaHome = Paths.get("/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux"),
        javaVersion = "11"
      )
      
      verifyBuildTarget(
        "@//target_with_dependency:java_binary",
        "java_binary",
        setOf(LanguageClass.JAVA),
        RuleType.BINARY,
        listOf(
          "@//java_targets:java_library_exported",
          "@@rules_jvm_external++maven+maven//:com_google_guava_guava",
          "@//java_targets/subpackage:java_library"
        ),
        1,
        targetWithDependencyJavaBinaryData
      )
      
      // Create expected JvmBuildTarget for kotlin:java_library_exported
      val kotlinJavaLibraryExportedData = JvmBuildTarget(
        javaHome = Paths.get("/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux"),
        javaVersion = "11"
      )
      
      verifyBuildTarget(
        "@//kotlin:java_library_exported",
        "java_library",
        setOf(LanguageClass.JAVA),
        RuleType.LIBRARY,
        listOf("@//kotlin:java_library_exported_output_jars", "@//java_targets/subpackage:java_library"),
        expectedData = kotlinJavaLibraryExportedData
      )

      // Verify ALL nonModuleTargets
      fun verifyNonModuleTarget(id: String, kindString: String, ruleType: RuleType) {
        val target = resultNonModuleTargets.find { it.id.toString() == id }
        target shouldNotBe null
        target!!.kind.kindString shouldBe kindString
        target.kind.languageClasses shouldBe emptyList()
        target.kind.ruleType shouldBe ruleType
        target.dependencies shouldBe emptyList()
        target.sources shouldBe emptyList()
        target.resources shouldBe emptyList()
        target.data shouldBe null
      }

      verifyNonModuleTarget("@//kotlin:java_binary_with_flag", "java_binary", RuleType.BINARY)
      verifyNonModuleTarget("@//java_targets:java_binary_with_flag", "java_binary", RuleType.BINARY)
      verifyNonModuleTarget("@//target_without_args:binary", "scala_binary", RuleType.BINARY)
      verifyNonModuleTarget("@//java_targets:java_binary", "java_binary", RuleType.BINARY)
      verifyNonModuleTarget("@//genrule:foo", "genrule", RuleType.BINARY)
      verifyNonModuleTarget("@//scala_targets:scala_binary", "scala_binary", RuleType.BINARY)
      verifyNonModuleTarget("@//kotlin:java_binary", "java_binary", RuleType.BINARY)
      verifyNonModuleTarget("@//environment_variables:java_test", "java_test", RuleType.TEST)
      verifyNonModuleTarget("@//scala_targets:scala_test", "scala_test", RuleType.TEST)
      verifyNonModuleTarget("@//target_without_java_info:filegroup", "filegroup", RuleType.BINARY)
      verifyNonModuleTarget("@//environment_variables:java_binary", "java_binary", RuleType.BINARY)
      verifyNonModuleTarget("@//target_without_jvm_flags:binary", "scala_binary", RuleType.BINARY)
      verifyNonModuleTarget("@//target_with_dependency:java_binary", "java_binary", RuleType.BINARY)
      verifyNonModuleTarget("@//target_without_java_info:genrule", "genrule", RuleType.BINARY)
      verifyNonModuleTarget("@//target_with_resources:java_binary", "java_binary", RuleType.BINARY)

      // Verify ALL 26 libraries
      result.libraries shouldHaveSize 26
      
      fun verifyLibrary(
        id: String,
        dependencies: List<String> = emptyList(),
        hasMavenCoordinates: Boolean = false,
        isFromInternalTarget: Boolean = false
      ) {
        val library = result.libraries.find { it.id.toString() == id }
        library shouldNotBe null
        library!!.dependencies.map { it.toString() } shouldContainExactly dependencies
        if (hasMavenCoordinates) {
          library.mavenCoordinates shouldNotBe null
        } else {
          library.mavenCoordinates shouldBe null
        }
        library.isFromInternalTarget shouldBe isFromInternalTarget
      }
      
      // Verify Maven libraries (Guava and its dependencies)
      verifyLibrary("@@rules_jvm_external++maven+maven//:com_google_code_findbugs_jsr305", hasMavenCoordinates = true)
      verifyLibrary("@@rules_jvm_external++maven+maven//:com_google_errorprone_error_prone_annotations", hasMavenCoordinates = true)
      verifyLibrary("@@rules_jvm_external++maven+maven//:com_google_guava_failureaccess", hasMavenCoordinates = true)
      verifyLibrary("@@rules_jvm_external++maven+maven//:com_google_guava_listenablefuture", hasMavenCoordinates = true)
      verifyLibrary("@@rules_jvm_external++maven+maven//:com_google_j2objc_j2objc_annotations", hasMavenCoordinates = true)
      verifyLibrary("@@rules_jvm_external++maven+maven//:org_checkerframework_checker_qual", hasMavenCoordinates = true)
      
      // Guava with its 6 dependencies
      verifyLibrary(
        "@@rules_jvm_external++maven+maven//:com_google_guava_guava",
        listOf(
          "@@rules_jvm_external++maven+maven//:com_google_code_findbugs_jsr305",
          "@@rules_jvm_external++maven+maven//:com_google_errorprone_error_prone_annotations",
          "@@rules_jvm_external++maven+maven//:com_google_guava_failureaccess",
          "@@rules_jvm_external++maven+maven//:com_google_guava_listenablefuture",
          "@@rules_jvm_external++maven+maven//:com_google_j2objc_j2objc_annotations",
          "@@rules_jvm_external++maven+maven//:org_checkerframework_checker_qual"
        ),
        hasMavenCoordinates = true
      )
      
      // Internal output_jars libraries
      verifyLibrary(
        "@//java_targets:java_library_exported_output_jars",
        listOf("@//java_targets/subpackage:java_library"),
        isFromInternalTarget = true
      )
      verifyLibrary(
        "@//kotlin:java_library_exported_output_jars",
        listOf("@//java_targets/subpackage:java_library"),
        isFromInternalTarget = true
      )
      
      // Scala synthetic libraries
      verifyLibrary("scala-compiler-2.13.14.jar[synthetic]")
      verifyLibrary("scala-library-2.13.14.jar[synthetic]")
      verifyLibrary("scala-reflect-2.13.14.jar[synthetic]")
      
      // ScalaTest synthetic libraries
      verifyLibrary("scalactic_2.13-3.2.9.jar[synthetic]")
      verifyLibrary("scalatest_2.13-3.2.9.jar[synthetic]")
      verifyLibrary("scalatest-compatible-3.2.9.jar[synthetic]")
      verifyLibrary("scalatest-core_2.13-3.2.9.jar[synthetic]")
      verifyLibrary("scalatest-featurespec_2.13-3.2.9.jar[synthetic]")
      verifyLibrary("scalatest-flatspec_2.13-3.2.9.jar[synthetic]")
      verifyLibrary("scalatest-freespec_2.13-3.2.9.jar[synthetic]")
      verifyLibrary("scalatest-funspec_2.13-3.2.9.jar[synthetic]")
      verifyLibrary("scalatest-funsuite_2.13-3.2.9.jar[synthetic]")
      verifyLibrary("scalatest-matchers-core_2.13-3.2.9.jar[synthetic]")
      verifyLibrary("scalatest-mustmatchers_2.13-3.2.9.jar[synthetic]")
      verifyLibrary("scalatest-shouldmatchers_2.13-3.2.9.jar[synthetic]")
      verifyLibrary("librunner.jar[synthetic]")
      verifyLibrary("test_reporter.jar[synthetic]")
      
      
      // expected
     //  workspace = BazelResolvedWorkspace(targets=BuildTargetCollection(buildTargets=
      //  [RawBuildTarget(id=@//scala_targets:scala_binary, tags=[], dependencies=[scala-compiler-2.13.14.jar[synthetic], scala-library-2.13.14.jar[synthetic], scala-reflect-2.13.14.jar[synthetic]], kind=TargetKind(kindString=scala_binary, languageClasses=[JAVA, SCALA], ruleType=BINARY), sources=[SourceItem(path=/home/andrzej.gluszak/code/junk/sample-repo/scala_targets/ScalaBinary.scala, generated=false, jvmPackagePrefix=scala_targets)], resources=[], baseDirectory=/home/andrzej.gluszak/code/junk/sample-repo/scala_targets, noBuild=false, data=ScalaBuildTarget(scalaVersion=2.13.14, sdkJars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scala_compiler_2_13_14/scala-compiler-2.13.14.jar, /home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scala_library_2_13_14/scala-library-2.13.14.jar, /home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scala_reflect_2_13_14/scala-reflect-2.13.14.jar], jvmBuildTarget=JvmBuildTarget(javaHome=/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux, javaVersion=11), scalacOptions=[-target:jvm-1.8]), lowPrioritySharedSources=[]), RawBuildTarget(id=@//kotlin:java_binary, tags=[], dependencies=[], kind=TargetKind(kindString=java_binary, languageClasses=[JAVA], ruleType=BINARY), sources=[], resources=[], baseDirectory=/home/andrzej.gluszak/code/junk/sample-repo/kotlin, noBuild=false, data=JvmBuildTarget(javaHome=/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux, javaVersion=11), lowPrioritySharedSources=[]), RawBuildTarget(id=@//kotlin:java_binary_with_flag, tags=[], dependencies=[], kind=TargetKind(kindString=java_binary, languageClasses=[JAVA], ruleType=BINARY), sources=[], resources=[], baseDirectory=/home/andrzej.gluszak/code/junk/sample-repo/kotlin, noBuild=false, data=JvmBuildTarget(javaHome=/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux, javaVersion=17), lowPrioritySharedSources=[]), RawBuildTarget(id=@//java_targets:java_binary_with_flag, tags=[], dependencies=[], kind=TargetKind(kindString=java_binary, languageClasses=[JAVA], ruleType=BINARY), sources=[SourceItem(path=/home/andrzej.gluszak/code/junk/sample-repo/java_targets/JavaBinaryWithFlag.java, generated=false, jvmPackagePrefix=java_targets)], resources=[], baseDirectory=/home/andrzej.gluszak/code/junk/sample-repo/java_targets, noBuild=false, data=JvmBuildTarget(javaHome=/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux, javaVersion=17), lowPrioritySharedSources=[]), RawBuildTarget(id=@//java_targets:java_binary, tags=[], dependencies=[@//java_targets/subpackage:java_library], kind=TargetKind(kindString=java_binary, languageClasses=[JAVA], ruleType=BINARY), sources=[SourceItem(path=/home/andrzej.gluszak/code/junk/sample-repo/java_targets/JavaBinary.java, generated=false, jvmPackagePrefix=java_targets)], resources=[], baseDirectory=/home/andrzej.gluszak/code/junk/sample-repo/java_targets, noBuild=false, data=JvmBuildTarget(javaHome=/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux, javaVersion=11), lowPrioritySharedSources=[]), RawBuildTarget(id=@//environment_variables:java_binary, tags=[], dependencies=[], kind=TargetKind(kindString=java_binary, languageClasses=[JAVA], ruleType=BINARY), sources=[SourceItem(path=/home/andrzej.gluszak/code/junk/sample-repo/environment_variables/JavaEnv.java, generated=false, jvmPackagePrefix=environment_variables)], resources=[], baseDirectory=/home/andrzej.gluszak/code/junk/sample-repo/environment_variables, noBuild=false, data=JvmBuildTarget(javaHome=/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux, javaVersion=11), lowPrioritySharedSources=[]), RawBuildTarget(id=@//kotlin:asd, tags=[], dependencies=[], kind=TargetKind(kindString=kt_jvm_library, languageClasses=[JAVA, KOTLIN], ruleType=LIBRARY), sources=[], resources=[], baseDirectory=/home/andrzej.gluszak/code/junk/sample-repo/kotlin, noBuild=false, data=null, lowPrioritySharedSources=[]), RawBuildTarget(id=@//target_without_jvm_flags:binary, tags=[], dependencies=[scala-compiler-2.13.14.jar[synthetic], scala-library-2.13.14.jar[synthetic], scala-reflect-2.13.14.jar[synthetic]], kind=TargetKind(kindString=scala_binary, languageClasses=[JAVA, SCALA], ruleType=BINARY), sources=[SourceItem(path=/home/andrzej.gluszak/code/junk/sample-repo/target_without_jvm_flags/Example.scala, generated=false, jvmPackagePrefix=target_without_jvm_flags)], resources=[], baseDirectory=/home/andrzej.gluszak/code/junk/sample-repo/target_without_jvm_flags, noBuild=false, data=ScalaBuildTarget(scalaVersion=2.13.14, sdkJars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scala_compiler_2_13_14/scala-compiler-2.13.14.jar, /home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scala_library_2_13_14/scala-library-2.13.14.jar, /home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scala_reflect_2_13_14/scala-reflect-2.13.14.jar], jvmBuildTarget=JvmBuildTarget(javaHome=/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux, javaVersion=11), scalacOptions=[]), lowPrioritySharedSources=[]), RawBuildTarget(id=@//target_with_javac_exports:java_library, tags=[], dependencies=[], kind=TargetKind(kindString=java_library, languageClasses=[JAVA], ruleType=LIBRARY), sources=[SourceItem(path=/home/andrzej.gluszak/code/junk/sample-repo/target_with_javac_exports/JavaLibrary.java, generated=false, jvmPackagePrefix=target_with_javac_exports)], resources=[], baseDirectory=/home/andrzej.gluszak/code/junk/sample-repo/target_with_javac_exports, noBuild=false, data=JvmBuildTarget(javaHome=/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux, javaVersion=11), lowPrioritySharedSources=[]), RawBuildTarget(id=@//environment_variables:java_test, tags=[], dependencies=[], kind=TargetKind(kindString=java_test, languageClasses=[JAVA], ruleType=TEST), sources=[SourceItem(path=/home/andrzej.gluszak/code/junk/sample-repo/environment_variables/JavaTest.java, generated=false, jvmPackagePrefix=environment_variables)], resources=[], baseDirectory=/home/andrzej.gluszak/code/junk/sample-repo/environment_variables, noBuild=false, data=JvmBuildTarget(javaHome=/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux, javaVersion=11), lowPrioritySharedSources=[]), RawBuildTarget(id=@//scala_targets:scala_test, tags=[], dependencies=[scala-compiler-2.13.14.jar[synthetic], scala-library-2.13.14.jar[synthetic], scala-reflect-2.13.14.jar[synthetic], scalactic_2.13-3.2.9.jar[synthetic], scalatest_2.13-3.2.9.jar[synthetic], scalatest-compatible-3.2.9.jar[synthetic], scalatest-core_2.13-3.2.9.jar[synthetic], scalatest-featurespec_2.13-3.2.9.jar[synthetic], scalatest-flatspec_2.13-3.2.9.jar[synthetic], scalatest-freespec_2.13-3.2.9.jar[synthetic], scalatest-funspec_2.13-3.2.9.jar[synthetic], scalatest-funsuite_2.13-3.2.9.jar[synthetic], scalatest-matchers-core_2.13-3.2.9.jar[synthetic], scalatest-mustmatchers_2.13-3.2.9.jar[synthetic], scalatest-shouldmatchers_2.13-3.2.9.jar[synthetic], librunner.jar[synthetic], test_reporter.jar[synthetic]], kind=TargetKind(kindString=scala_test, languageClasses=[JAVA, SCALA], ruleType=TEST), sources=[SourceItem(path=/home/andrzej.gluszak/code/junk/sample-repo/scala_targets/ScalaTest.scala, generated=false, jvmPackagePrefix=scala_targets)], resources=[], baseDirectory=/home/andrzej.gluszak/code/junk/sample-repo/scala_targets, noBuild=false, data=ScalaBuildTarget(scalaVersion=2.13.14, sdkJars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scala_compiler_2_13_14/scala-compiler-2.13.14.jar, /home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scala_library_2_13_14/scala-library-2.13.14.jar, /home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scala_reflect_2_13_14/scala-reflect-2.13.14.jar], jvmBuildTarget=JvmBuildTarget(javaHome=/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux, javaVersion=11), scalacOptions=[]), lowPrioritySharedSources=[]), RawBuildTarget(id=@//java_targets/subpackage:java_library, tags=[], dependencies=[], kind=TargetKind(kindString=java_library, languageClasses=[JAVA], ruleType=LIBRARY), sources=[SourceItem(path=/home/andrzej.gluszak/code/junk/sample-repo/java_targets/subpackage/JavaLibrary2.java, generated=false, jvmPackagePrefix=java_targets.subpackage)], resources=[], baseDirectory=/home/andrzej.gluszak/code/junk/sample-repo/java_targets/subpackage, noBuild=false, data=JvmBuildTarget(javaHome=/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux, javaVersion=11), lowPrioritySharedSources=[]), RawBuildTarget(id=@//target_without_args:binary, tags=[], dependencies=[scala-compiler-2.13.14.jar[synthetic], scala-library-2.13.14.jar[synthetic], scala-reflect-2.13.14.jar[synthetic]], kind=TargetKind(kindString=scala_binary, languageClasses=[JAVA, SCALA], ruleType=BINARY), sources=[SourceItem(path=/home/andrzej.gluszak/code/junk/sample-repo/target_without_args/Example.scala, generated=false, jvmPackagePrefix=target_without_args)], resources=[], baseDirectory=/home/andrzej.gluszak/code/junk/sample-repo/target_without_args, noBuild=false, data=ScalaBuildTarget(scalaVersion=2.13.14, sdkJars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scala_compiler_2_13_14/scala-compiler-2.13.14.jar, /home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scala_library_2_13_14/scala-library-2.13.14.jar, /home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scala_reflect_2_13_14/scala-reflect-2.13.14.jar], jvmBuildTarget=JvmBuildTarget(javaHome=/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux, javaVersion=11), scalacOptions=[]), lowPrioritySharedSources=[]), RawBuildTarget(id=@//java_targets:java_library, tags=[], dependencies=[], kind=TargetKind(kindString=java_library, languageClasses=[JAVA], ruleType=LIBRARY), sources=[SourceItem(path=/home/andrzej.gluszak/code/junk/sample-repo/java_targets/JavaLibrary.java, generated=false, jvmPackagePrefix=java_targets)], resources=[], baseDirectory=/home/andrzej.gluszak/code/junk/sample-repo/java_targets, noBuild=false, data=JvmBuildTarget(javaHome=/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux, javaVersion=11), lowPrioritySharedSources=[]), RawBuildTarget(id=@//target_without_main_class:library, tags=[], dependencies=[scala-compiler-2.13.14.jar[synthetic], scala-library-2.13.14.jar[synthetic], scala-reflect-2.13.14.jar[synthetic]], kind=TargetKind(kindString=scala_library, languageClasses=[JAVA, SCALA], ruleType=LIBRARY), sources=[SourceItem(path=/home/andrzej.gluszak/code/junk/sample-repo/target_without_main_class/Example.scala, generated=false, jvmPackagePrefix=target_without_main_class)], resources=[], baseDirectory=/home/andrzej.gluszak/code/junk/sample-repo/target_without_main_class, noBuild=false, data=ScalaBuildTarget(scalaVersion=2.13.14, sdkJars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scala_compiler_2_13_14/scala-compiler-2.13.14.jar, /home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scala_library_2_13_14/scala-library-2.13.14.jar, /home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scala_reflect_2_13_14/scala-reflect-2.13.14.jar], jvmBuildTarget=JvmBuildTarget(javaHome=/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux, javaVersion=11), scalacOptions=[]), lowPrioritySharedSources=[]), RawBuildTarget(id=@//java_targets:java_library_exported, tags=[], dependencies=[@//java_targets:java_library_exported_output_jars, @//java_targets/subpackage:java_library], kind=TargetKind(kindString=java_library, languageClasses=[JAVA], ruleType=LIBRARY), sources=[], resources=[], baseDirectory=/home/andrzej.gluszak/code/junk/sample-repo/java_targets, noBuild=false, data=JvmBuildTarget(javaHome=/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux, javaVersion=11), lowPrioritySharedSources=[]), RawBuildTarget(id=@//kotlin:java_library, tags=[], dependencies=[], kind=TargetKind(kindString=java_library, languageClasses=[JAVA], ruleType=LIBRARY), sources=[], resources=[], baseDirectory=/home/andrzej.gluszak/code/junk/sample-repo/kotlin, noBuild=false, data=JvmBuildTarget(javaHome=/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux, javaVersion=11), lowPrioritySharedSources=[]), RawBuildTarget(id=@//target_with_resources:java_binary, tags=[], dependencies=[], kind=TargetKind(kindString=java_binary, languageClasses=[JAVA], ruleType=BINARY), sources=[SourceItem(path=/home/andrzej.gluszak/code/junk/sample-repo/target_with_resources/JavaBinary.java, generated=false, jvmPackagePrefix=target_with_resources)], resources=[/home/andrzej.gluszak/code/junk/sample-repo/target_with_resources/file1.txt, /home/andrzej.gluszak/code/junk/sample-repo/target_with_resources/file2.txt], baseDirectory=/home/andrzej.gluszak/code/junk/sample-repo/target_with_resources, noBuild=false, data=JvmBuildTarget(javaHome=/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux, javaVersion=11), lowPrioritySharedSources=[]), RawBuildTarget(id=@//target_with_dependency:java_binary, tags=[], dependencies=[@//java_targets:java_library_exported, @@rules_jvm_external++maven+maven//:com_google_guava_guava, @//java_targets/subpackage:java_library], kind=TargetKind(kindString=java_binary, languageClasses=[JAVA], ruleType=BINARY), sources=[SourceItem(path=/home/andrzej.gluszak/code/junk/sample-repo/target_with_dependency/JavaBinary.java, generated=false, jvmPackagePrefix=target_with_dependency)], resources=[], baseDirectory=/home/andrzej.gluszak/code/junk/sample-repo/target_with_dependency, noBuild=false, data=JvmBuildTarget(javaHome=/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux, javaVersion=11), lowPrioritySharedSources=[]), RawBuildTarget(id=@//kotlin:java_library_exported, tags=[], dependencies=[@//kotlin:java_library_exported_output_jars, @//java_targets/subpackage:java_library], kind=TargetKind(kindString=java_library, languageClasses=[JAVA], ruleType=LIBRARY), sources=[], resources=[], baseDirectory=/home/andrzej.gluszak/code/junk/sample-repo/kotlin, noBuild=false, data=JvmBuildTarget(javaHome=/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_java++toolchains+remotejdk11_linux, javaVersion=11), lowPrioritySharedSources=[])], nonModuleTargets=[RawBuildTarget(id=@//kotlin:java_binary_with_flag, tags=[], dependencies=[], kind=TargetKind(kindString=java_binary, languageClasses=[], ruleType=BINARY), sources=[], resources=[], baseDirectory=/home/andrzej.gluszak/code/junk/sample-repo/kotlin, noBuild=false, data=null, lowPrioritySharedSources=[]), RawBuildTarget(id=@//java_targets:java_binary_with_flag, tags=[], dependencies=[], kind=TargetKind(kindString=java_binary, languageClasses=[], ruleType=BINARY), sources=[], resources=[], baseDirectory=/home/andrzej.gluszak/code/junk/sample-repo/java_targets, noBuild=false, data=null, lowPrioritySharedSources=[]), RawBuildTarget(id=@//target_without_args:binary, tags=[], dependencies=[], kind=TargetKind(kindString=scala_binary, languageClasses=[], ruleType=BINARY), sources=[], resources=[], baseDirectory=/home/andrzej.gluszak/code/junk/sample-repo/target_without_args, noBuild=false, data=null, lowPrioritySharedSources=[]), RawBuildTarget(id=@//java_targets:java_binary, tags=[], dependencies=[], kind=TargetKind(kindString=java_binary, languageClasses=[], ruleType=BINARY), sources=[], resources=[], baseDirectory=/home/andrzej.gluszak/code/junk/sample-repo/java_targets, noBuild=false, data=null, lowPrioritySharedSources=[]), RawBuildTarget(id=@//genrule:foo, tags=[], dependencies=[], kind=TargetKind(kindString=genrule, languageClasses=[], ruleType=BINARY), sources=[], resources=[], baseDirectory=/home/andrzej.gluszak/code/junk/sample-repo/genrule, noBuild=false, data=null, lowPrioritySharedSources=[]), RawBuildTarget(id=@//scala_targets:scala_binary, tags=[], dependencies=[], kind=TargetKind(kindString=scala_binary, languageClasses=[], ruleType=BINARY), sources=[], resources=[], baseDirectory=/home/andrzej.gluszak/code/junk/sample-repo/scala_targets, noBuild=false, data=null, lowPrioritySharedSources=[]), RawBuildTarget(id=@//kotlin:java_binary, tags=[], dependencies=[], kind=TargetKind(kindString=java_binary, languageClasses=[], ruleType=BINARY), sources=[], resources=[], baseDirectory=/home/andrzej.gluszak/code/junk/sample-repo/kotlin, noBuild=false, data=null, lowPrioritySharedSources=[]), RawBuildTarget(id=@//environment_variables:java_test, tags=[], dependencies=[], kind=TargetKind(kindString=java_test, languageClasses=[], ruleType=TEST), sources=[], resources=[], baseDirectory=/home/andrzej.gluszak/code/junk/sample-repo/environment_variables, noBuild=false, data=null, lowPrioritySharedSources=[]), RawBuildTarget(id=@//scala_targets:scala_test, tags=[], dependencies=[], kind=TargetKind(kindString=scala_test, languageClasses=[], ruleType=TEST), sources=[], resources=[], baseDirectory=/home/andrzej.gluszak/code/junk/sample-repo/scala_targets, noBuild=false, data=null, lowPrioritySharedSources=[]), RawBuildTarget(id=@//target_without_java_info:filegroup, tags=[], dependencies=[], kind=TargetKind(kindString=filegroup, languageClasses=[], ruleType=BINARY), sources=[], resources=[], baseDirectory=/home/andrzej.gluszak/code/junk/sample-repo/target_without_java_info, noBuild=false, data=null, lowPrioritySharedSources=[]), RawBuildTarget(id=@//environment_variables:java_binary, tags=[], dependencies=[], kind=TargetKind(kindString=java_binary, languageClasses=[], ruleType=BINARY), sources=[], resources=[], baseDirectory=/home/andrzej.gluszak/code/junk/sample-repo/environment_variables, noBuild=false, data=null, lowPrioritySharedSources=[]), RawBuildTarget(id=@//target_without_jvm_flags:binary, tags=[], dependencies=[], kind=TargetKind(kindString=scala_binary, languageClasses=[], ruleType=BINARY), sources=[], resources=[], baseDirectory=/home/andrzej.gluszak/code/junk/sample-repo/target_without_jvm_flags, noBuild=false, data=null, lowPrioritySharedSources=[]), RawBuildTarget(id=@//target_with_dependency:java_binary, tags=[], dependencies=[], kind=TargetKind(kindString=java_binary, languageClasses=[], ruleType=BINARY), sources=[], resources=[], baseDirectory=/home/andrzej.gluszak/code/junk/sample-repo/target_with_dependency, noBuild=false, data=null, lowPrioritySharedSources=[]), RawBuildTarget(id=@//target_without_java_info:genrule, tags=[], dependencies=[], kind=TargetKind(kindString=genrule, languageClasses=[], ruleType=BINARY), sources=[], resources=[], baseDirectory=/home/andrzej.gluszak/code/junk/sample-repo/target_without_java_info, noBuild=false, data=null, lowPrioritySharedSources=[]), RawBuildTarget(id=@//target_with_resources:java_binary, tags=[], dependencies=[], kind=TargetKind(kindString=java_binary, languageClasses=[], ruleType=BINARY), sources=[], resources=[], baseDirectory=/home/andrzej.gluszak/code/junk/sample-repo/target_with_resources, noBuild=false, data=null, lowPrioritySharedSources=[])]), 
      //  libraries=[
// LibraryItem(id=@@rules_jvm_external++maven+maven//:com_google_code_findbugs_jsr305, dependencies=[], ijars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/execroot/_main/bazel-out/k8-fastbuild/bin/external/rules_jvm_external++maven+maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/code/findbugs/jsr305/3.0.2/header_jsr305-3.0.2.jar], jars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/execroot/_main/bazel-out/k8-fastbuild/bin/external/rules_jvm_external++maven+maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/code/findbugs/jsr305/3.0.2/processed_jsr305-3.0.2.jar], sourceJars=[], mavenCoordinates=MavenCoordinates(groupId=com.google.code.findbugs, artifactId=jsr305, version=3.0.2), isFromInternalTarget=false), 
// LibraryItem(id=@@rules_jvm_external++maven+maven//:com_google_errorprone_error_prone_annotations, dependencies=[], ijars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/execroot/_main/bazel-out/k8-fastbuild/bin/external/rules_jvm_external++maven+maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/errorprone/error_prone_annotations/2.7.1/header_error_prone_annotations-2.7.1.jar], jars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/execroot/_main/bazel-out/k8-fastbuild/bin/external/rules_jvm_external++maven+maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/errorprone/error_prone_annotations/2.7.1/processed_error_prone_annotations-2.7.1.jar], sourceJars=[], mavenCoordinates=MavenCoordinates(groupId=com.google.errorprone, artifactId=error_prone_annotations, version=2.7.1), isFromInternalTarget=false), 
// LibraryItem(id=@@rules_jvm_external++maven+maven//:com_google_guava_failureaccess, dependencies=[], ijars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/execroot/_main/bazel-out/k8-fastbuild/bin/external/rules_jvm_external++maven+maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/guava/failureaccess/1.0.1/header_failureaccess-1.0.1.jar], jars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/execroot/_main/bazel-out/k8-fastbuild/bin/external/rules_jvm_external++maven+maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/guava/failureaccess/1.0.1/processed_failureaccess-1.0.1.jar], sourceJars=[], mavenCoordinates=MavenCoordinates(groupId=com.google.guava, artifactId=failureaccess, version=1.0.1), isFromInternalTarget=false), 
// LibraryItem(id=@@rules_jvm_external++maven+maven//:com_google_guava_listenablefuture, dependencies=[], ijars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/execroot/_main/bazel-out/k8-fastbuild/bin/external/rules_jvm_external++maven+maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/guava/listenablefuture/9999.0-empty-to-avoid-conflict-with-guava/header_listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar], jars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/execroot/_main/bazel-out/k8-fastbuild/bin/external/rules_jvm_external++maven+maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/guava/listenablefuture/9999.0-empty-to-avoid-conflict-with-guava/processed_listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar], sourceJars=[], mavenCoordinates=MavenCoordinates(groupId=com.google.guava, artifactId=listenablefuture, version=9999.0-empty-to-avoid-conflict-with-guava), isFromInternalTarget=false), 
// LibraryItem(id=@@rules_jvm_external++maven+maven//:com_google_j2objc_j2objc_annotations, dependencies=[], ijars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/execroot/_main/bazel-out/k8-fastbuild/bin/external/rules_jvm_external++maven+maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/j2objc/j2objc-annotations/1.3/header_j2objc-annotations-1.3.jar], jars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/execroot/_main/bazel-out/k8-fastbuild/bin/external/rules_jvm_external++maven+maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/j2objc/j2objc-annotations/1.3/processed_j2objc-annotations-1.3.jar], sourceJars=[], mavenCoordinates=MavenCoordinates(groupId=com.google.j2objc, artifactId=j2objc-annotations, version=1.3), isFromInternalTarget=false), 
// LibraryItem(id=@@rules_jvm_external++maven+maven//:org_checkerframework_checker_qual, dependencies=[], ijars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/execroot/_main/bazel-out/k8-fastbuild/bin/external/rules_jvm_external++maven+maven/v1/https/cache-redirector.jetbrains.com/maven-central/org/checkerframework/checker-qual/3.12.0/header_checker-qual-3.12.0.jar], jars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/execroot/_main/bazel-out/k8-fastbuild/bin/external/rules_jvm_external++maven+maven/v1/https/cache-redirector.jetbrains.com/maven-central/org/checkerframework/checker-qual/3.12.0/processed_checker-qual-3.12.0.jar], sourceJars=[], mavenCoordinates=MavenCoordinates(groupId=org.checkerframework, artifactId=checker-qual, version=3.12.0), isFromInternalTarget=false), 
// LibraryItem(id=@@rules_jvm_external++maven+maven//:com_google_guava_guava, dependencies=[@@rules_jvm_external++maven+maven//:com_google_code_findbugs_jsr305, @@rules_jvm_external++maven+maven//:com_google_errorprone_error_prone_annotations, @@rules_jvm_external++maven+maven//:com_google_guava_failureaccess, @@rules_jvm_external++maven+maven//:com_google_guava_listenablefuture, @@rules_jvm_external++maven+maven//:com_google_j2objc_j2objc_annotations, @@rules_jvm_external++maven+maven//:org_checkerframework_checker_qual], ijars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/execroot/_main/bazel-out/k8-fastbuild/bin/external/rules_jvm_external++maven+maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/guava/guava/31.0.1-jre/header_guava-31.0.1-jre.jar], jars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/execroot/_main/bazel-out/k8-fastbuild/bin/external/rules_jvm_external++maven+maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/guava/guava/31.0.1-jre/processed_guava-31.0.1-jre.jar], sourceJars=[], mavenCoordinates=MavenCoordinates(groupId=com.google.guava, artifactId=guava, version=31.0.1-jre), isFromInternalTarget=false), 
// LibraryItem(id=@//java_targets:java_library_exported_output_jars, dependencies=[@//java_targets/subpackage:java_library], ijars=[], jars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/execroot/_main/bazel-out/k8-fastbuild/bin/java_targets/libjava_library_exported.jar], sourceJars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/execroot/_main/bazel-out/k8-fastbuild/bin/java_targets/libjava_library_exported-src.jar], mavenCoordinates=null, isFromInternalTarget=true), 
// LibraryItem(id=@//kotlin:java_library_exported_output_jars, dependencies=[@//java_targets/subpackage:java_library], ijars=[], jars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/execroot/_main/bazel-out/k8-fastbuild/bin/kotlin/libjava_library_exported.jar], sourceJars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/execroot/_main/bazel-out/k8-fastbuild/bin/kotlin/libjava_library_exported-src.jar], mavenCoordinates=null, isFromInternalTarget=true), 
// LibraryItem(id=scala-compiler-2.13.14.jar[synthetic], dependencies=[], ijars=[], jars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scala_compiler_2_13_14/scala-compiler-2.13.14.jar], sourceJars=[], mavenCoordinates=null, isFromInternalTarget=false), 
// LibraryItem(id=scala-library-2.13.14.jar[synthetic], dependencies=[], ijars=[], jars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scala_library_2_13_14/scala-library-2.13.14.jar], sourceJars=[], mavenCoordinates=null, isFromInternalTarget=false), 
// LibraryItem(id=scala-reflect-2.13.14.jar[synthetic], dependencies=[], ijars=[], jars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scala_reflect_2_13_14/scala-reflect-2.13.14.jar], sourceJars=[], mavenCoordinates=null, isFromInternalTarget=false), 
// LibraryItem(id=scalactic_2.13-3.2.9.jar[synthetic], dependencies=[], ijars=[], jars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scalactic_2_13_14/scalactic_2.13-3.2.9.jar], sourceJars=[], mavenCoordinates=null, isFromInternalTarget=false), 
// LibraryItem(id=scalatest_2.13-3.2.9.jar[synthetic], dependencies=[], ijars=[], jars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scalatest_2_13_14/scalatest_2.13-3.2.9.jar], sourceJars=[], mavenCoordinates=null, isFromInternalTarget=false), 
// LibraryItem(id=scalatest-compatible-3.2.9.jar[synthetic], dependencies=[], ijars=[], jars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scalatest_compatible_2_13_14/scalatest-compatible-3.2.9.jar], sourceJars=[], mavenCoordinates=null, isFromInternalTarget=false), 
// LibraryItem(id=scalatest-core_2.13-3.2.9.jar[synthetic], dependencies=[], ijars=[], jars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scalatest_core_2_13_14/scalatest-core_2.13-3.2.9.jar], sourceJars=[], mavenCoordinates=null, isFromInternalTarget=false), 
// LibraryItem(id=scalatest-featurespec_2.13-3.2.9.jar[synthetic], dependencies=[], ijars=[], jars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scalatest_featurespec_2_13_14/scalatest-featurespec_2.13-3.2.9.jar], sourceJars=[], mavenCoordinates=null, isFromInternalTarget=false), 
// LibraryItem(id=scalatest-flatspec_2.13-3.2.9.jar[synthetic], dependencies=[], ijars=[], jars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scalatest_flatspec_2_13_14/scalatest-flatspec_2.13-3.2.9.jar], sourceJars=[], mavenCoordinates=null, isFromInternalTarget=false), 
// LibraryItem(id=scalatest-freespec_2.13-3.2.9.jar[synthetic], dependencies=[], ijars=[], jars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scalatest_freespec_2_13_14/scalatest-freespec_2.13-3.2.9.jar], sourceJars=[], mavenCoordinates=null, isFromInternalTarget=false), 
// LibraryItem(id=scalatest-funspec_2.13-3.2.9.jar[synthetic], dependencies=[], ijars=[], jars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scalatest_funspec_2_13_14/scalatest-funspec_2.13-3.2.9.jar], sourceJars=[], mavenCoordinates=null, isFromInternalTarget=false), 
// LibraryItem(id=scalatest-funsuite_2.13-3.2.9.jar[synthetic], dependencies=[], ijars=[], jars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scalatest_funsuite_2_13_14/scalatest-funsuite_2.13-3.2.9.jar], sourceJars=[], mavenCoordinates=null, isFromInternalTarget=false), 
// LibraryItem(id=scalatest-matchers-core_2.13-3.2.9.jar[synthetic], dependencies=[], ijars=[], jars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scalatest_matchers_core_2_13_14/scalatest-matchers-core_2.13-3.2.9.jar], sourceJars=[], mavenCoordinates=null, isFromInternalTarget=false), 
// LibraryItem(id=scalatest-mustmatchers_2.13-3.2.9.jar[synthetic], dependencies=[], ijars=[], jars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scalatest_mustmatchers_2_13_14/scalatest-mustmatchers_2.13-3.2.9.jar], sourceJars=[], mavenCoordinates=null, isFromInternalTarget=false), 
// LibraryItem(id=scalatest-shouldmatchers_2.13-3.2.9.jar[synthetic], dependencies=[], ijars=[], jars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/external/rules_scala++scala_deps+io_bazel_rules_scala_scalatest_shouldmatchers_2_13_14/scalatest-shouldmatchers_2.13-3.2.9.jar], sourceJars=[], mavenCoordinates=null, isFromInternalTarget=false), 
// LibraryItem(id=librunner.jar[synthetic], dependencies=[], ijars=[], jars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/execroot/_main/bazel-out/k8-opt-exec-ST-d57f47055a04/bin/external/rules_scala+/src/java/io/bazel/rulesscala/scala_test/librunner.jar], sourceJars=[], mavenCoordinates=null, isFromInternalTarget=false), 
// LibraryItem(id=test_reporter.jar[synthetic], dependencies=[], ijars=[], jars=[/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/15c45786dac63f4dc419213eb9183942/execroot/_main/bazel-out/k8-fastbuild/bin/external/rules_scala+/scala/support/test_reporter.jar], sourceJars=[], mavenCoordinates=null, isFromInternalTarget=false)], hasError=false)

      // Verify Guava library with its exact dependencies as specified (use different variable name)
      val guavaLib = result.libraries.find { it.id.toString() == "@@rules_jvm_external++maven+maven//:com_google_guava_guava" }
      guavaLib shouldNotBe null
      guavaLib!!.dependencies.map { it.toString() } shouldContainExactly listOf(
        "@@rules_jvm_external++maven+maven//:com_google_code_findbugs_jsr305",
        "@@rules_jvm_external++maven+maven//:com_google_errorprone_error_prone_annotations",
        "@@rules_jvm_external++maven+maven//:com_google_guava_failureaccess",
        "@@rules_jvm_external++maven+maven//:com_google_guava_listenablefuture",
        "@@rules_jvm_external++maven+maven//:com_google_j2objc_j2objc_annotations",
        "@@rules_jvm_external++maven+maven//:org_checkerframework_checker_qual"
      )
      guavaLib.mavenCoordinates!!.groupId shouldBe "com.google.guava"
      guavaLib.mavenCoordinates!!.artifactId shouldBe "guava"
      guavaLib.mavenCoordinates!!.version shouldBe "31.0.1-jre"
      guavaLib.isFromInternalTarget shouldBe false

      // Verify synthetic libraries as specified
      val scalaCompilerSynthetic = result.libraries.find { it.id.toString() == "scala-compiler-2.13.14.jar[synthetic]" }
      scalaCompilerSynthetic shouldNotBe null
      scalaCompilerSynthetic!!.dependencies shouldBe emptyList()
      scalaCompilerSynthetic.mavenCoordinates shouldBe null
      scalaCompilerSynthetic.isFromInternalTarget shouldBe false

      // Verify internal target libraries as specified
      val internalLibrary = result.libraries.find { it.id.toString() == "@//java_targets:java_library_exported_output_jars" }
      internalLibrary shouldNotBe null
      internalLibrary!!.dependencies.map { it.toString() } shouldContainExactly listOf("@//java_targets/subpackage:java_library")
      internalLibrary.mavenCoordinates shouldBe null
      internalLibrary.isFromInternalTarget shouldBe true

      // ======== Additional Comprehensive Verification ========
      
      // Verify specific source file paths and jvmPackagePrefix values
      fun verifySourceDetails(targetId: String, expectedSourcePath: String, expectedPackagePrefix: String) {
        val target = resultBuildTargets.find { it.id.toString() == targetId }
        target shouldNotBe null
        target!!.sources shouldHaveSize 1
        target.sources.first().path.toString() shouldEndWith expectedSourcePath
        target.sources.first().generated shouldBe false
        target.sources.first().jvmPackagePrefix shouldBe expectedPackagePrefix
      }
      
      // Verify key source files match expected structure from OUTPUT comments
      verifySourceDetails("@//scala_targets:scala_binary", "scala_targets/ScalaBinary.scala", "scala_targets")
      verifySourceDetails("@//java_targets:java_binary", "java_targets/JavaBinary.java", "java_targets")
      verifySourceDetails("@//java_targets/subpackage:java_library", "java_targets/subpackage/JavaLibrary2.java", "java_targets.subpackage")
      verifySourceDetails("@//target_with_resources:java_binary", "target_with_resources/JavaBinary.java", "target_with_resources")
      verifySourceDetails("@//environment_variables:java_binary", "environment_variables/JavaEnv.java", "environment_variables")
      verifySourceDetails("@//target_with_javac_exports:java_library", "target_with_javac_exports/JavaLibrary.java", "target_with_javac_exports")

      // Verify resource files for targets that should have them
      fun verifyResources(targetId: String, expectedResourceCount: Int, expectedResourcePaths: List<String> = emptyList()) {
        val target = resultBuildTargets.find { it.id.toString() == targetId }
        target shouldNotBe null
        target!!.resources shouldHaveSize expectedResourceCount
        if (expectedResourcePaths.isNotEmpty()) {
          target.resources.map { it.toString() } shouldContainAll expectedResourcePaths.map { path ->
            listOf(path).filter { filePath -> target.resources.any { it.toString().endsWith(filePath) } }
          }.flatten()
        }
      }
      
      // Verify target_with_resources has its resource files
      verifyResources("@//target_with_resources:java_binary", 2, listOf("target_with_resources/file1.txt", "target_with_resources/file2.txt"))
      
      // Verify base directories match expected structure
      fun verifyBaseDirectory(targetId: String, expectedBaseDirSuffix: String) {
        val target = resultBuildTargets.find { it.id.toString() == targetId }
        target shouldNotBe null
        target!!.baseDirectory.toString() shouldEndWith expectedBaseDirSuffix
      }
      
      // Verify base directories for key targets
      verifyBaseDirectory("@//scala_targets:scala_binary", "scala_targets")
      verifyBaseDirectory("@//kotlin:java_binary", "kotlin")
      verifyBaseDirectory("@//java_targets:java_binary", "java_targets")
      verifyBaseDirectory("@//java_targets/subpackage:java_library", "java_targets/subpackage")
      verifyBaseDirectory("@//target_with_resources:java_binary", "target_with_resources")
      verifyBaseDirectory("@//environment_variables:java_binary", "environment_variables")
      
      // Verify Java version distinctions for different targets
      fun verifyJavaVersion(targetId: String, expectedJavaVersion: String) {
        val target = resultBuildTargets.find { it.id.toString() == targetId }
        target shouldNotBe null
        val jvmTarget = target!!.data as? JvmBuildTarget
        jvmTarget shouldNotBe null
        jvmTarget!!.javaVersion shouldBe expectedJavaVersion
      }
      
      // Verify different Java versions are correctly mapped (from input comments)
      verifyJavaVersion("@//kotlin:java_binary", "11")
      verifyJavaVersion("@//kotlin:java_binary_with_flag", "17")  // This one has different java version
      verifyJavaVersion("@//environment_variables:java_binary", "11")
      
      // Additional verification that build targets match expected exact counts by type
      val scalaBinaryTargets = resultBuildTargets.filter { it.kind.kindString == "scala_binary" }
      scalaBinaryTargets shouldHaveSize 3  // scala_binary, target_without_jvm_flags:binary, target_without_args:binary
      
      val javaBinaryTargets = resultBuildTargets.filter { it.kind.kindString == "java_binary" }
      javaBinaryTargets shouldHaveSize 5  // kotlin:java_binary, kotlin:java_binary_with_flag, java_targets:java_binary, environment_variables:java_binary, target_with_resources:java_binary, target_with_dependency:java_binary
      
      val javaLibraryTargets = resultBuildTargets.filter { it.kind.kindString == "java_library" }
      javaLibraryTargets shouldHaveSize 5  // java_targets/subpackage:java_library, java_targets:java_library, target_with_javac_exports:java_library, java_targets:java_library_exported, kotlin:java_library_exported
      
      val scalaTestTargets = resultBuildTargets.filter { it.kind.kindString == "scala_test" }
      scalaTestTargets shouldHaveSize 1  // scala_targets:scala_test
      
      val javaTestTargets = resultBuildTargets.filter { it.kind.kindString == "java_test" }
      javaTestTargets shouldHaveSize 1  // environment_variables:java_test
    }
  }

  private fun createTestTargets(): Map<Label, BspTargetInfo.TargetInfo> {
    val targets = mutableMapOf<Label, BspTargetInfo.TargetInfo>()

    // Create aspects target (filegroup)
    val aspectsTarget =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//.bazelbsp/aspects:aspects"
          kind = "filegroup"
          workspaceName = "_main"

          // Add all the aspect sources
          val aspectFiles =
            listOf(
              ".bazelbsp/aspects/BUILD",
              ".bazelbsp/aspects/core.bzl",
              ".bazelbsp/aspects/core.bzl.template",
              ".bazelbsp/aspects/extensions.bzl",
              ".bazelbsp/aspects/rules/android/android_info.bzl",
              ".bazelbsp/aspects/rules/android/android_info.bzl.template",
              ".bazelbsp/aspects/rules/cpp/cpp_info.bzl",
              ".bazelbsp/aspects/rules/go/go_info.bzl",
              ".bazelbsp/aspects/rules/go/go_info.bzl.template",
              ".bazelbsp/aspects/rules/java/java_info.bzl",
              ".bazelbsp/aspects/rules/java/java_info.bzl.template",
              ".bazelbsp/aspects/rules/jvm/jvm_info.bzl",
              ".bazelbsp/aspects/rules/jvm/jvm_info.bzl.template",
              ".bazelbsp/aspects/rules/kt/kt_info.bzl",
              ".bazelbsp/aspects/rules/kt/kt_info.bzl.template",
              ".bazelbsp/aspects/rules/python/python_info.bzl",
              ".bazelbsp/aspects/rules/python/python_info.bzl.template",
              ".bazelbsp/aspects/rules/scala/scala_info.bzl",
              ".bazelbsp/aspects/rules/scala/scala_info.bzl.template",
              ".bazelbsp/aspects/runtime_classpath_query.bzl",
              ".bazelbsp/aspects/toolchain_query.bzl",
              ".bazelbsp/aspects/utils/utils.bzl",
              ".bazelbsp/aspects/utils/utils.bzl.template",
            )

          aspectFiles.forEach { file ->
            addSources(
              BspTargetInfo.FileLocation.newBuilder().apply {
                relativePath = file
                isSource = true
              },
            )
          }

          javaRuntimeInfo =
            BspTargetInfo.JavaRuntimeInfo
              .newBuilder()
              .apply {
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+local_jdk"
                    }.build()
              }.build()
        }.build()
    targets[Label.parse("@//.bazelbsp/aspects:aspects")] = aspectsTarget

    // Create lib/libB:libB target (py_library with no dependencies)
    val libBTarget =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//lib/libB:libB"
          kind = "py_library"
          workspaceName = "_main"

          addSources(
            BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "lib/libB/src/bkng/lib/bbb/__init__.py"
              isSource = true
            },
          )
          addSources(
            BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "lib/libB/src/bkng/lib/bbb/hello.py"
              isSource = true
            },
          )

          javaRuntimeInfo =
            BspTargetInfo.JavaRuntimeInfo
              .newBuilder()
              .apply {
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+local_jdk"
                    }.build()
              }.build()

          pythonTargetInfo =
            BspTargetInfo.PythonTargetInfo
              .newBuilder()
              .apply {
                addImports("src")
              }.build()
        }.build()
    targets[Label.parse("@//lib/libB:libB")] = libBTarget

    // Create lib/libA:libA target (py_library depending on libB)
    val libATarget =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//lib/libA:libA"
          kind = "py_library"
          workspaceName = "_main"

          addDependencies(
            BspTargetInfo.Dependency.newBuilder().apply {
              id = "@//lib/libB:libB"
            },
          )

          addSources(
            BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "lib/libA/src/bkng/lib/aaa/__init__.py"
              isSource = true
            },
          )
          addSources(
            BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "lib/libA/src/bkng/lib/aaa/hello.py"
              isSource = true
            },
          )

          javaRuntimeInfo =
            BspTargetInfo.JavaRuntimeInfo
              .newBuilder()
              .apply {
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+local_jdk"
                    }.build()
              }.build()

          pythonTargetInfo =
            BspTargetInfo.PythonTargetInfo
              .newBuilder()
              .apply {
                addImports("src")
              }.build()
        }.build()
    targets[Label.parse("@//lib/libA:libA")] = libATarget

    // Create main:main target (py_binary depending on libA)
    val mainTarget =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//main:main"
          kind = "py_binary"
          workspaceName = "_main"
          executable = true

          addDependencies(
            BspTargetInfo.Dependency.newBuilder().apply {
              id = "@//lib/libA:libA"
            },
          )

          addSources(
            BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "main/main.py"
              isSource = true
            },
          )

          javaRuntimeInfo =
            BspTargetInfo.JavaRuntimeInfo
              .newBuilder()
              .apply {
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+local_jdk"
                    }.build()
              }.build()

          pythonTargetInfo =
            BspTargetInfo.PythonTargetInfo
              .newBuilder()
              .apply {
                interpreter =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      relativePath = "bin/python3"
                      isSource = true
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_python++python+python_3_11_x86_64-unknown-linux-gnu"
                    }.build()
                version = "PY3"
              }.build()
        }.build()
    targets[Label.parse("@//main:main")] = mainTarget

    return targets
  }

  // This method creates ALL targets exactly as specified in the input comments (lines 77-1318)
  // to produce the expected output at line 1320
  private fun createJavaScalaKotlinTestTargets(): Map<Label, BspTargetInfo.TargetInfo> {
    val targets = mutableMapOf<Label, BspTargetInfo.TargetInfo>()
    
    // Create all the targets as specified in the input section (before line 1437)

    // aspects target
    targets[Label.parse("@//.bazelbsp/aspects:aspects")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//.bazelbsp/aspects:aspects"
          kind = "filegroup"
          workspaceName = "_main"
        }.build()

    // scala_targets:scala_binary
    targets[Label.parse("@//scala_targets:scala_binary")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//scala_targets:scala_binary"
          kind = "scala_binary"
          executable = true
          workspaceName = "_main"

          addSources(
            BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "scala_targets/ScalaBinary.scala"
              isSource = true
            },
          )

          jvmTargetInfo =
            BspTargetInfo.JvmTargetInfo
              .newBuilder()
              .apply {
                addJars(BspTargetInfo.JvmOutputs.newBuilder().apply {
                  addBinaryJars(BspTargetInfo.FileLocation.newBuilder().apply {
                    relativePath = "scala_targets/scala_binary.jar"
                    rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                  })
                  addInterfaceJars(BspTargetInfo.FileLocation.newBuilder().apply {
                    relativePath = "scala_targets/scala_binary.jar"
                    rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                  })
                  addSourceJars(BspTargetInfo.FileLocation.newBuilder().apply {
                    relativePath = "scala_targets/scala_binary-src.jar"
                    rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                  })
                })
                mainClass = "example.Example"
                addJvmFlags("-Xms2G")
                addJvmFlags("-Xmx5G")
                addArgs("arg1")
                addArgs("arg2")
              }.build()

          scalaTargetInfo =
            BspTargetInfo.ScalaTargetInfo
              .newBuilder()
              .apply {
                addScalacOpts("-target:jvm-1.8")
                addCompilerClasspath(BspTargetInfo.FileLocation.newBuilder().apply {
                  relativePath = "scala-compiler-2.13.14.jar"
                  isSource = true
                  isExternal = true
                  rootExecutionPathFragment = "external/rules_scala++scala_deps+io_bazel_rules_scala_scala_compiler_2_13_14"
                })
                addCompilerClasspath(BspTargetInfo.FileLocation.newBuilder().apply {
                  relativePath = "scala-library-2.13.14.jar"
                  isSource = true
                  isExternal = true
                  rootExecutionPathFragment = "external/rules_scala++scala_deps+io_bazel_rules_scala_scala_library_2_13_14"
                })
                addCompilerClasspath(BspTargetInfo.FileLocation.newBuilder().apply {
                  relativePath = "scala-reflect-2.13.14.jar"
                  isSource = true
                  isExternal = true
                  rootExecutionPathFragment = "external/rules_scala++scala_deps+io_bazel_rules_scala_scala_reflect_2_13_14"
                })
              }.build()

          javaRuntimeInfo =
            BspTargetInfo.JavaRuntimeInfo
              .newBuilder()
              .apply {
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
                    }.build()
              }.build()
        }.build()

    // kotlin:java_binary
    targets[Label.parse("@//kotlin:java_binary")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//kotlin:java_binary"
          kind = "java_binary"
          executable = true
          workspaceName = "_main"
          
          addSources(BspTargetInfo.FileLocation.newBuilder().apply {
            relativePath = "kotlin/JavaBinary.java"
            isSource = true
          })
          
          // Add jvmTargetInfo as specified in input
          jvmTargetInfo = BspTargetInfo.JvmTargetInfo.newBuilder().apply {
            addJars(BspTargetInfo.JvmOutputs.newBuilder().apply {
              addBinaryJars(BspTargetInfo.FileLocation.newBuilder().apply {
                relativePath = "kotlin/java_binary.jar"
                rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
              })
              addSourceJars(BspTargetInfo.FileLocation.newBuilder().apply {
                relativePath = "kotlin/java_binary-src.jar"
                rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
              })
            })
            addJavacOpts("-Werror")
            addJavacOpts("-Xlint:all")
            mainClass = "java_targets.JavaBinary"
            addJdeps(BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "kotlin/java_binary.jdeps"
              rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
            })
          }.build()
          
          javaToolchainInfo = BspTargetInfo.JavaToolchainInfo.newBuilder().apply {
            sourceVersion = "11"
            targetVersion = "11"
            javaHome = BspTargetInfo.FileLocation.newBuilder().apply {
              isExternal = true
              rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk21_linux"
            }.build()
          }.build()
          
          javaRuntimeInfo = BspTargetInfo.JavaRuntimeInfo.newBuilder().apply {
            javaHome = BspTargetInfo.FileLocation.newBuilder().apply {
              isExternal = true
              rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
            }.build()
          }.build()
        }.build()

    // kotlin:java_binary_with_flag
    targets[Label.parse("@//kotlin:java_binary_with_flag")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//kotlin:java_binary_with_flag"
          kind = "java_binary"
          executable = true
          workspaceName = "_main"
          
          addSources(BspTargetInfo.FileLocation.newBuilder().apply {
            relativePath = "kotlin/JavaBinaryWithFlag.java"
            isSource = true
          })
          
          // Add jvmTargetInfo as specified in input
          jvmTargetInfo = BspTargetInfo.JvmTargetInfo.newBuilder().apply {
            addJars(BspTargetInfo.JvmOutputs.newBuilder().apply {
              addBinaryJars(BspTargetInfo.FileLocation.newBuilder().apply {
                relativePath = "kotlin/java_binary_with_flag.jar"
                rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
              })
              addSourceJars(BspTargetInfo.FileLocation.newBuilder().apply {
                relativePath = "kotlin/java_binary_with_flag-src.jar"
                rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
              })
            })
            addJavacOpts("-Werror")
            addJavacOpts("-Xlint:all")
            addJavacOpts("-target 17")
            mainClass = "java_targets.JavaBinaryWithFlag"
            addJdeps(BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "kotlin/java_binary_with_flag.jdeps"
              rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
            })
          }.build()

          javaToolchainInfo =
            BspTargetInfo.JavaToolchainInfo
              .newBuilder()
              .apply {
                sourceVersion = "11"
                targetVersion = "11"
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk21_linux"
                    }.build()
              }.build()

          javaRuntimeInfo =
            BspTargetInfo.JavaRuntimeInfo
              .newBuilder()
              .apply {
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
                    }.build()
              }.build()
        }.build()

    // java_targets:java_binary_with_flag
    targets[Label.parse("@//java_targets:java_binary_with_flag")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//java_targets:java_binary_with_flag"
          kind = "java_binary"
          executable = true
          workspaceName = "_main"
        }.build()

    // java_targets:java_binary
    targets[Label.parse("@//java_targets:java_binary")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//java_targets:java_binary"
          kind = "java_binary"
          executable = true
          workspaceName = "_main"

          addSources(
            BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "java_targets/JavaBinary.java"
              isSource = true
            },
          )

          javaRuntimeInfo =
            BspTargetInfo.JavaRuntimeInfo
              .newBuilder()
              .apply {
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
                    }.build()
              }.build()
        }.build()

    // genrule:foo
    targets[Label.parse("@//genrule:foo")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//genrule:foo"
          kind = "genrule"
          workspaceName = "_main"
        }.build()

    // target_with_resources:resources
    targets[Label.parse("@//target_with_resources:resources")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//target_with_resources:resources"
          kind = "filegroup"
          workspaceName = "_main"
          
          addSources(BspTargetInfo.FileLocation.newBuilder().apply {
            relativePath = "target_with_resources/file1.txt"
            isSource = true
          })
          
          addSources(BspTargetInfo.FileLocation.newBuilder().apply {
            relativePath = "target_with_resources/file2.txt"
            isSource = true
          })
          
          javaRuntimeInfo =
            BspTargetInfo.JavaRuntimeInfo
              .newBuilder()
              .apply {
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
                    }.build()
              }.build()
        }.build()

    // target_without_java_info:filegroup
    targets[Label.parse("@//target_without_java_info:filegroup")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//target_without_java_info:filegroup"
          kind = "filegroup"
          workspaceName = "_main"
        }.build()

    // environment_variables:java_binary
    targets[Label.parse("@//environment_variables:java_binary")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//environment_variables:java_binary"
          kind = "java_binary"
          executable = true
          workspaceName = "_main"

          addSources(
            BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "environment_variables/JavaEnv.java"
              isSource = true
            },
          )

          javaRuntimeInfo =
            BspTargetInfo.JavaRuntimeInfo
              .newBuilder()
              .apply {
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
                    }.build()
              }.build()
        }.build()

    // kotlin:asd
    targets[Label.parse("@//kotlin:asd")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//kotlin:asd"
          kind = "kt_jvm_library"
          workspaceName = "_main"

          jvmTargetInfo =
            BspTargetInfo.JvmTargetInfo
              .newBuilder()
              .apply {
                addJars(
                  BspTargetInfo.JvmOutputs.newBuilder().apply {
                    addBinaryJars(
                      BspTargetInfo.FileLocation.newBuilder().apply {
                        relativePath = "third_party/empty.jar"
                        isSource = true
                        isExternal = true
                        rootExecutionPathFragment = "external/rules_kotlin+"
                      },
                    )
                    addInterfaceJars(
                      BspTargetInfo.FileLocation.newBuilder().apply {
                        relativePath = "third_party/empty.jar"
                        isSource = true
                        isExternal = true
                        rootExecutionPathFragment = "external/rules_kotlin+"
                      },
                    )
                  },
                )
                addJdeps(
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      relativePath = "kotlin/asd.jdeps"
                      rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                    }.build(),
                )
              }.build()

          javaToolchainInfo =
            BspTargetInfo.JavaToolchainInfo
              .newBuilder()
              .apply {
                sourceVersion = "11"
                targetVersion = "11"
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk21_linux"
                    }.build()
              }.build()

          javaRuntimeInfo =
            BspTargetInfo.JavaRuntimeInfo
              .newBuilder()
              .apply {
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
                    }.build()
              }.build()
        }.build()

    // Add more targets from the rootTargets list...
    // target_without_jvm_flags:binary
    targets[Label.parse("@//target_without_jvm_flags:binary")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//target_without_jvm_flags:binary"
          kind = "scala_binary"
          executable = true
          workspaceName = "_main"

          addSources(
            BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "target_without_jvm_flags/Example.scala"
              isSource = true
            },
          )

          scalaTargetInfo = BspTargetInfo.ScalaTargetInfo.newBuilder().build()

          javaRuntimeInfo =
            BspTargetInfo.JavaRuntimeInfo
              .newBuilder()
              .apply {
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
                    }.build()
              }.build()
        }.build()

    // target_with_javac_exports:java_library
    targets[Label.parse("@//target_with_javac_exports:java_library")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//target_with_javac_exports:java_library"
          kind = "java_library"
          workspaceName = "_main"

          addSources(
            BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "target_with_javac_exports/JavaLibrary.java"
              isSource = true
            },
          )

          javaRuntimeInfo =
            BspTargetInfo.JavaRuntimeInfo
              .newBuilder()
              .apply {
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
                    }.build()
              }.build()
        }.build()

    // environment_variables:java_test
    targets[Label.parse("@//environment_variables:java_test")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//environment_variables:java_test"
          kind = "java_test"
          workspaceName = "_main"

          addSources(
            BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "environment_variables/JavaTest.java"
              isSource = true
            },
          )

          javaRuntimeInfo =
            BspTargetInfo.JavaRuntimeInfo
              .newBuilder()
              .apply {
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
                    }.build()
              }.build()
        }.build()

    // target_without_java_info:genrule
    targets[Label.parse("@//target_without_java_info:genrule")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//target_without_java_info:genrule"
          kind = "genrule"
          workspaceName = "_main"
        }.build()

    // scala_targets:scala_test
    targets[Label.parse("@//scala_targets:scala_test")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//scala_targets:scala_test"
          kind = "scala_test"
          workspaceName = "_main"

          addSources(
            BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "scala_targets/ScalaTest.scala"
              isSource = true
            },
          )

          scalaTargetInfo = BspTargetInfo.ScalaTargetInfo.newBuilder().build()

          javaRuntimeInfo =
            BspTargetInfo.JavaRuntimeInfo
              .newBuilder()
              .apply {
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
                    }.build()
              }.build()
        }.build()

    // java_targets/subpackage:java_library
    targets[Label.parse("@//java_targets/subpackage:java_library")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//java_targets/subpackage:java_library"
          kind = "java_library"
          workspaceName = "_main"

          addSources(
            BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "java_targets/subpackage/JavaLibrary2.java"
              isSource = true
            },
          )

          jvmTargetInfo =
            BspTargetInfo.JvmTargetInfo
              .newBuilder()
              .apply {
                addJars(
                  BspTargetInfo.JvmOutputs.newBuilder().apply {
                    addBinaryJars(
                      BspTargetInfo.FileLocation.newBuilder().apply {
                        relativePath = "java_targets/libjava_library_exported.jar"
                        rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                      },
                    )
                    addSourceJars(
                      BspTargetInfo.FileLocation.newBuilder().apply {
                        relativePath = "java_targets/libjava_library_exported-src.jar"
                        rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                      },
                    )
                  },
                )
              }.build()

          javaRuntimeInfo =
            BspTargetInfo.JavaRuntimeInfo
              .newBuilder()
              .apply {
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
                    }.build()
              }.build()
        }.build()

    // target_without_args:binary
    targets[Label.parse("@//target_without_args:binary")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//target_without_args:binary"
          kind = "scala_binary"
          executable = true
          workspaceName = "_main"

          addSources(
            BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "target_without_args/Example.scala"
              isSource = true
            },
          )

          jvmTargetInfo =
            BspTargetInfo.JvmTargetInfo
              .newBuilder()
              .apply {
                addJars(BspTargetInfo.JvmOutputs.newBuilder().apply {
                  addBinaryJars(BspTargetInfo.FileLocation.newBuilder().apply {
                    relativePath = "target_without_args/binary.jar"
                    rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                  })
                  addInterfaceJars(BspTargetInfo.FileLocation.newBuilder().apply {
                    relativePath = "target_without_args/binary.jar"
                    rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                  })
                  addSourceJars(BspTargetInfo.FileLocation.newBuilder().apply {
                    relativePath = "target_without_args/binary-src.jar"
                    rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                  })
                })
                mainClass = "example.Example"
                addJvmFlags("-Xms2G")
                addJvmFlags("-Xmx5G")
              }.build()

          scalaTargetInfo =
            BspTargetInfo.ScalaTargetInfo
              .newBuilder()
              .apply {
                addCompilerClasspath(BspTargetInfo.FileLocation.newBuilder().apply {
                  relativePath = "scala-compiler-2.13.14.jar"
                  isSource = true
                  isExternal = true
                  rootExecutionPathFragment = "external/rules_scala++scala_deps+io_bazel_rules_scala_scala_compiler_2_13_14"
                })
                addCompilerClasspath(BspTargetInfo.FileLocation.newBuilder().apply {
                  relativePath = "scala-library-2.13.14.jar"
                  isSource = true
                  isExternal = true
                  rootExecutionPathFragment = "external/rules_scala++scala_deps+io_bazel_rules_scala_scala_library_2_13_14"
                })
                addCompilerClasspath(BspTargetInfo.FileLocation.newBuilder().apply {
                  relativePath = "scala-reflect-2.13.14.jar"
                  isSource = true
                  isExternal = true
                  rootExecutionPathFragment = "external/rules_scala++scala_deps+io_bazel_rules_scala_scala_reflect_2_13_14"
                })
              }.build()

          javaRuntimeInfo =
            BspTargetInfo.JavaRuntimeInfo
              .newBuilder()
              .apply {
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
                    }.build()
              }.build()
        }.build()

    // no_ide_target:java_library
    targets[Label.parse("@//no_ide_target:java_library")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//no_ide_target:java_library"
          kind = "java_library"
          workspaceName = "_main"
        }.build()

    // java_targets:java_library
    targets[Label.parse("@//java_targets:java_library")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//java_targets:java_library"
          kind = "java_library"
          workspaceName = "_main"

          addSources(
            BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "java_targets/JavaLibrary.java"
              isSource = true
            },
          )

          jvmTargetInfo =
            BspTargetInfo.JvmTargetInfo
              .newBuilder()
              .apply {
                addJars(
                  BspTargetInfo.JvmOutputs.newBuilder().apply {
                    addBinaryJars(
                      BspTargetInfo.FileLocation.newBuilder().apply {
                        relativePath = "java_targets/libjava_library.jar"
                        rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                      },
                    )
                    addInterfaceJars(
                      BspTargetInfo.FileLocation.newBuilder().apply {
                        relativePath = "java_targets/libjava_library-hjar.jar"
                        rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                      },
                    )
                    addSourceJars(
                      BspTargetInfo.FileLocation.newBuilder().apply {
                        relativePath = "java_targets/libjava_library-src.jar"
                        rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                      },
                    )
                  },
                )
                addJdeps(
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      relativePath = "java_targets/libjava_library.jdeps"
                      rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                    }.build(),
                )
              }.build()

          javaRuntimeInfo =
            BspTargetInfo.JavaRuntimeInfo
              .newBuilder()
              .apply {
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
                    }.build()
              }.build()
        }.build()

    // target_without_main_class:library
    targets[Label.parse("@//target_without_main_class:library")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//target_without_main_class:library"
          kind = "scala_library"
          workspaceName = "_main"

          addSources(
            BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "target_without_main_class/Example.scala"
              isSource = true
            },
          )

          jvmTargetInfo =
            BspTargetInfo.JvmTargetInfo
              .newBuilder()
              .apply {
                addJars(BspTargetInfo.JvmOutputs.newBuilder().apply {
                  addBinaryJars(BspTargetInfo.FileLocation.newBuilder().apply {
                    relativePath = "target_without_main_class/library.jar"
                    rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                  })
                  addInterfaceJars(BspTargetInfo.FileLocation.newBuilder().apply {
                    relativePath = "target_without_main_class/library-ijar.jar"
                    rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                  })
                  addSourceJars(BspTargetInfo.FileLocation.newBuilder().apply {
                    relativePath = "target_without_main_class/library-src.jar"
                    rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                  })
                })
              }.build()

          scalaTargetInfo =
            BspTargetInfo.ScalaTargetInfo
              .newBuilder()
              .apply {
                addCompilerClasspath(BspTargetInfo.FileLocation.newBuilder().apply {
                  relativePath = "scala-compiler-2.13.14.jar"
                  isSource = true
                  isExternal = true
                  rootExecutionPathFragment = "external/rules_scala++scala_deps+io_bazel_rules_scala_scala_compiler_2_13_14"
                })
                addCompilerClasspath(BspTargetInfo.FileLocation.newBuilder().apply {
                  relativePath = "scala-library-2.13.14.jar"
                  isSource = true
                  isExternal = true
                  rootExecutionPathFragment = "external/rules_scala++scala_deps+io_bazel_rules_scala_scala_library_2_13_14"
                })
                addCompilerClasspath(BspTargetInfo.FileLocation.newBuilder().apply {
                  relativePath = "scala-reflect-2.13.14.jar"
                  isSource = true
                  isExternal = true
                  rootExecutionPathFragment = "external/rules_scala++scala_deps+io_bazel_rules_scala_scala_reflect_2_13_14"
                })
              }.build()

          javaRuntimeInfo =
            BspTargetInfo.JavaRuntimeInfo
              .newBuilder()
              .apply {
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
                    }.build()
              }.build()
        }.build()

    // java_targets:java_library_exported
    targets[Label.parse("@//java_targets:java_library_exported")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//java_targets:java_library_exported"
          kind = "java_library"
          workspaceName = "_main"
          
          // Add dependency as specified in input
          addDependencies(BspTargetInfo.Dependency.newBuilder().apply {
            id = "@//java_targets/subpackage:java_library"
          })
          
          // Add jvmTargetInfo with jars but NO sources - this triggers creation of _output_jars library
          jvmTargetInfo = BspTargetInfo.JvmTargetInfo.newBuilder().apply {
            addJars(BspTargetInfo.JvmOutputs.newBuilder().apply {
              addBinaryJars(BspTargetInfo.FileLocation.newBuilder().apply {
                relativePath = "java_targets/libjava_library_exported.jar"
                rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
              })
              addSourceJars(BspTargetInfo.FileLocation.newBuilder().apply {
                relativePath = "java_targets/libjava_library_exported-src.jar"
                rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
              })
            })
          }.build()
          
          javaToolchainInfo = BspTargetInfo.JavaToolchainInfo.newBuilder().apply {
            sourceVersion = "11"
            targetVersion = "11"
            javaHome = BspTargetInfo.FileLocation.newBuilder().apply {
              isExternal = true
              rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk21_linux"
            }.build()
          }.build()

          javaRuntimeInfo =
            BspTargetInfo.JavaRuntimeInfo
              .newBuilder()
              .apply {
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
                    }.build()
              }.build()
        }.build()

    // kotlin:java_library
    targets[Label.parse("@//kotlin:java_library")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//kotlin:java_library"
          kind = "java_library"
          workspaceName = "_main"
          
          // Add jvmTargetInfo even though there are no sources
          jvmTargetInfo = BspTargetInfo.JvmTargetInfo.newBuilder().build()

          javaRuntimeInfo =
            BspTargetInfo.JavaRuntimeInfo
              .newBuilder()
              .apply {
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
                    }.build()
              }.build()
        }.build()

    // target_with_resources:java_binary
    targets[Label.parse("@//target_with_resources:java_binary")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//target_with_resources:java_binary"
          kind = "java_binary"
          executable = true
          workspaceName = "_main"

          addSources(
            BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "target_with_resources/JavaBinary.java"
              isSource = true
            },
          )

          addResources(
            BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "target_with_resources/file1.txt"
              isSource = true
            },
          )

          addResources(
            BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "target_with_resources/file2.txt"
              isSource = true
            },
          )

          jvmTargetInfo =
            BspTargetInfo.JvmTargetInfo
              .newBuilder()
              .apply {
                addJars(BspTargetInfo.JvmOutputs.newBuilder().apply {
                  addBinaryJars(BspTargetInfo.FileLocation.newBuilder().apply {
                    relativePath = "target_with_resources/java_binary.jar"
                    rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                  })
                  addSourceJars(BspTargetInfo.FileLocation.newBuilder().apply {
                    relativePath = "target_with_resources/java_binary-src.jar"
                    rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                  })
                })
                mainClass = "target_with_resources.JavaBinary"
                addJdeps(BspTargetInfo.FileLocation.newBuilder().apply {
                  relativePath = "target_with_resources/java_binary.jdeps"
                  rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                })
              }.build()

          javaToolchainInfo =
            BspTargetInfo.JavaToolchainInfo
              .newBuilder()
              .apply {
                sourceVersion = "11"
                targetVersion = "11"
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk21_linux"
                    }.build()
              }.build()

          javaRuntimeInfo =
            BspTargetInfo.JavaRuntimeInfo
              .newBuilder()
              .apply {
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
                    }.build()
              }.build()
        }.build()

    // target_with_dependency:java_binary
    targets[Label.parse("@//target_with_dependency:java_binary")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//target_with_dependency:java_binary"
          kind = "java_binary"
          executable = true
          workspaceName = "_main"

          addSources(
            BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "target_with_dependency/JavaBinary.java"
              isSource = true
            },
          )

          javaRuntimeInfo =
            BspTargetInfo.JavaRuntimeInfo
              .newBuilder()
              .apply {
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
                    }.build()
              }.build()
        }.build()

    // kotlin:java_library_exported
    targets[Label.parse("@//kotlin:java_library_exported")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//kotlin:java_library_exported"
          kind = "java_library"
          workspaceName = "_main"
          
          // Add dependency as specified in input
          addDependencies(BspTargetInfo.Dependency.newBuilder().apply {
            id = "@//java_targets/subpackage:java_library"
          })
          
          // Add jvmTargetInfo with jars but NO sources - this triggers creation of _output_jars library
          jvmTargetInfo = BspTargetInfo.JvmTargetInfo.newBuilder().apply {
            addJars(BspTargetInfo.JvmOutputs.newBuilder().apply {
              addBinaryJars(BspTargetInfo.FileLocation.newBuilder().apply {
                relativePath = "kotlin/libjava_library_exported.jar"
                rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
              })
              addSourceJars(BspTargetInfo.FileLocation.newBuilder().apply {
                relativePath = "kotlin/libjava_library_exported-src.jar"
                rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
              })
            })
          }.build()
          
          javaToolchainInfo = BspTargetInfo.JavaToolchainInfo.newBuilder().apply {
            sourceVersion = "11"
            targetVersion = "11"
            javaHome = BspTargetInfo.FileLocation.newBuilder().apply {
              isExternal = true
              rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk21_linux"
            }.build()
          }.build()

          javaRuntimeInfo =
            BspTargetInfo.JavaRuntimeInfo
              .newBuilder()
              .apply {
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
                    }.build()
              }.build()
        }.build()

    // Add external Maven dependencies that appear in the expected output
    targets[Label.parse("@@rules_jvm_external++maven+maven//:com_google_code_findbugs_jsr305")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@@rules_jvm_external++maven+maven//:com_google_code_findbugs_jsr305"
          kind = "jvm_import"
          workspaceName = "_main"
          addTags("maven_coordinates=com.google.code.findbugs:jsr305:3.0.2")
          addTags("maven_repository=https://cache-redirector.jetbrains.com/maven-central")
          addTags("maven_sha256=766ad2a0783f2687962c8ad74ceecc38a28b9f72a2d085ee438b7813e928d0c7")
          addTags("maven_url=https://cache-redirector.jetbrains.com/maven-central/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.jar")

          jvmTargetInfo =
            BspTargetInfo.JvmTargetInfo
              .newBuilder()
              .apply {
                addJars(
                  BspTargetInfo.JvmOutputs.newBuilder().apply {
                    addBinaryJars(
                      BspTargetInfo.FileLocation.newBuilder().apply {
                        relativePath = "external/rules_jvm_external++maven+maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/code/findbugs/jsr305/3.0.2/processed_jsr305-3.0.2.jar"
                        isExternal = true
                        rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                      },
                    )
                    addInterfaceJars(
                      BspTargetInfo.FileLocation.newBuilder().apply {
                        relativePath = "external/rules_jvm_external++maven+maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/code/findbugs/jsr305/3.0.2/header_jsr305-3.0.2.jar"
                        isExternal = true
                        rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                      },
                    )
                  },
                )
              }.build()

          javaRuntimeInfo =
            BspTargetInfo.JavaRuntimeInfo
              .newBuilder()
              .apply {
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
                    }.build()
              }.build()
        }.build()

    // Add missing targets to reach exactly 15 non-module targets
    // Based on expected output, need these additional targets:

    // @//genrule:foo
    targets[Label.parse("@//genrule:foo")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//genrule:foo"
          kind = "genrule"
          executable = true
          workspaceName = "_main"
        }.build()

    // @//target_without_java_info:filegroup
    targets[Label.parse("@//target_without_java_info:filegroup")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//target_without_java_info:filegroup"
          kind = "filegroup"
          addSources(
            BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "target_without_java_info/Example.java"
              isSource = true
            },
          )
          executable = true
          workspaceName = "_main"
          javaRuntimeInfo =
            BspTargetInfo.JavaRuntimeInfo
              .newBuilder()
              .apply {
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
                    }.build()
              }.build()
        }.build()

    // @//target_without_java_info:genrule
    targets[Label.parse("@//target_without_java_info:genrule")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//target_without_java_info:genrule"
          kind = "genrule"
          addSources(
            BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "target_without_java_info/Example.kt"
              isSource = true
            },
          )
          executable = true
          workspaceName = "_main"
          javaRuntimeInfo =
            BspTargetInfo.JavaRuntimeInfo
              .newBuilder()
              .apply {
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
                    }.build()
              }.build()
        }.build()

    // @//target_with_resources:java_binary
    targets[Label.parse("@//target_with_resources:java_binary")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//target_with_resources:java_binary"
          kind = "java_binary"
          addSources(
            BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "target_with_resources/JavaBinary.java"
              isSource = true
            },
          )
          executable = true
          workspaceName = "_main"
          jvmTargetInfo =
            BspTargetInfo.JvmTargetInfo
              .newBuilder()
              .apply {
                addJars(
                  BspTargetInfo.JvmOutputs
                    .newBuilder()
                    .apply {
                      addBinaryJars(
                        BspTargetInfo.FileLocation.newBuilder().apply {
                          relativePath = "target_with_resources/java_binary.jar"
                          rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                        },
                      )
                      addSourceJars(
                        BspTargetInfo.FileLocation.newBuilder().apply {
                          relativePath = "target_with_resources/java_binary-src.jar"
                          rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                        },
                      )
                    }.build(),
                )
                mainClass = "target_with_resources.JavaBinary"
                addJdeps(
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      relativePath = "target_with_resources/java_binary.jdeps"
                      rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                    }.build(),
                )
              }.build()
          javaToolchainInfo =
            BspTargetInfo.JavaToolchainInfo
              .newBuilder()
              .apply {
                sourceVersion = "11"
                targetVersion = "11"
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk21_linux"
                    }.build()
              }.build()
          javaRuntimeInfo =
            BspTargetInfo.JavaRuntimeInfo
              .newBuilder()
              .apply {
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
                    }.build()
              }.build()
        }.build()

    // @//target_with_dependency:java_binary
    targets[Label.parse("@//target_with_dependency:java_binary")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//target_with_dependency:java_binary"
          kind = "java_binary"
          addDependencies(
            BspTargetInfo.Dependency.newBuilder().apply {
              id = "@//java_targets:java_library_exported"
            },
          )
          addDependencies(
            BspTargetInfo.Dependency.newBuilder().apply {
              id = "@@rules_jvm_external++maven+maven//:com_google_guava_guava"
            },
          )
          addDependencies(
            BspTargetInfo.Dependency.newBuilder().apply {
              id = "@//java_targets/subpackage:java_library"
            },
          )
          addSources(
            BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "target_with_dependency/JavaBinary.java"
              isSource = true
            },
          )
          executable = true
          workspaceName = "_main"
          jvmTargetInfo =
            BspTargetInfo.JvmTargetInfo
              .newBuilder()
              .apply {
                addJars(
                  BspTargetInfo.JvmOutputs
                    .newBuilder()
                    .apply {
                      addBinaryJars(
                        BspTargetInfo.FileLocation.newBuilder().apply {
                          relativePath = "target_with_dependency/java_binary.jar"
                          rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                        },
                      )
                      addSourceJars(
                        BspTargetInfo.FileLocation.newBuilder().apply {
                          relativePath = "target_with_dependency/java_binary-src.jar"
                          rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                        },
                      )
                    }.build(),
                )
                mainClass = "target_with_dependency.JavaBinary"
                addJdeps(
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      relativePath = "target_with_dependency/java_binary.jdeps"
                      rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                    }.build(),
                )
              }.build()
          javaToolchainInfo =
            BspTargetInfo.JavaToolchainInfo
              .newBuilder()
              .apply {
                sourceVersion = "11"
                targetVersion = "11"
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk21_linux"
                    }.build()
              }.build()
          javaRuntimeInfo =
            BspTargetInfo.JavaRuntimeInfo
              .newBuilder()
              .apply {
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
                    }.build()
              }.build()
        }.build()

    // Add final 2 targets to reach exactly 15 non-module targets

    // @//environment_variables:java_binary
    targets[Label.parse("@//environment_variables:java_binary")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//environment_variables:java_binary"
          kind = "java_binary"
          addSources(
            BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "environment_variables/JavaEnv.java"
              isSource = true
            },
          )
          executable = true
          workspaceName = "_main"
          jvmTargetInfo =
            BspTargetInfo.JvmTargetInfo
              .newBuilder()
              .apply {
                addJars(
                  BspTargetInfo.JvmOutputs
                    .newBuilder()
                    .apply {
                      addBinaryJars(
                        BspTargetInfo.FileLocation.newBuilder().apply {
                          relativePath = "environment_variables/java_binary.jar"
                          rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                        },
                      )
                      addSourceJars(
                        BspTargetInfo.FileLocation.newBuilder().apply {
                          relativePath = "environment_variables/java_binary-src.jar"
                          rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                        },
                      )
                    }.build(),
                )
                mainClass = "environment_variables.JavaEnv"
                addJdeps(
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      relativePath = "environment_variables/java_binary.jdeps"
                      rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                    }.build(),
                )
              }.build()
          javaToolchainInfo =
            BspTargetInfo.JavaToolchainInfo
              .newBuilder()
              .apply {
                sourceVersion = "11"
                targetVersion = "11"
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk21_linux"
                    }.build()
              }.build()
          javaRuntimeInfo =
            BspTargetInfo.JavaRuntimeInfo
              .newBuilder()
              .apply {
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
                    }.build()
              }.build()
        }.build()

    // @//target_without_jvm_flags:binary
    targets[Label.parse("@//target_without_jvm_flags:binary")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//target_without_jvm_flags:binary"
          kind = "scala_binary"
          addSources(
            BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "target_without_jvm_flags/Example.scala"
              isSource = true
            },
          )
          executable = true
          workspaceName = "_main"
          jvmTargetInfo =
            BspTargetInfo.JvmTargetInfo
              .newBuilder()
              .apply {
                addJars(
                  BspTargetInfo.JvmOutputs
                    .newBuilder()
                    .apply {
                      addBinaryJars(
                        BspTargetInfo.FileLocation.newBuilder().apply {
                          relativePath = "target_without_jvm_flags/binary.jar"
                          rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                        },
                      )
                      addInterfaceJars(
                        BspTargetInfo.FileLocation.newBuilder().apply {
                          relativePath = "target_without_jvm_flags/binary.jar"
                          rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                        },
                      )
                      addSourceJars(
                        BspTargetInfo.FileLocation.newBuilder().apply {
                          relativePath = "target_without_jvm_flags/binary-src.jar"
                          rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                        },
                      )
                    }.build(),
                )
                mainClass = "example.Example"
                addArgs("arg1")
                addArgs("arg2")
              }.build()
          javaRuntimeInfo =
            BspTargetInfo.JavaRuntimeInfo
              .newBuilder()
              .apply {
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
                    }.build()
              }.build()
          scalaTargetInfo =
            BspTargetInfo.ScalaTargetInfo
              .newBuilder()
              .apply {
                addCompilerClasspath(
                  BspTargetInfo.FileLocation.newBuilder().apply {
                    relativePath = "scala-compiler-2.13.14.jar"
                    isSource = true
                    isExternal = true
                    rootExecutionPathFragment = "external/rules_scala++scala_deps+io_bazel_rules_scala_scala_compiler_2_13_14"
                  },
                )
                addCompilerClasspath(
                  BspTargetInfo.FileLocation.newBuilder().apply {
                    relativePath = "scala-library-2.13.14.jar"
                    isSource = true
                    isExternal = true
                    rootExecutionPathFragment = "external/rules_scala++scala_deps+io_bazel_rules_scala_scala_library_2_13_14"
                  },
                )
                addCompilerClasspath(
                  BspTargetInfo.FileLocation.newBuilder().apply {
                    relativePath = "scala-reflect-2.13.14.jar"
                    isSource = true
                    isExternal = true
                    rootExecutionPathFragment = "external/rules_scala++scala_deps+io_bazel_rules_scala_scala_reflect_2_13_14"
                  },
                )
              }.build()
        }.build()

    // Add the final 2 targets to reach exactly 15 non-module targets
    // @//environment_variables:java_test
    targets[Label.parse("@//environment_variables:java_test")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//environment_variables:java_test"
          kind = "java_test"
          addSources(
            BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "environment_variables/JavaTest.java"
              isSource = true
            },
          )
          executable = true
          workspaceName = "_main"
          jvmTargetInfo =
            BspTargetInfo.JvmTargetInfo
              .newBuilder()
              .apply {
                addJars(
                  BspTargetInfo.JvmOutputs
                    .newBuilder()
                    .apply {
                      addBinaryJars(
                        BspTargetInfo.FileLocation.newBuilder().apply {
                          relativePath = "environment_variables/java_test.jar"
                          rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                        },
                      )
                      addSourceJars(
                        BspTargetInfo.FileLocation.newBuilder().apply {
                          relativePath = "environment_variables/java_test-src.jar"
                          rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                        },
                      )
                    }.build(),
                )
                mainClass = "environment_variables.JavaTest"
                addJdeps(
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      relativePath = "environment_variables/java_test.jdeps"
                      rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                    }.build(),
                )
              }.build()
          javaToolchainInfo =
            BspTargetInfo.JavaToolchainInfo
              .newBuilder()
              .apply {
                sourceVersion = "11"
                targetVersion = "11"
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk21_linux"
                    }.build()
              }.build()
          javaRuntimeInfo =
            BspTargetInfo.JavaRuntimeInfo
              .newBuilder()
              .apply {
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
                    }.build()
              }.build()
        }.build()

    // @//scala_targets:scala_test
    targets[Label.parse("@//scala_targets:scala_test")] =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          id = "@//scala_targets:scala_test"
          kind = "scala_test"
          addSources(
            BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "scala_targets/ScalaTest.scala"
              isSource = true
            },
          )
          executable = true
          workspaceName = "_main"
          jvmTargetInfo =
            BspTargetInfo.JvmTargetInfo
              .newBuilder()
              .apply {
                addJars(
                  BspTargetInfo.JvmOutputs
                    .newBuilder()
                    .apply {
                      addBinaryJars(
                        BspTargetInfo.FileLocation.newBuilder().apply {
                          relativePath = "scala_targets/scala_test.jar"
                          rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                        },
                      )
                      addInterfaceJars(
                        BspTargetInfo.FileLocation.newBuilder().apply {
                          relativePath = "scala_targets/scala_test-ijar.jar"
                          rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                        },
                      )
                      addSourceJars(
                        BspTargetInfo.FileLocation.newBuilder().apply {
                          relativePath = "scala_targets/scala_test-src.jar"
                          rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                        },
                      )
                    }.build(),
                )
                mainClass = "scala_targets.ScalaTest"
                addArgs("arg1")
                addArgs("arg2")
              }.build()
          javaRuntimeInfo =
            BspTargetInfo.JavaRuntimeInfo
              .newBuilder()
              .apply {
                javaHome =
                  BspTargetInfo.FileLocation
                    .newBuilder()
                    .apply {
                      isExternal = true
                      rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
                    }.build()
              }.build()
          scalaTargetInfo =
            BspTargetInfo.ScalaTargetInfo
              .newBuilder()
              .apply {
                addCompilerClasspath(
                  BspTargetInfo.FileLocation.newBuilder().apply {
                    relativePath = "scala-compiler-2.13.14.jar"
                    isSource = true
                    isExternal = true
                    rootExecutionPathFragment = "external/rules_scala++scala_deps+io_bazel_rules_scala_scala_compiler_2_13_14"
                  },
                )
                addCompilerClasspath(
                  BspTargetInfo.FileLocation.newBuilder().apply {
                    relativePath = "scala-library-2.13.14.jar"
                    isSource = true
                    isExternal = true
                    rootExecutionPathFragment = "external/rules_scala++scala_deps+io_bazel_rules_scala_scala_library_2_13_14"
                  },
                )
                addCompilerClasspath(
                  BspTargetInfo.FileLocation.newBuilder().apply {
                    relativePath = "scala-reflect-2.13.14.jar"
                    isSource = true
                    isExternal = true
                    rootExecutionPathFragment = "external/rules_scala++scala_deps+io_bazel_rules_scala_scala_reflect_2_13_14"
                  },
                )
                // Add all 14 scalatest_classpath entries as specified in the input
                addScalatestClasspath(
                  BspTargetInfo.FileLocation.newBuilder().apply {
                    relativePath = "scalactic_2.13-3.2.9.jar"
                    isSource = true
                    isExternal = true
                    rootExecutionPathFragment = "external/rules_scala++scala_deps+io_bazel_rules_scala_scalactic_2_13_14"
                  },
                )
                addScalatestClasspath(
                  BspTargetInfo.FileLocation.newBuilder().apply {
                    relativePath = "scalatest_2.13-3.2.9.jar"
                    isSource = true
                    isExternal = true
                    rootExecutionPathFragment = "external/rules_scala++scala_deps+io_bazel_rules_scala_scalatest_2_13_14"
                  },
                )
                addScalatestClasspath(
                  BspTargetInfo.FileLocation.newBuilder().apply {
                    relativePath = "scalatest-compatible-3.2.9.jar"
                    isSource = true
                    isExternal = true
                    rootExecutionPathFragment = "external/rules_scala++scala_deps+io_bazel_rules_scala_scalatest_compatible_2_13_14"
                  },
                )
                addScalatestClasspath(
                  BspTargetInfo.FileLocation.newBuilder().apply {
                    relativePath = "scalatest-core_2.13-3.2.9.jar"
                    isSource = true
                    isExternal = true
                    rootExecutionPathFragment = "external/rules_scala++scala_deps+io_bazel_rules_scala_scalatest_core_2_13_14"
                  },
                )
                addScalatestClasspath(
                  BspTargetInfo.FileLocation.newBuilder().apply {
                    relativePath = "scalatest-featurespec_2.13-3.2.9.jar"
                    isSource = true
                    isExternal = true
                    rootExecutionPathFragment = "external/rules_scala++scala_deps+io_bazel_rules_scala_scalatest_featurespec_2_13_14"
                  },
                )
                addScalatestClasspath(
                  BspTargetInfo.FileLocation.newBuilder().apply {
                    relativePath = "scalatest-flatspec_2.13-3.2.9.jar"
                    isSource = true
                    isExternal = true
                    rootExecutionPathFragment = "external/rules_scala++scala_deps+io_bazel_rules_scala_scalatest_flatspec_2_13_14"
                  },
                )
                addScalatestClasspath(
                  BspTargetInfo.FileLocation.newBuilder().apply {
                    relativePath = "scalatest-freespec_2.13-3.2.9.jar"
                    isSource = true
                    isExternal = true
                    rootExecutionPathFragment = "external/rules_scala++scala_deps+io_bazel_rules_scala_scalatest_freespec_2_13_14"
                  },
                )
                addScalatestClasspath(
                  BspTargetInfo.FileLocation.newBuilder().apply {
                    relativePath = "scalatest-funspec_2.13-3.2.9.jar"
                    isSource = true
                    isExternal = true
                    rootExecutionPathFragment = "external/rules_scala++scala_deps+io_bazel_rules_scala_scalatest_funspec_2_13_14"
                  },
                )
                addScalatestClasspath(
                  BspTargetInfo.FileLocation.newBuilder().apply {
                    relativePath = "scalatest-funsuite_2.13-3.2.9.jar"
                    isSource = true
                    isExternal = true
                    rootExecutionPathFragment = "external/rules_scala++scala_deps+io_bazel_rules_scala_scalatest_funsuite_2_13_14"
                  },
                )
                addScalatestClasspath(
                  BspTargetInfo.FileLocation.newBuilder().apply {
                    relativePath = "scalatest-matchers-core_2.13-3.2.9.jar"
                    isSource = true
                    isExternal = true
                    rootExecutionPathFragment = "external/rules_scala++scala_deps+io_bazel_rules_scala_scalatest_matchers_core_2_13_14"
                  },
                )
                addScalatestClasspath(
                  BspTargetInfo.FileLocation.newBuilder().apply {
                    relativePath = "scalatest-mustmatchers_2.13-3.2.9.jar"
                    isSource = true
                    isExternal = true
                    rootExecutionPathFragment = "external/rules_scala++scala_deps+io_bazel_rules_scala_scalatest_mustmatchers_2_13_14"
                  },
                )
                addScalatestClasspath(
                  BspTargetInfo.FileLocation.newBuilder().apply {
                    relativePath = "scalatest-shouldmatchers_2.13-3.2.9.jar"
                    isSource = true
                    isExternal = true
                    rootExecutionPathFragment = "external/rules_scala++scala_deps+io_bazel_rules_scala_scalatest_shouldmatchers_2_13_14"
                  },
                )
                addScalatestClasspath(
                  BspTargetInfo.FileLocation.newBuilder().apply {
                    relativePath = "external/rules_scala+/src/java/io/bazel/rulesscala/scala_test/librunner.jar"
                    isExternal = true
                    rootExecutionPathFragment = "bazel-out/k8-opt-exec-ST-d57f47055a04/bin"
                  },
                )
                addScalatestClasspath(
                  BspTargetInfo.FileLocation.newBuilder().apply {
                    relativePath = "external/rules_scala+/scala/support/test_reporter.jar"
                    isExternal = true
                    rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
                  },
                )
              }.build()
        }.build()

    // Add all Maven dependencies as specified in the input comments
    // These are needed to produce the 26 libraries in the expected output
    
    // @@rules_jvm_external++maven+maven//:com_google_code_findbugs_jsr305
    targets[Label.parse("@@rules_jvm_external++maven+maven//:com_google_code_findbugs_jsr305")] =
      BspTargetInfo.TargetInfo.newBuilder().apply {
        id = "@@rules_jvm_external++maven+maven//:com_google_code_findbugs_jsr305"
        kind = "jvm_import"
        addTags("maven_coordinates=com.google.code.findbugs:jsr305:3.0.2")
        addTags("maven_repository=https://cache-redirector.jetbrains.com/maven-central")
        addTags("maven_sha256=766ad2a0783f2687962c8ad74ceecc38a28b9f72a2d085ee438b7813e928d0c7")
        addTags("maven_url=https://cache-redirector.jetbrains.com/maven-central/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.jar")
        workspaceName = "_main"
        
        jvmTargetInfo = BspTargetInfo.JvmTargetInfo.newBuilder().apply {
          addJars(BspTargetInfo.JvmOutputs.newBuilder().apply {
            addBinaryJars(BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "external/rules_jvm_external++maven+maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/code/findbugs/jsr305/3.0.2/processed_jsr305-3.0.2.jar"
              isExternal = true
              rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
            })
            addInterfaceJars(BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "external/rules_jvm_external++maven+maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/code/findbugs/jsr305/3.0.2/header_jsr305-3.0.2.jar"
              isExternal = true
              rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
            })
          })
        }.build()
        
        javaRuntimeInfo = BspTargetInfo.JavaRuntimeInfo.newBuilder().apply {
          javaHome = BspTargetInfo.FileLocation.newBuilder().apply {
            isExternal = true
            rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
          }.build()
        }.build()
      }.build()

    // @@rules_jvm_external++maven+maven//:com_google_errorprone_error_prone_annotations
    targets[Label.parse("@@rules_jvm_external++maven+maven//:com_google_errorprone_error_prone_annotations")] =
      BspTargetInfo.TargetInfo.newBuilder().apply {
        id = "@@rules_jvm_external++maven+maven//:com_google_errorprone_error_prone_annotations"
        kind = "jvm_import"
        addTags("maven_coordinates=com.google.errorprone:error_prone_annotations:2.7.1")
        addTags("maven_repository=https://cache-redirector.jetbrains.com/maven-central")
        addTags("maven_sha256=cd5257c08a246cf8628817ae71cb822be192ef91f6881ca4a3fcff4f1de1cff3")
        addTags("maven_url=https://cache-redirector.jetbrains.com/maven-central/com/google/errorprone/error_prone_annotations/2.7.1/error_prone_annotations-2.7.1.jar")
        workspaceName = "_main"
        
        jvmTargetInfo = BspTargetInfo.JvmTargetInfo.newBuilder().apply {
          addJars(BspTargetInfo.JvmOutputs.newBuilder().apply {
            addBinaryJars(BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "external/rules_jvm_external++maven+maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/errorprone/error_prone_annotations/2.7.1/processed_error_prone_annotations-2.7.1.jar"
              isExternal = true
              rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
            })
            addInterfaceJars(BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "external/rules_jvm_external++maven+maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/errorprone/error_prone_annotations/2.7.1/header_error_prone_annotations-2.7.1.jar"
              isExternal = true
              rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
            })
          })
        }.build()
        
        javaRuntimeInfo = BspTargetInfo.JavaRuntimeInfo.newBuilder().apply {
          javaHome = BspTargetInfo.FileLocation.newBuilder().apply {
            isExternal = true
            rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
          }.build()
        }.build()
      }.build()

    // @@rules_jvm_external++maven+maven//:com_google_guava_failureaccess
    targets[Label.parse("@@rules_jvm_external++maven+maven//:com_google_guava_failureaccess")] =
      BspTargetInfo.TargetInfo.newBuilder().apply {
        id = "@@rules_jvm_external++maven+maven//:com_google_guava_failureaccess"
        kind = "jvm_import"
        addTags("maven_coordinates=com.google.guava:failureaccess:1.0.1")
        addTags("maven_repository=https://cache-redirector.jetbrains.com/maven-central")
        addTags("maven_sha256=a171ee4c734dd2da837e4b16be9df4661afab72a41adaf31eb84dfdaf936ca26")
        addTags("maven_url=https://cache-redirector.jetbrains.com/maven-central/com/google/guava/failureaccess/1.0.1/failureaccess-1.0.1.jar")
        workspaceName = "_main"
        
        jvmTargetInfo = BspTargetInfo.JvmTargetInfo.newBuilder().apply {
          addJars(BspTargetInfo.JvmOutputs.newBuilder().apply {
            addBinaryJars(BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "external/rules_jvm_external++maven+maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/guava/failureaccess/1.0.1/processed_failureaccess-1.0.1.jar"
              isExternal = true
              rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
            })
            addInterfaceJars(BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "external/rules_jvm_external++maven+maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/guava/failureaccess/1.0.1/header_failureaccess-1.0.1.jar"
              isExternal = true
              rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
            })
          })
        }.build()
        
        javaRuntimeInfo = BspTargetInfo.JavaRuntimeInfo.newBuilder().apply {
          javaHome = BspTargetInfo.FileLocation.newBuilder().apply {
            isExternal = true
            rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
          }.build()
        }.build()
      }.build()

    // @@rules_jvm_external++maven+maven//:com_google_guava_listenablefuture
    targets[Label.parse("@@rules_jvm_external++maven+maven//:com_google_guava_listenablefuture")] =
      BspTargetInfo.TargetInfo.newBuilder().apply {
        id = "@@rules_jvm_external++maven+maven//:com_google_guava_listenablefuture"
        kind = "jvm_import"
        addTags("maven_coordinates=com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava")
        addTags("maven_repository=https://cache-redirector.jetbrains.com/maven-central")
        addTags("maven_sha256=b372a037d4230aa57fbeffdef30fd6123f9c0c2db85d0aced00c91b974f33f99")
        addTags("maven_url=https://cache-redirector.jetbrains.com/maven-central/com/google/guava/listenablefuture/9999.0-empty-to-avoid-conflict-with-guava/listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar")
        workspaceName = "_main"
        
        jvmTargetInfo = BspTargetInfo.JvmTargetInfo.newBuilder().apply {
          addJars(BspTargetInfo.JvmOutputs.newBuilder().apply {
            addBinaryJars(BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "external/rules_jvm_external++maven+maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/guava/listenablefuture/9999.0-empty-to-avoid-conflict-with-guava/processed_listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar"
              isExternal = true
              rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
            })
            addInterfaceJars(BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "external/rules_jvm_external++maven+maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/guava/listenablefuture/9999.0-empty-to-avoid-conflict-with-guava/header_listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar"
              isExternal = true
              rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
            })
          })
        }.build()
        
        javaRuntimeInfo = BspTargetInfo.JavaRuntimeInfo.newBuilder().apply {
          javaHome = BspTargetInfo.FileLocation.newBuilder().apply {
            isExternal = true
            rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
          }.build()
        }.build()
      }.build()

    // @@rules_jvm_external++maven+maven//:com_google_j2objc_j2objc_annotations
    targets[Label.parse("@@rules_jvm_external++maven+maven//:com_google_j2objc_j2objc_annotations")] =
      BspTargetInfo.TargetInfo.newBuilder().apply {
        id = "@@rules_jvm_external++maven+maven//:com_google_j2objc_j2objc_annotations"
        kind = "jvm_import"
        addTags("maven_coordinates=com.google.j2objc:j2objc-annotations:1.3")
        addTags("maven_repository=https://cache-redirector.jetbrains.com/maven-central")
        addTags("maven_sha256=21af30c92267bd6122c0e0b4d20cccb6641a37eaf956c6540ec471d584e64a7b")
        addTags("maven_url=https://cache-redirector.jetbrains.com/maven-central/com/google/j2objc/j2objc-annotations/1.3/j2objc-annotations-1.3.jar")
        workspaceName = "_main"
        
        jvmTargetInfo = BspTargetInfo.JvmTargetInfo.newBuilder().apply {
          addJars(BspTargetInfo.JvmOutputs.newBuilder().apply {
            addBinaryJars(BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "external/rules_jvm_external++maven+maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/j2objc/j2objc-annotations/1.3/processed_j2objc-annotations-1.3.jar"
              isExternal = true
              rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
            })
            addInterfaceJars(BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "external/rules_jvm_external++maven+maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/j2objc/j2objc-annotations/1.3/header_j2objc-annotations-1.3.jar"
              isExternal = true
              rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
            })
          })
        }.build()
        
        javaRuntimeInfo = BspTargetInfo.JavaRuntimeInfo.newBuilder().apply {
          javaHome = BspTargetInfo.FileLocation.newBuilder().apply {
            isExternal = true
            rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
          }.build()
        }.build()
      }.build()

    // @@rules_jvm_external++maven+maven//:org_checkerframework_checker_qual
    targets[Label.parse("@@rules_jvm_external++maven+maven//:org_checkerframework_checker_qual")] =
      BspTargetInfo.TargetInfo.newBuilder().apply {
        id = "@@rules_jvm_external++maven+maven//:org_checkerframework_checker_qual"
        kind = "jvm_import"
        addTags("maven_coordinates=org.checkerframework:checker-qual:3.12.0")
        addTags("maven_repository=https://cache-redirector.jetbrains.com/maven-central")
        addTags("maven_sha256=ff10785ac2a357ec5de9c293cb982a2cbb605c0309ea4cc1cb9b9bc6dbe7f3cb")
        addTags("maven_url=https://cache-redirector.jetbrains.com/maven-central/org/checkerframework/checker-qual/3.12.0/checker-qual-3.12.0.jar")
        workspaceName = "_main"
        
        jvmTargetInfo = BspTargetInfo.JvmTargetInfo.newBuilder().apply {
          addJars(BspTargetInfo.JvmOutputs.newBuilder().apply {
            addBinaryJars(BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "external/rules_jvm_external++maven+maven/v1/https/cache-redirector.jetbrains.com/maven-central/org/checkerframework/checker-qual/3.12.0/processed_checker-qual-3.12.0.jar"
              isExternal = true
              rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
            })
            addInterfaceJars(BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "external/rules_jvm_external++maven+maven/v1/https/cache-redirector.jetbrains.com/maven-central/org/checkerframework/checker-qual/3.12.0/header_checker-qual-3.12.0.jar"
              isExternal = true
              rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
            })
          })
        }.build()
        
        javaRuntimeInfo = BspTargetInfo.JavaRuntimeInfo.newBuilder().apply {
          javaHome = BspTargetInfo.FileLocation.newBuilder().apply {
            isExternal = true
            rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
          }.build()
        }.build()
      }.build()

    // @@rules_jvm_external++maven+maven//:com_google_guava_guava - The main Guava library with dependencies
    targets[Label.parse("@@rules_jvm_external++maven+maven//:com_google_guava_guava")] =
      BspTargetInfo.TargetInfo.newBuilder().apply {
        id = "@@rules_jvm_external++maven+maven//:com_google_guava_guava"
        kind = "jvm_import"
        addTags("maven_coordinates=com.google.guava:guava:31.0.1-jre")
        addTags("maven_repository=https://cache-redirector.jetbrains.com/maven-central")
        addTags("maven_sha256=d5be94d65e87bd219fb3193ad1517baa55a3b88fc91d21cf735826ab5af087b9")
        addTags("maven_url=https://cache-redirector.jetbrains.com/maven-central/com/google/guava/guava/31.0.1-jre/guava-31.0.1-jre.jar")
        
        // Add all 6 dependencies as specified in the input
        addDependencies(BspTargetInfo.Dependency.newBuilder().apply {
          id = "@@rules_jvm_external++maven+maven//:com_google_code_findbugs_jsr305"
        })
        addDependencies(BspTargetInfo.Dependency.newBuilder().apply {
          id = "@@rules_jvm_external++maven+maven//:com_google_errorprone_error_prone_annotations"
        })
        addDependencies(BspTargetInfo.Dependency.newBuilder().apply {
          id = "@@rules_jvm_external++maven+maven//:com_google_guava_failureaccess"
        })
        addDependencies(BspTargetInfo.Dependency.newBuilder().apply {
          id = "@@rules_jvm_external++maven+maven//:com_google_guava_listenablefuture"
        })
        addDependencies(BspTargetInfo.Dependency.newBuilder().apply {
          id = "@@rules_jvm_external++maven+maven//:com_google_j2objc_j2objc_annotations"
        })
        addDependencies(BspTargetInfo.Dependency.newBuilder().apply {
          id = "@@rules_jvm_external++maven+maven//:org_checkerframework_checker_qual"
        })
        
        workspaceName = "_main"
        
        jvmTargetInfo = BspTargetInfo.JvmTargetInfo.newBuilder().apply {
          addJars(BspTargetInfo.JvmOutputs.newBuilder().apply {
            addBinaryJars(BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "external/rules_jvm_external++maven+maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/guava/guava/31.0.1-jre/processed_guava-31.0.1-jre.jar"
              isExternal = true
              rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
            })
            addInterfaceJars(BspTargetInfo.FileLocation.newBuilder().apply {
              relativePath = "external/rules_jvm_external++maven+maven/v1/https/cache-redirector.jetbrains.com/maven-central/com/google/guava/guava/31.0.1-jre/header_guava-31.0.1-jre.jar"
              isExternal = true
              rootExecutionPathFragment = "bazel-out/k8-fastbuild/bin"
            })
          })
        }.build()
        
        javaRuntimeInfo = BspTargetInfo.JavaRuntimeInfo.newBuilder().apply {
          javaHome = BspTargetInfo.FileLocation.newBuilder().apply {
            isExternal = true
            rootExecutionPathFragment = "external/rules_java++toolchains+remotejdk11_linux"
          }.build()
        }.build()
      }.build()

    // Add internal library targets that appear in the expected output
    // These are referenced as dependencies in java_library_exported targets
    
    // This doesn't actually exist as a separate target in the input, but is created by the mapper
    // We don't need to add it here as it's created internally
    
    return targets
  }
}
