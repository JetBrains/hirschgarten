package org.jetbrains.bsp.bazel.server

import ch.epfl.scala.bsp4j.InitializeBuildParams
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.jetbrains.bsp.bazel.bazelrunner.BazelInfoResolver
import org.jetbrains.bsp.bazel.bazelrunner.BazelRunner
import org.jetbrains.bsp.bazel.bazelrunner.utils.BazelInfo
import org.jetbrains.bsp.bazel.logger.BspClientLogger
import org.jetbrains.bsp.bazel.server.benchmark.TelemetryConfig
import org.jetbrains.bsp.bazel.server.benchmark.setupTelemetry
import org.jetbrains.bsp.bazel.server.bsp.BazelServices
import org.jetbrains.bsp.bazel.server.bsp.info.BspInfo
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelBspAspectsManager
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelBspCompilationManager
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelBspLanguageExtensionsGenerator
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelLabelExpander
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelToolchainManager
import org.jetbrains.bsp.bazel.server.bsp.utils.InternalAspectsResolver
import org.jetbrains.bsp.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bsp.bazel.server.sync.AdditionalAndroidBuildTargetsProvider
import org.jetbrains.bsp.bazel.server.sync.BazelProjectMapper
import org.jetbrains.bsp.bazel.server.sync.BspProjectMapper
import org.jetbrains.bsp.bazel.server.sync.ExecuteService
import org.jetbrains.bsp.bazel.server.sync.MavenCoordinatesResolver
import org.jetbrains.bsp.bazel.server.sync.ProjectProvider
import org.jetbrains.bsp.bazel.server.sync.ProjectResolver
import org.jetbrains.bsp.bazel.server.sync.ProjectSyncService
import org.jetbrains.bsp.bazel.server.sync.TargetInfoReader
import org.jetbrains.bsp.bazel.server.sync.TargetTagsResolver
import org.jetbrains.bsp.bazel.server.sync.firstPhase.FirstPhaseProjectResolver
import org.jetbrains.bsp.bazel.server.sync.firstPhase.FirstPhaseTargetToBspMapper
import org.jetbrains.bsp.bazel.server.sync.languages.LanguagePluginsService
import org.jetbrains.bsp.bazel.server.sync.languages.android.AndroidLanguagePlugin
import org.jetbrains.bsp.bazel.server.sync.languages.android.KotlinAndroidModulesMerger
import org.jetbrains.bsp.bazel.server.sync.languages.cpp.CppLanguagePlugin
import org.jetbrains.bsp.bazel.server.sync.languages.go.GoLanguagePlugin
import org.jetbrains.bsp.bazel.server.sync.languages.java.JavaLanguagePlugin
import org.jetbrains.bsp.bazel.server.sync.languages.java.JdkResolver
import org.jetbrains.bsp.bazel.server.sync.languages.java.JdkVersionResolver
import org.jetbrains.bsp.bazel.server.sync.languages.kotlin.KotlinLanguagePlugin
import org.jetbrains.bsp.bazel.server.sync.languages.python.PythonLanguagePlugin
import org.jetbrains.bsp.bazel.server.sync.languages.rust.RustLanguagePlugin
import org.jetbrains.bsp.bazel.server.sync.languages.scala.ScalaLanguagePlugin
import org.jetbrains.bsp.bazel.server.sync.languages.thrift.ThriftLanguagePlugin
import org.jetbrains.bsp.bazel.workspacecontext.WorkspaceContextProvider
import org.jetbrains.bsp.protocol.FeatureFlags
import org.jetbrains.bsp.protocol.InitializeBuildData
import java.nio.file.Path

class BazelBspServer(
  private val bspInfo: BspInfo,
  val workspaceContextProvider: WorkspaceContextProvider,
  val workspaceRoot: Path,
  private val telemetryConfig: TelemetryConfig,
) {
  private val gson = Gson()

  fun bspServerData(
    initializeBuildParams: InitializeBuildParams,
    bspClientLogger: BspClientLogger,
    bazelRunner: BazelRunner,
    compilationManager: BazelBspCompilationManager,
    bazelInfo: BazelInfo,
    workspaceContextProvider: WorkspaceContextProvider,
    bazelPathsResolver: BazelPathsResolver,
  ): BazelServices {
    val initializeBuildData =
      gson.fromJson(initializeBuildParams.data as? JsonObject, InitializeBuildData::class.java)
        ?: InitializeBuildData()

    val telemetryConfig =
      telemetryConfig.copy(
        bspClientLogger = bspClientLogger,
        openTelemetryEndpoint = initializeBuildData.openTelemetryEndpoint,
      )
    setupTelemetry(telemetryConfig)

    val languagePluginsService = createLanguagePluginsService(bazelPathsResolver, bspClientLogger)
    val featureFlags = initializeBuildData.featureFlags ?: FeatureFlags()
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
      ProjectSyncService(bspProjectMapper, firstPhaseTargetToBspMapper, projectProvider, initializeBuildParams.capabilities)
    val additionalBuildTargetsProvider = AdditionalAndroidBuildTargetsProvider(projectProvider)
    val executeService =
      ExecuteService(
        compilationManager = compilationManager,
        projectProvider = projectProvider,
        bazelRunner = bazelRunner,
        workspaceContextProvider = workspaceContextProvider,
        bspClientLogger = bspClientLogger,
        bazelPathsResolver = bazelPathsResolver,
        additionalBuildTargetsProvider = additionalBuildTargetsProvider,
        featureFlags = featureFlags,
      )

    return BazelServices(
      projectSyncService,
      executeService,
    )
  }

  fun createBazelInfo(bazelRunner: BazelRunner): BazelInfo {
    val bazelDataResolver = BazelInfoResolver(bazelRunner)
    return bazelDataResolver.resolveBazelInfo { }
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

  fun verifyBazelVersion(bazelRunner: BazelRunner) {
    val command = bazelRunner.buildBazelCommand { version {} }
    bazelRunner
      .runBazelCommand(command, serverPidFuture = null)
      .waitAndGetResult({}, true)
      .also {
        if (it.isNotSuccess) error("Querying Bazel version failed.\n${it.stderrLines.joinToString("\n")}")
      }
  }
}
