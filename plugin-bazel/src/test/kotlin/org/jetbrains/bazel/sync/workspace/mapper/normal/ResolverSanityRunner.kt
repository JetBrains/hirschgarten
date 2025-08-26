package org.jetbrains.bazel.sync.workspace.mapper.normal

import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.commons.*
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.performance.telemetry.TelemetryManager
import org.jetbrains.bazel.server.sync.ProjectResolver
import org.jetbrains.bazel.server.sync.TargetInfoReader
import org.jetbrains.bazel.startup.*
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
import org.jetbrains.bazel.sync.workspace.mapper.normal.AspectBazelProjectMapper
import org.jetbrains.bazel.sync.workspace.mapper.normal.AspectClientProjectMapper  
import org.jetbrains.bazel.sync.workspace.mapper.normal.MavenCoordinatesResolver
import org.jetbrains.bazel.sync.workspace.mapper.normal.TargetTagsResolver
import org.jetbrains.bazel.workspacecontext.*
import org.jetbrains.bsp.protocol.FeatureFlags
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.pathString
import kotlin.system.exitProcess

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

/**
 * Pretty prints a string representation by adding proper indentation and line breaks.
 * Handles parentheses, brackets, and splits lines after commas for better readability.
 */
fun prettyPrint(input: String): String {
  val result = StringBuilder()
  var indentLevel = 0
  val indentSize = 2
  var i = 0
  
  fun addIndent() {
    repeat(indentLevel * indentSize) { result.append(' ') }
  }
  
  fun addNewlineAndIndent() {
    result.append('\n')
    addIndent()
  }
  
  while (i < input.length) {
    val char = input[i]
    
    when (char) {
      '(', '[', '{' -> {
        result.append(char)
        indentLevel++
        addNewlineAndIndent()
      }
      ')', ']', '}' -> {
        indentLevel--
        addNewlineAndIndent()
        result.append(char)
      }
      ',' -> {
        result.append(char)
        // Check if next non-space character starts a new field or is a closing bracket
        var j = i + 1
        while (j < input.length && input[j] == ' ') j++
        if (j < input.length && input[j] !in setOf(')', ']', '}')) {
          addNewlineAndIndent()
        } else {
          result.append(' ')
        }
      }
      ' ' -> {
        // Skip multiple spaces, we control spacing
        if (result.isNotEmpty() && result.last() != ' ' && result.last() != '\n') {
          result.append(' ')
        }
      }
      else -> {
        result.append(char)
      }
    }
    i++
  }
  
  return result.toString()
}

fun main(args: Array<String>) {
  if (args.isEmpty()) {
    println("Usage: ResolverSanityRunner <path-to-textproto-file> [<path-to-textproto-file> ...]")
    exitProcess(1)
  }

  runBlocking {
    try {
      // Initialize providers for tests
      SystemInfoProvider.provideSystemInfoProvider(IntellijSystemInfoProvider)
      FileUtil.provideFileUtil(FileUtilIntellij)
      EnvironmentProvider.provideEnvironmentProvider(IntellijEnvironmentProvider)
      TelemetryManager.provideTelemetryManager(IntellijTelemetryManager)

      val targetInfoReader = TargetInfoReader(null)

      // Convert string arguments to Path objects
      val textprotoFiles = args.map { Paths.get(it) }.toSet()
      
      // Read target map from aspect outputs
      val rawTargetsMap: Map<Label, TargetInfo> = targetInfoReader.readTargetMapFromAspectOutputs(textprotoFiles)

      // Process with unified configuration
      processWithUnifiedSetup(rawTargetsMap)

    } catch (e: Exception) {
      println("Error processing files: ${e.message}")
      e.printStackTrace()
      exitProcess(1)
    }
  }
}

private suspend fun processWithUnifiedSetup(rawTargetsMap: Map<Label, TargetInfo>) {
  // Use generic workspace root that works for test data
  val workspaceRoot = Paths.get("/tmp/test-workspace")
  
  // Create BazelInfo with sensible defaults
  val bazelInfo =
    BazelInfo(
      execRoot = workspaceRoot.resolve("bazel-out"),
      outputBase = Paths.get("/tmp/.cache/bazel/output_base"),
      workspaceRoot = workspaceRoot,
      bazelBin = workspaceRoot.resolve("bazel-bin"),
      release = BazelRelease(major = 8),
      isBzlModEnabled = true,
      isWorkspaceEnabled = true,
      externalAutoloads = emptyList(),
    )

  // Create unified RepoMapping with common external repositories
  val canonicalRepoNameToPath =
    mapOf(
      "+_repo_rules+bazelbsp_aspect" to
        Paths.get("/tmp/.cache/bazel/external/+_repo_rules+bazelbsp_aspect"),
      "" to workspaceRoot,
      "bazel_tools" to Paths.get("/tmp/.cache/bazel/external/bazel_tools"),
      "local_config_platform" to Paths.get("/tmp/.cache/bazel/external/local_config_platform"),
      "rules_java+" to Paths.get("/tmp/.cache/bazel/external/rules_java+"),
      "rules_python+" to Paths.get("/tmp/.cache/bazel/external/rules_python+"),
      "rules_scala+" to Paths.get("/tmp/.cache/bazel/external/rules_scala+"),
      "rules_kotlin+" to Paths.get("/tmp/.cache/bazel/external/rules_kotlin+"),
    )

  val repoMapping: RepoMapping =
    BzlmodRepoMapping(
      canonicalRepoNameToLocalPath = emptyMap(),
      apparentRepoNameToCanonicalName = IntellijBidirectionalMap(),
      canonicalRepoNameToPath = canonicalRepoNameToPath,
    )

  // Create unified workspace context
  val workspaceContext =
    WorkspaceContext(
      targets = TargetsSpec(values = listOf(Label.parse("@//...:all")), excludedValues = emptyList()),
      directories =
        DirectoriesSpec(
          values = listOf(workspaceRoot, workspaceRoot.resolve(".bazelproject")),
          excludedValues = emptyList(),
        ),
      buildFlags = BuildFlagsSpec(values = emptyList()),
      syncFlags = SyncFlagsSpec(values = emptyList()),
      debugFlags = DebugFlagsSpec(values = emptyList()),
      bazelBinary = BazelBinarySpec(value = Paths.get("/usr/bin/bazel")),
      allowManualTargetsSync = AllowManualTargetsSyncSpec(value = false),
      dotBazelBspDirPath = DotBazelBspDirPathSpec(value = workspaceRoot.resolve(".bazelbsp")),
      importDepth = ImportDepthSpec(value = -1),
      enabledRules = EnabledRulesSpec(values = listOf("rules_java", "rules_python", "rules_scala", "rules_kotlin")),
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

  // Setup unified feature flags  
  val featureFlags =
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

  processAndPrint(rawTargetsMap, repoMapping, workspaceContext, featureFlags, bazelInfo)
}


private suspend fun processAndPrint(
  rawTargetsMap: Map<Label, TargetInfo>,
  repoMapping: RepoMapping,
  workspaceContext: WorkspaceContext,
  featureFlags: FeatureFlags,
  bazelInfo: BazelInfo
) {
  // Process target map using ProjectResolver.processTargetMap
  val targets = ProjectResolver.processTargetMap(rawTargetsMap, repoMapping)

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

  // Determine root targets from the processed targets
  val rootTargets = targets.keys.toSet()

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

  // Print the result with pretty formatting
  println(prettyPrint(resolvedWorkspace.toString()))
}
