package org.jetbrains.bsp.bazel.server.sync

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.CleanCacheParams
import ch.epfl.scala.bsp4j.CleanCacheResult
import ch.epfl.scala.bsp4j.CompileParams
import ch.epfl.scala.bsp4j.CompileResult
import ch.epfl.scala.bsp4j.RunParams
import ch.epfl.scala.bsp4j.RunResult
import ch.epfl.scala.bsp4j.StatusCode
import ch.epfl.scala.bsp4j.TestParams
import ch.epfl.scala.bsp4j.TestResult
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode
import org.jetbrains.bazel.commons.label.Label
import org.jetbrains.bazel.commons.label.label
import org.jetbrains.bsp.bazel.bazelrunner.BazelProcessResult
import org.jetbrains.bsp.bazel.bazelrunner.BazelRunner
import org.jetbrains.bsp.bazel.bazelrunner.HasAdditionalBazelOptions
import org.jetbrains.bsp.bazel.bazelrunner.HasEnvironment
import org.jetbrains.bsp.bazel.bazelrunner.HasMultipleTargets
import org.jetbrains.bsp.bazel.bazelrunner.HasProgramArguments
import org.jetbrains.bsp.bazel.bazelrunner.params.BazelFlag
import org.jetbrains.bsp.bazel.logger.BspClientLogger
import org.jetbrains.bsp.bazel.server.bep.BepServer
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelBspCompilationManager
import org.jetbrains.bsp.bazel.server.bsp.managers.BepReader
import org.jetbrains.bsp.bazel.server.diagnostics.DiagnosticsService
import org.jetbrains.bsp.bazel.server.model.BspMappings
import org.jetbrains.bsp.bazel.server.model.Module
import org.jetbrains.bsp.bazel.server.model.Tag
import org.jetbrains.bsp.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bsp.bazel.server.sync.JvmDebugHelper.generateRunArguments
import org.jetbrains.bsp.bazel.server.sync.JvmDebugHelper.verifyDebugRequest
import org.jetbrains.bsp.bazel.workspacecontext.TargetsSpec
import org.jetbrains.bsp.bazel.workspacecontext.WorkspaceContextProvider
import org.jetbrains.bsp.protocol.AnalysisDebugParams
import org.jetbrains.bsp.protocol.AnalysisDebugResult
import org.jetbrains.bsp.protocol.BazelTestParamsData
import org.jetbrains.bsp.protocol.FeatureFlags
import org.jetbrains.bsp.protocol.MobileInstallParams
import org.jetbrains.bsp.protocol.MobileInstallResult
import org.jetbrains.bsp.protocol.MobileInstallStartType
import org.jetbrains.bsp.protocol.RunWithDebugParams
import org.jetbrains.bsp.protocol.TestWithDebugParams
import java.net.URI
import kotlin.io.path.Path
import kotlin.io.path.toPath

class ExecuteService(
  private val compilationManager: BazelBspCompilationManager,
  private val projectProvider: ProjectProvider,
  private val bazelRunner: BazelRunner,
  private val workspaceContextProvider: WorkspaceContextProvider,
  private val bspClientLogger: BspClientLogger,
  private val bazelPathsResolver: BazelPathsResolver,
  private val additionalBuildTargetsProvider: AdditionalAndroidBuildTargetsProvider,
  private val featureFlags: FeatureFlags,
) {
  private val gson = Gson()

  private fun <T> withBepServer(
    originId: String?,
    target: Label?,
    body: (BepReader) -> T,
  ): T {
    val diagnosticsService = DiagnosticsService(compilationManager.workspaceRoot)
    val server = BepServer(compilationManager.client, diagnosticsService, originId, target, bazelPathsResolver)
    val bepReader = BepReader(server)

    return runBlocking {
      val reader = bepReader.start(this)
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
      CompileResult(result.bspStatusCode).apply { originId = params.originId }
    } else {
      CompileResult(StatusCode.ERROR).apply { originId = params.originId }
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
    val requestedDebugType = JvmDebugType.fromDebugData(params.debug)
    val debugArguments = generateRunArguments(requestedDebugType)
    verifyDebugRequest(requestedDebugType, singleModule)

    return runImpl(cancelChecker, params.runParams, debugArguments)
  }

  private fun runImpl(
    cancelChecker: CancelChecker,
    params: RunParams,
    additionalProgramArguments: List<String>? = null,
  ): RunResult {
    val targets = listOf(params.target)
    val result = build(cancelChecker, targets, params.originId)
    if (result.isNotSuccess) {
      return RunResult(result.bspStatusCode).apply { originId = originId }
    }
    val command =
      bazelRunner.buildBazelCommand {
        run(params.target.label()) {
          options.add(BazelFlag.color(true))
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
    return RunResult(bazelProcessResult.bspStatusCode).apply { originId = originId }
  }

  fun test(cancelChecker: CancelChecker, params: TestParams): TestResult = testImpl(cancelChecker, params)

  /**
   * If `debugArguments` is empty, test task will be executed normally without any debugging options
   */
  fun testWithDebug(cancelChecker: CancelChecker, params: TestWithDebugParams): TestResult {
    val modules = selectModules(cancelChecker, params.testParams.targets)
    val singleModule = modules.singleOrResponseError(params.testParams.targets.first())
    val requestedDebugType = JvmDebugType.fromDebugData(params.debug)
    val debugArguments = generateRunArguments(requestedDebugType)
    verifyDebugRequest(requestedDebugType, singleModule)

    return testImpl(cancelChecker, params.testParams, debugArguments)
  }

  private fun testImpl(
    cancelChecker: CancelChecker,
    params: TestParams,
    additionalProgramArguments: List<String>? = emptyList(),
  ): TestResult {
    val targetsSpec = TargetsSpec(params.targets.map { it.label() }, emptyList())

    var bazelTestParamsData: BazelTestParamsData? = null
    try {
      if (params.dataKind == BazelTestParamsData.DATA_KIND) {
        bazelTestParamsData = gson.fromJson(params.data as JsonObject, BazelTestParamsData::class.java)
      }
    } catch (e: Exception) {
      bspClientLogger.warn("Failed to parse BazelTestParamsData: $e")
    }

    val command =
      when (bazelTestParamsData?.coverage) {
        true -> bazelRunner.buildBazelCommand { coverage() }
        else -> bazelRunner.buildBazelCommand { test() }
      }

    bazelTestParamsData?.additionalBazelParams?.let { additionalParams ->
      (command as HasAdditionalBazelOptions).additionalBazelOptions.addAll(additionalParams.split(" "))
    }

    bazelTestParamsData?.testFilter?.let { testFilter ->
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
      withBepServer(params.originId, params.targets.first().label()) { bepReader ->
        command.useBes(bepReader.eventFile.toPath().toAbsolutePath())
        bazelRunner
          .runBazelCommand(
            command,
            originId = params.originId,
            serverPidFuture = bepReader.serverPid,
          ).waitAndGetResult(cancelChecker, true)
      }

    return TestResult(result.bspStatusCode).apply {
      originId = originId
      data = result // TODO: why do we return the entire result? and no `dataKind`?
    }
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
    withBepServer(null, null) { bepReader ->
      val command =
        bazelRunner.buildBazelCommand {
          clean {
            useBes(bepReader.eventFile.toPath().toAbsolutePath())
          }
        }
      bazelRunner.runBazelCommand(command, serverPidFuture = bepReader.serverPid).waitAndGetResult(cancelChecker)
    }
    return CleanCacheResult(true)
  }

  private fun build(
    cancelChecker: CancelChecker,
    bspIds: List<BuildTargetIdentifier>,
    originId: String?,
    additionalArguments: List<String> = emptyList(),
  ): BazelProcessResult {
    val allTargets = bspIds + getAdditionalBuildTargets(cancelChecker, bspIds)
    // TODO: what if there's more than one target?
    //  (it was like this in now-deleted BazelBspCompilationManager.buildTargetsWithBep)
    return withBepServer(originId, bspIds.firstOrNull()?.label()) { bepReader ->
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
    val project = projectProvider.get(cancelChecker)
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
