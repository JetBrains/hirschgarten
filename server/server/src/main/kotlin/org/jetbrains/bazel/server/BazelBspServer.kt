package org.jetbrains.bazel.server

import org.jetbrains.bazel.bazelrunner.BazelInfoResolver
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.bazelrunner.utils.BazelInfo
import org.jetbrains.bazel.logger.BspClientLogger
import org.jetbrains.bazel.server.bsp.BazelServices
import org.jetbrains.bazel.server.bsp.info.BspInfo
import org.jetbrains.bazel.server.bsp.managers.BazelBspAspectsManager
import org.jetbrains.bazel.server.bsp.managers.BazelBspCompilationManager
import org.jetbrains.bazel.server.bsp.managers.BazelBspLanguageExtensionsGenerator
import org.jetbrains.bazel.server.bsp.managers.BazelLabelExpander
import org.jetbrains.bazel.server.bsp.managers.BazelToolchainManager
import org.jetbrains.bazel.server.bsp.utils.InternalAspectsResolver
import org.jetbrains.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bazel.server.sync.AdditionalAndroidBuildTargetsProvider
import org.jetbrains.bazel.server.sync.BazelProjectMapper
import org.jetbrains.bazel.server.sync.BspProjectMapper
import org.jetbrains.bazel.server.sync.ExecuteService
import org.jetbrains.bazel.server.sync.MavenCoordinatesResolver
import org.jetbrains.bazel.server.sync.ProjectProvider
import org.jetbrains.bazel.server.sync.ProjectResolver
import org.jetbrains.bazel.server.sync.ProjectSyncService
import org.jetbrains.bazel.server.sync.TargetInfoReader
import org.jetbrains.bazel.server.sync.TargetTagsResolver
import org.jetbrains.bazel.server.sync.firstPhase.FirstPhaseProjectResolver
import org.jetbrains.bazel.server.sync.firstPhase.FirstPhaseTargetToBspMapper
import org.jetbrains.bazel.server.sync.languages.LanguagePluginsService
import org.jetbrains.bazel.server.sync.languages.android.AndroidLanguagePlugin
import org.jetbrains.bazel.server.sync.languages.android.KotlinAndroidModulesMerger
import org.jetbrains.bazel.server.sync.languages.cpp.CppLanguagePlugin
import org.jetbrains.bazel.server.sync.languages.go.GoLanguagePlugin
import org.jetbrains.bazel.server.sync.languages.java.JavaLanguagePlugin
import org.jetbrains.bazel.server.sync.languages.java.JdkResolver
import org.jetbrains.bazel.server.sync.languages.java.JdkVersionResolver
import org.jetbrains.bazel.server.sync.languages.kotlin.KotlinLanguagePlugin
import org.jetbrains.bazel.server.sync.languages.python.PythonLanguagePlugin
import org.jetbrains.bazel.server.sync.languages.rust.RustLanguagePlugin
import org.jetbrains.bazel.server.sync.languages.scala.ScalaLanguagePlugin
import org.jetbrains.bazel.server.sync.languages.thrift.ThriftLanguagePlugin
import org.jetbrains.bazel.workspacecontext.WorkspaceContextProvider
import org.jetbrains.bsp.protocol.FeatureFlags
import org.jetbrains.bsp.protocol.InitializeBuildParams
import java.nio.file.Path

class BazelBspServer(
  private val bspInfo: BspInfo,
  val workspaceContextProvider: WorkspaceContextProvider,
  val workspaceRoot: Path,
) {
  fun bspServerData(
    initializeBuildParams: InitializeBuildParams,
    bspClientLogger: BspClientLogger,
    bazelRunner: BazelRunner,
    compilationManager: BazelBspCompilationManager,
    bazelInfo: BazelInfo,
    workspaceContextProvider: WorkspaceContextProvider,
    bazelPathsResolver: BazelPathsResolver,
  ): BazelServices {
    val languagePluginsService = createLanguagePluginsService(bazelPathsResolver, bspClientLogger)
    val featureFlags = initializeBuildParams.featureFlags
    val projectProvider =
      createProjectProvider(
        bspInfo = bspInfo,
        bazelInfo = bazelInfo,
        workspaceContextProvider = workspaceContextProvider,
        bazelRunner = bazelRunner,
        languagePluginsService = languagePluginsService,
        bazelPathsResolver = bazelPathsResolver,
        compilationManager = compilationManager,
        bspClientLogger = bspClientLogger,
        featureFlags = featureFlags,
      )
    val bspProjectMapper =
      BspProjectMapper(
        languagePluginsService = languagePluginsService,
        workspaceContextProvider = workspaceContextProvider,
        bazelPathsResolver = bazelPathsResolver,
        bazelRunner = bazelRunner,
        bspInfo = bspInfo,
      )
    val firstPhaseTargetToBspMapper = FirstPhaseTargetToBspMapper(workspaceContextProvider, workspaceRoot)
    val projectSyncService =
      ProjectSyncService(bspProjectMapper, firstPhaseTargetToBspMapper, projectProvider, bazelInfo)
    val additionalBuildTargetsProvider = AdditionalAndroidBuildTargetsProvider(projectProvider)
    val executeService =
      ExecuteService(
        compilationManager = compilationManager,
        projectProvider = projectProvider,
        bazelRunner = bazelRunner,
        workspaceContextProvider = workspaceContextProvider,
        bazelPathsResolver = bazelPathsResolver,
        additionalBuildTargetsProvider = additionalBuildTargetsProvider,
        featureFlags = featureFlags,
      )

    return BazelServices(
      projectSyncService,
      executeService,
    )
  }

  suspend fun createBazelInfo(bazelRunner: BazelRunner): BazelInfo {
    val bazelDataResolver = BazelInfoResolver(bazelRunner)
    return bazelDataResolver.resolveBazelInfo()
  }

  private fun createLanguagePluginsService(
    bazelPathsResolver: BazelPathsResolver,
    bspClientLogger: BspClientLogger,
  ): LanguagePluginsService {
    val jdkResolver = JdkResolver(bazelPathsResolver, JdkVersionResolver())
    val javaLanguagePlugin = JavaLanguagePlugin(workspaceContextProvider, bazelPathsResolver, jdkResolver)
    val scalaLanguagePlugin = ScalaLanguagePlugin(javaLanguagePlugin, bazelPathsResolver)
    val cppLanguagePlugin = CppLanguagePlugin(bazelPathsResolver)
    val kotlinLanguagePlugin = KotlinLanguagePlugin(javaLanguagePlugin, bazelPathsResolver)
    val thriftLanguagePlugin = ThriftLanguagePlugin(bazelPathsResolver)
    val pythonLanguagePlugin = PythonLanguagePlugin(bazelPathsResolver)
    val rustLanguagePlugin = RustLanguagePlugin(bazelPathsResolver)
    val androidLanguagePlugin =
      AndroidLanguagePlugin(workspaceContextProvider, javaLanguagePlugin, kotlinLanguagePlugin, bazelPathsResolver)
    val goLanguagePlugin = GoLanguagePlugin(bazelPathsResolver, bspClientLogger)

    return LanguagePluginsService(
      scalaLanguagePlugin,
      javaLanguagePlugin,
      cppLanguagePlugin,
      kotlinLanguagePlugin,
      thriftLanguagePlugin,
      pythonLanguagePlugin,
      rustLanguagePlugin,
      androidLanguagePlugin,
      goLanguagePlugin,
    )
  }

  private fun createProjectProvider(
    bspInfo: BspInfo,
    bazelInfo: BazelInfo,
    workspaceContextProvider: WorkspaceContextProvider,
    bazelRunner: BazelRunner,
    languagePluginsService: LanguagePluginsService,
    bazelPathsResolver: BazelPathsResolver,
    compilationManager: BazelBspCompilationManager,
    bspClientLogger: BspClientLogger,
    featureFlags: FeatureFlags,
  ): ProjectProvider {
    val aspectsResolver =
      InternalAspectsResolver(
        bspInfo = bspInfo,
        bazelRelease = bazelInfo.release,
        shouldUseInjectRepository = bazelInfo.shouldUseInjectRepository(),
      )

    val bazelBspAspectsManager =
      BazelBspAspectsManager(
        bazelBspCompilationManager = compilationManager,
        aspectsResolver = aspectsResolver,
        workspaceContextProvider = workspaceContextProvider,
        featureFlags = featureFlags,
        bazelRelease = bazelInfo.release,
      )
    val bazelToolchainManager = BazelToolchainManager(bazelRunner, featureFlags)
    val bazelBspLanguageExtensionsGenerator = BazelBspLanguageExtensionsGenerator(aspectsResolver)
    val bazelLabelExpander = BazelLabelExpander(bazelRunner)
    val targetTagsResolver = TargetTagsResolver()
    val mavenCoordinatesResolver = MavenCoordinatesResolver()
    val kotlinAndroidModulesMerger = KotlinAndroidModulesMerger(featureFlags)
    val bazelProjectMapper =
      BazelProjectMapper(
        languagePluginsService,
        bazelPathsResolver,
        targetTagsResolver,
        mavenCoordinatesResolver,
        kotlinAndroidModulesMerger,
        bspClientLogger,
        featureFlags,
      )
    val targetInfoReader = TargetInfoReader(bspClientLogger)

    val projectResolver =
      ProjectResolver(
        bazelBspAspectsManager = bazelBspAspectsManager,
        bazelToolchainManager = bazelToolchainManager,
        bazelBspLanguageExtensionsGenerator = bazelBspLanguageExtensionsGenerator,
        bazelLabelExpander = bazelLabelExpander,
        workspaceContextProvider = workspaceContextProvider,
        bazelProjectMapper = bazelProjectMapper,
        targetInfoReader = targetInfoReader,
        bazelInfo = bazelInfo,
        bazelRunner = bazelRunner,
        bazelPathsResolver = bazelPathsResolver,
        bspClientLogger = bspClientLogger,
        featureFlags = featureFlags,
      )
    val firstPhaseProjectResolver =
      FirstPhaseProjectResolver(
        workspaceRoot = workspaceRoot,
        bazelRunner = bazelRunner,
        workspaceContextProvider = workspaceContextProvider,
        bazelInfo = bazelInfo,
        bspClientLogger = bspClientLogger,
      )
    return ProjectProvider(projectResolver, firstPhaseProjectResolver)
  }

  suspend fun verifyBazelVersion(bazelRunner: BazelRunner) {
    val command = bazelRunner.buildBazelCommand { version {} }
    bazelRunner
      .runBazelCommand(command, serverPidFuture = null)
      .waitAndGetResult(true)
      .also {
        if (it.isNotSuccess) error("Querying Bazel version failed.\n${it.stderrLines.joinToString("\n")}")
      }
  }
}
