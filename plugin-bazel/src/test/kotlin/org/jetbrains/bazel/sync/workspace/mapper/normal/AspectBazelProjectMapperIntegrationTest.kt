package org.jetbrains.bazel.sync.workspace.mapper.normal

import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.FileLocation
import org.jetbrains.bazel.info.BspTargetInfo.JvmOutputs
import org.jetbrains.bazel.info.BspTargetInfo.JvmTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.KotlincPluginInfo
import org.jetbrains.bazel.info.BspTargetInfo.KotlinTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.languages.DefaultJvmPackageResolver
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginsService
import org.jetbrains.bazel.workspace.model.test.framework.BazelPathsResolverMock
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.FeatureFlags
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Path

class AspectBazelProjectMapperIntegrationTest {
  private fun resolver(tmp: Path = Path.of("")): BazelPathsResolver = BazelPathsResolverMock.create(tmp)

  private fun fileLocation(path: String): FileLocation = FileLocation.newBuilder().setRelativePath(path).build()

  private fun jvmOutputs(bin: String): JvmOutputs = JvmOutputs.newBuilder().addBinaryJars(fileLocation(bin)).build()

  private fun jvmInfoWithOutputs(vararg jars: String): JvmTargetInfo =
    JvmTargetInfo.newBuilder().addAllJars(jars.map { jvmOutputs(it) }).build()

  private fun kotlinInfo(stdlib: String, pluginJar: String): KotlinTargetInfo =
    KotlinTargetInfo.newBuilder()
      .addStdlibs(fileLocation(stdlib))
      .addKotlincPluginInfos(KotlincPluginInfo.newBuilder().addPluginJars(fileLocation(pluginJar)).build())
      .build()

  private fun target(
    id: String,
    kind: String,
    jvm: JvmTargetInfo? = null,
    kt: KotlinTargetInfo? = null,
    resources: List<String> = emptyList(),
    deps: List<String> = emptyList(),
  ): TargetInfo =
    TargetInfo.newBuilder()
      .setId(id)
      .setKind(kind)
      .apply { if (jvm != null) setJvmTargetInfo(jvm) }
      .apply { if (kt != null) setKotlinTargetInfo(kt) }
      .addAllResources(resources.map { fileLocation(it) })
      .addAllDependencies(deps.map { BspTargetInfo.Dependency.newBuilder().setId(it).build() })
      .build()

  private fun workspace(): WorkspaceContext = WorkspaceContext(
    targets = emptyList(),
    directories = emptyList(),
    buildFlags = emptyList(),
    syncFlags = emptyList(),
    debugFlags = emptyList(),
    bazelBinary = null,
    allowManualTargetsSync = false,
    dotBazelBspDirPath = Path.of(""),
    importDepth = 1,
    enabledRules = emptyList(),
    ideJavaHomeOverride = null,
    shardSync = false,
    targetShardSize = 0,
    shardingApproach = null,
    importRunConfigurations = emptyList(),
    gazelleTarget = null,
    indexAllFilesInDirectories = false,
    pythonCodeGeneratorRuleNames = emptyList(),
    importIjars = false,
    deriveInstrumentationFilterFromTargets = false,
    indexAdditionalFilesInDirectories = emptyList(),
  )

  @Test
  fun `mapper aggregates plugin libraries and resources`() = runBlocking {
    val pathsResolver = resolver()
    val lps = LanguagePluginsService().also { it.registerDefaultPlugins(pathsResolver, DefaultJvmPackageResolver()) }

    // Initialize telemetry for tests using mapper
    org.jetbrains.bazel.performance.telemetry.TelemetryManager.provideTelemetryManager(org.jetbrains.bazel.startup.IntellijTelemetryManager)
    val mapper = AspectBazelProjectMapper(
      languagePluginsService = lps,
      featureFlags = FeatureFlags(),
      bazelPathsResolver = pathsResolver,
      targetTagsResolver = TargetTagsResolver(),
    )

    val stdlib = "external/maven/v1/https/maven/org/jetbrains/kotlin/kotlin-stdlib-1.9.0.jar"
    val ktPlugin = "bazel-bin/plugins/kotlin/my-plugin.jar"

    val jvmBin = "bazel-bin/app/libapp.jar"
    val res = "app/src/main/resources/conf.txt"

    val core = target(
      id = "//core:lib",
      kind = "java_library",
      jvm = jvmInfoWithOutputs(jvmBin),
      deps = emptyList(),
      resources = listOf(res),
    )

    val app = target(
      id = "//app:lib",
      kind = "kt_jvm_library",
      jvm = jvmInfoWithOutputs(jvmBin),
      kt = kotlinInfo(stdlib, ktPlugin),
      deps = listOf(core.id),
    )

    val all = mapOf(Label.parse(core.id) to core, Label.parse(app.id) to app)
    val roots = setOf(Label.parse(app.id))

    val ws = mapper.createProject(
      targets = all,
      rootTargets = roots,
      workspaceContext = workspace(),
      repoMapping = RepoMappingDisabled,
      hasError = false,
    )

    // Libraries should include Kotlin stdlib bundle + Kotlin plugin jars + output jars (from Java plugin)
    val libJars = ws.libraries.flatMap { it.jars + it.ijars }.map { it.fileName.toString() }.toSet()
    assertTrue(libJars.contains("kotlin-stdlib-1.9.0.jar"))
    assertTrue(libJars.contains("my-plugin.jar"))
    assertTrue(libJars.contains("libapp.jar"))

    // Resources declared on a target should be exposed for that target via plugin collectResources
    val coreTarget = ws.targets.getTargets().first { it.id == Label.parse(core.id) }
    val resourceNames = coreTarget.resources.map { it.fileName.toString() }.toSet()
    assertTrue(resourceNames.contains("conf.txt"))
  }
}
