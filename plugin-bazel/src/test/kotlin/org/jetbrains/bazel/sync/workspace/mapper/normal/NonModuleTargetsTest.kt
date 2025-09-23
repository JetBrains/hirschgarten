package org.jetbrains.bazel.sync.workspace.mapper.normal

import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.info.BspTargetInfo
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

class NonModuleTargetsTest {
  private fun resolver(tmp: Path = Path.of("")): BazelPathsResolver = BazelPathsResolverMock.create(tmp)

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

  private fun target(id: String, kind: String): TargetInfo = TargetInfo.newBuilder().setId(id).setKind(kind).build()

  @Test
  fun `non-module executable targets included, dot-bazelbsp filtered`() = runBlocking {

    val pathsResolver = resolver()
    // Initialize telemetry for tests using mapper
    org.jetbrains.bazel.performance.telemetry.TelemetryManager.provideTelemetryManager(org.jetbrains.bazel.startup.IntellijTelemetryManager)
    val lps = LanguagePluginsService().also { it.registerDefaultPlugins(pathsResolver, DefaultJvmPackageResolver()) }
    val mapper = AspectBazelProjectMapper(lps, FeatureFlags(), pathsResolver, TargetTagsResolver())

    val binary = target("//tools:runme", "java_binary")
    val dot = target("//.bazelbsp:internal_task", "java_binary")

    val all = mapOf(Label.parse(binary.id) to binary, Label.parse(dot.id) to dot)
    val roots = setOf(Label.parse(binary.id))

    val ws = mapper.createProject(
      targets = all,
      rootTargets = roots,
      workspaceContext = workspace(),
      repoMapping = RepoMappingDisabled,
      hasError = false,
    )

    // BuildTargetCollection has separate non-module targets added
    val nonModules = ws.targets.nonModuleTargets
    val containsBinary = nonModules.any { it.id == Label.parse(binary.id) }
    val containsDot = nonModules.any { it.id == Label.parse(dot.id) }

    assertTrue("binary target imported as module should not be in non-module list", !containsBinary)
    assertTrue(".bazelbsp target should be filtered out", !containsDot)
  }
}
