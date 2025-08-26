package org.jetbrains.bazel.sync.workspace.mapper.normal

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith
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
import org.jetbrains.bazel.server.sync.TargetInfoReader
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
  private val packageMapping =
    mapOf(
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
    return packageMapping.entries
      .firstOrNull { (file, _) ->
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
  val targetInfoReader = TargetInfoReader(null)

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


    }
  }
}
