package org.jetbrains.bazel.server.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode
import org.jetbrains.bazel.bazelrunner.BazelProcessResult
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.bazelrunner.HasAdditionalBazelOptions
import org.jetbrains.bazel.bazelrunner.HasEnvironment
import org.jetbrains.bazel.bazelrunner.HasMultipleTargets
import org.jetbrains.bazel.bazelrunner.HasProgramArguments
import org.jetbrains.bazel.bazelrunner.params.BazelFlag
import org.jetbrains.bazel.label.label
import org.jetbrains.bazel.server.bep.BepServer
import org.jetbrains.bazel.server.bsp.managers.BazelBspCompilationManager
import org.jetbrains.bazel.server.bsp.managers.BepReader
import org.jetbrains.bazel.server.diagnostics.DiagnosticsService
import org.jetbrains.bazel.server.model.AspectSyncProject
import org.jetbrains.bazel.server.model.BspMappings
import org.jetbrains.bazel.server.model.Module
import org.jetbrains.bazel.server.model.Tag
import org.jetbrains.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bazel.server.sync.DebugHelper.buildBeforeRun
import org.jetbrains.bazel.server.sync.DebugHelper.generateRunArguments
import org.jetbrains.bazel.server.sync.DebugHelper.generateRunOptions
import org.jetbrains.bazel.server.sync.DebugHelper.verifyDebugRequest
import org.jetbrains.bazel.workspacecontext.TargetsSpec
import org.jetbrains.bazel.workspacecontext.WorkspaceContextProvider
import org.jetbrains.bsp.protocol.AnalysisDebugParams
import org.jetbrains.bsp.protocol.AnalysisDebugResult
import org.jetbrains.bsp.protocol.BuildTargetIdentifier
import org.jetbrains.bsp.protocol.CleanCacheParams
import org.jetbrains.bsp.protocol.CleanCacheResult
import org.jetbrains.bsp.protocol.CompileParams
import org.jetbrains.bsp.protocol.CompileResult
import org.jetbrains.bsp.protocol.FeatureFlags
import org.jetbrains.bsp.protocol.MobileInstallParams
import org.jetbrains.bsp.protocol.MobileInstallResult
import org.jetbrains.bsp.protocol.MobileInstallStartType
import org.jetbrains.bsp.protocol.RunParams
import org.jetbrains.bsp.protocol.RunResult
import org.jetbrains.bsp.protocol.RunWithDebugParams
import org.jetbrains.bsp.protocol.StatusCode
import org.jetbrains.bsp.protocol.TestParams
import org.jetbrains.bsp.protocol.TestResult
import java.net.URI
import kotlin.io.path.Path
import kotlin.io.path.toPath

class ExecuteService(
  private val compilationManager: BazelBspCompilationManager,
  private val projectProvider: ProjectProvider,
  private val bazelRunner: BazelRunner,
  private val workspaceContextProvider: WorkspaceContextProvider,
  private val bazelPathsResolver: BazelPathsResolver,
  private val additionalBuildTargetsProvider: AdditionalAndroidBuildTargetsProvider,
  private val featureFlags: FeatureFlags,
) {
  private fun <T> withBepServer(originId: String?, body: (BepReader) -> T): T {
    val diagnosticsService = DiagnosticsService(compilationManager.workspaceRoot)
    val server = BepServer(compilationManager.client, diagnosticsService, originId, bazelPathsResolver)
    val bepReader = BepReader(server)

    return runBlocking {
      val reader =
        async(Dispatchers.Default) {
          bepReader.start()
        }
      try {
        return@runBlocking body(bepReader)
      } finally {
        bepReader.finishBuild()
        reader.await()
      }
    }
  }

  fun compile(cancelChecker: CancelChecker, params: CompileParams): CompileResult =
    if (params.targets.isNotEmpty()) {
      val result = build(cancelChecker, params.targets, params.originId, params.arguments ?: emptyList())
      CompileResult(statusCode = result.bspStatusCode, originId = params.originId)
    } else {
      CompileResult(statusCode = StatusCode.ERROR, originId = params.originId)
    }

  fun analysisDebug(cancelChecker: CancelChecker, params: AnalysisDebugParams): AnalysisDebugResult {
    val statusCode =
      if (params.targets.isNotEmpty()) {
        val debugFlags =
          listOf(BazelFlag.noBuild(), BazelFlag.starlarkDebug(), BazelFlag.starlarkDebugPort(params.port))
        val result = build(cancelChecker, params.targets, params.originId, debugFlags)
        result.bspStatusCode
      } else {
        StatusCode.ERROR
      }
    return AnalysisDebugResult(statusCode)
  }

  fun run(cancelChecker: CancelChecker, params: RunParams): RunResult = runImpl(cancelChecker, params)

  /**
   * If `debugArguments` is empty, run task will be executed normally without any debugging options
   */
  fun runWithDebug(cancelChecker: CancelChecker, params: RunWithDebugParams): RunResult {
    val modules = selectModules(cancelChecker, listOf(params.runParams.target))
    val singleModule = modules.singleOrResponseError(params.runParams.target)
    val requestedDebugType = DebugType.fromDebugData(params.debug)
    val debugArguments = generateRunArguments(requestedDebugType)
    val debugOptions = generateRunOptions(requestedDebugType)
    val buildBeforeRun = buildBeforeRun(requestedDebugType)
    verifyDebugRequest(requestedDebugType, singleModule)

    return runImpl(cancelChecker, params.runParams, debugArguments, debugOptions, buildBeforeRun)
  }

  private fun runImpl(
    cancelChecker: CancelChecker,
    params: RunParams,
    additionalProgramArguments: List<String>? = null,
    additionalOptions: List<String>? = null,
    buildBeforeRun: Boolean = true,
  ): RunResult {
    if (buildBeforeRun) {
      val targets = listOf(params.target)
      val result = build(cancelChecker, targets, params.originId)
      if (result.isNotSuccess) {
        return RunResult(statusCode = result.bspStatusCode, originId = params.originId)
      }
    }
    val command =
      bazelRunner.buildBazelCommand {
        run(params.target.label()) {
          options.add(BazelFlag.color(true))
          additionalOptions?.let { options.addAll(it) }
          additionalProgramArguments?.let { programArguments.addAll(it) }
          params.environmentVariables?.let { environment.putAll(it) }
          params.workingDirectory?.let { workingDirectory = Path(it) }
          params.arguments?.let { programArguments.addAll(it) }
        }
      }
    val bazelProcessResult =
      bazelRunner
        .runBazelCommand(
          command,
          originId = params.originId,
          serverPidFuture = null,
        ).waitAndGetResult(cancelChecker)
    return RunResult(statusCode = bazelProcessResult.bspStatusCode, originId = params.originId)
  }

  /**
   * If `debugArguments` is empty, test task will be executed normally without any debugging options
   */
  fun testWithDebug(cancelChecker: CancelChecker, params: TestParams): TestResult {
    val modules = selectModules(cancelChecker, params.targets)
    val singleModule = modules.singleOrResponseError(params.targets.first())
    val requestedDebugType = DebugType.fromDebugData(params.debug)
    val debugArguments = generateRunArguments(requestedDebugType)
    verifyDebugRequest(requestedDebugType, singleModule)

    return testImpl(cancelChecker, params, debugArguments)
  }

  private fun testImpl(
    cancelChecker: CancelChecker,
    params: TestParams,
    additionalProgramArguments: List<String>? = emptyList(),
  ): TestResult {
    val targetsSpec = TargetsSpec(params.targets.map { it.label() }, emptyList())

    val command =
      when (params.coverage) {
        true -> bazelRunner.buildBazelCommand { coverage() }
        else -> bazelRunner.buildBazelCommand { test() }
      }

    params.additionalBazelParams?.let { additionalParams ->
      (command as HasAdditionalBazelOptions).additionalBazelOptions.addAll(additionalParams.split(" "))
    }

    params.testFilter?.let { testFilter ->
      command.options.add(BazelFlag.testFilter(testFilter))
    }

    params.environmentVariables?.let { (command as HasEnvironment).environment.putAll(it) }
    params.arguments?.let { (command as HasProgramArguments).programArguments.addAll(it) }
    additionalProgramArguments?.let { (command as HasProgramArguments).programArguments.addAll(it) }
    command.options.add(BazelFlag.color(true))
    command.options.add(BazelFlag.buildEventBinaryPathConversion(false))
    (command as HasMultipleTargets).addTargetsFromSpec(targetsSpec)

    // TODO: handle multiple targets
    val result =
      withBepServer(params.originId) { bepReader ->
        command.useBes(bepReader.eventFile.toPath().toAbsolutePath())
        bazelRunner
          .runBazelCommand(
            command,
            originId = params.originId,
            serverPidFuture = bepReader.serverPid,
          ).waitAndGetResult(cancelChecker, true)
      }

    return TestResult(statusCode = result.bspStatusCode, originId = params.originId)
  }

  fun mobileInstall(cancelChecker: CancelChecker, params: MobileInstallParams): MobileInstallResult {
    val startType =
      when (params.startType) {
        MobileInstallStartType.NO -> "no"
        MobileInstallStartType.COLD -> "cold"
        MobileInstallStartType.WARM -> "warm"
        MobileInstallStartType.DEBUG -> "debug"
      }

    val command =
      bazelRunner.buildBazelCommand {
        mobileInstall(params.target.label()) {
          options.add(BazelFlag.device(params.targetDeviceSerialNumber))
          options.add(BazelFlag.start(startType))
          params.adbPath?.let { adbPath ->
            // TODO: move safeCastToURI to a common target and use it here
            options.add(BazelFlag.adb(URI.create(adbPath).toPath().toString()))
          }
          options.add(BazelFlag.color(true))
        }
      }

    val bazelProcessResult =
      bazelRunner.runBazelCommand(command, originId = params.originId, serverPidFuture = null).waitAndGetResult(cancelChecker)
    return MobileInstallResult(bazelProcessResult.bspStatusCode, params.originId)
  }

  @Suppress("UNUSED_PARAMETER") // params is used by BspRequestsRunner.handleRequest
  fun clean(cancelChecker: CancelChecker, params: CleanCacheParams?): CleanCacheResult {
    withBepServer(null) { bepReader ->
      val command =
        bazelRunner.buildBazelCommand {
          clean {
            useBes(bepReader.eventFile.toPath().toAbsolutePath())
          }
        }
      bazelRunner.runBazelCommand(command, serverPidFuture = bepReader.serverPid).waitAndGetResult(cancelChecker)
    }
    return CleanCacheResult(cleaned = true)
  }

  private fun build(
    cancelChecker: CancelChecker,
    bspIds: List<BuildTargetIdentifier>,
    originId: String?,
    additionalArguments: List<String> = emptyList(),
  ): BazelProcessResult {
    val allTargets = bspIds + getAdditionalBuildTargets(cancelChecker, bspIds)
    return withBepServer(originId) { bepReader ->
      val command =
        bazelRunner.buildBazelCommand {
          build {
            options.addAll(additionalArguments)
            targets.addAll(allTargets.map { it.label() })
            useBes(bepReader.eventFile.toPath().toAbsolutePath())
          }
        }
      bazelRunner
        .runBazelCommand(command, originId = originId, serverPidFuture = bepReader.serverPid)
        .waitAndGetResult(cancelChecker, true)
    }
  }

  private fun getAdditionalBuildTargets(cancelChecker: CancelChecker, bspIds: List<BuildTargetIdentifier>): List<BuildTargetIdentifier> =
    if (featureFlags.isAndroidSupportEnabled) {
      additionalBuildTargetsProvider.getAdditionalBuildTargets(cancelChecker, bspIds)
    } else {
      emptyList()
    }

  private fun selectModules(cancelChecker: CancelChecker, targets: List<BuildTargetIdentifier>): List<Module> {
    val project = projectProvider.get(cancelChecker) as? AspectSyncProject ?: return emptyList()
    val modules = BspMappings.getModules(project, targets)
    val ignoreManualTag = targets.size == 1
    return modules.filter { isBuildable(it, ignoreManualTag) }
  }

  private fun isBuildable(m: Module, ignoreManualTag: Boolean = true): Boolean =
    !m.isSynthetic && !m.tags.contains(Tag.NO_BUILD) && (ignoreManualTag || isBuildableIfManual(m))

  private fun isBuildableIfManual(m: Module): Boolean =
    (
      !m.tags.contains(Tag.MANUAL) ||
        workspaceContextProvider.currentWorkspaceContext().allowManualTargetsSync.value
    )
}

private fun <T> List<T>.singleOrResponseError(requestedTarget: BuildTargetIdentifier): T =
  when {
    this.isEmpty() -> throwResponseError("No supported target found for ${requestedTarget.uri}")
    this.size == 1 -> this.single()
    else -> throwResponseError("More than one supported target found for ${requestedTarget.uri}")
  }

private fun throwResponseError(message: String, data: Any? = null): Nothing =
  throw ResponseErrorException(
    ResponseError(ResponseErrorCode.InvalidRequest, message, data),
  )
