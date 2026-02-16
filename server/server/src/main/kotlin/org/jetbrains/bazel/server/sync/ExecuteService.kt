package org.jetbrains.bazel.server.sync

import com.intellij.execution.process.OSProcessUtil.killProcess
import com.intellij.openapi.project.Project
import com.intellij.platform.util.progress.reportRawProgress
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.jetbrains.bazel.bazelrunner.BazelCommand
import org.jetbrains.bazel.bazelrunner.BazelProcessResult
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.bazelrunner.HasAdditionalBazelOptions
import org.jetbrains.bazel.bazelrunner.HasEnvironment
import org.jetbrains.bazel.bazelrunner.HasMultipleTargets
import org.jetbrains.bazel.bazelrunner.HasProgramArguments
import org.jetbrains.bazel.bazelrunner.params.BazelFlag
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.commons.TargetCollection
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.bep.BepBuildResult
import org.jetbrains.bazel.server.bep.BepReader
import org.jetbrains.bazel.server.bep.BepServer
import org.jetbrains.bazel.server.diagnostics.DiagnosticsService
import org.jetbrains.bazel.server.sync.DebugHelper.generateRunArguments
import org.jetbrains.bazel.server.sync.DebugHelper.generateRunOptions
import org.jetbrains.bazel.ui.console.ConsoleService
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.AnalysisDebugParams
import org.jetbrains.bsp.protocol.AnalysisDebugResult
import org.jetbrains.bsp.protocol.BazelTaskEventsHandler
import org.jetbrains.bsp.protocol.CompileParams
import org.jetbrains.bsp.protocol.CompileResult
import org.jetbrains.bsp.protocol.DebugType
import org.jetbrains.bsp.protocol.RunParams
import org.jetbrains.bsp.protocol.RunResult
import org.jetbrains.bsp.protocol.RunWithDebugParams
import org.jetbrains.bsp.protocol.TestParams
import org.jetbrains.bsp.protocol.TestResult
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.path.deleteExisting

class ExecuteService(
  private val project: Project,
  private val workspaceRoot: Path,
  private val taskEventsHandler: BazelTaskEventsHandler,
  private val bazelRunner: BazelRunner,
  private val workspaceContext: WorkspaceContext,
  private val bazelPathsResolver: BazelPathsResolver,
) {
  private suspend fun runWithBepServer(
    command: BazelCommand,
    originId: String?,
    pidDeferred: CompletableDeferred<Long?>? = null,
    shouldLogInvocation: Boolean = true,
  ): BepBuildResult = coroutineScope {
    reportRawProgress { rawProgressReporter ->
      val eventFile: Path =
        Files.createTempFile("bazel-bep-output", null).toAbsolutePath()

      var bepReader: BepReader? = null
      try {
        command.useBes(eventFile)
        val executionDescriptor = command.buildExecutionDescriptor()
        val diagnosticsService = DiagnosticsService(workspaceRoot)
        val server = BepServer(taskEventsHandler, diagnosticsService, originId, bazelPathsResolver, rawProgressReporter)
        bepReader = BepReader(server, eventFile)
        val bepReaderDeferred = async(Dispatchers.IO) {
          bepReader.start()
        }

        val processResult: BazelProcessResult =
          bazelRunner.runBazelCommand(executionDescriptor, originId = originId, shouldLogInvocation = shouldLogInvocation)
            .also { bazelProcess -> pidDeferred?.complete(bazelProcess.pid) }
            .waitAndGetResult()
        bepReader.bazelBuildFinished()
        bepReaderDeferred.await()

        val bepOutput = bepReader.bepServer.bepOutput
        BepBuildResult(processResult, bepOutput)
      }
      catch (e: CancellationException) {
        if (BazelFeatureFlags.killServerOnCancel) {
          val serverPid = bepReader?.serverPid?.get() ?: 0
          if (serverPid > 0) {
            killProcess(serverPid.toInt())
          }
        }
        throw e
      }
      finally {
        eventFile.deleteExisting()
      }
    }
  }

  suspend fun compile(params: CompileParams): CompileResult =
    if (params.targets.isNotEmpty()) {
      val result = build(params.targets, params.originId, params.arguments ?: emptyList())
      CompileResult(statusCode = result.bazelStatus)
    }
    else {
      CompileResult(statusCode = BazelStatus.FATAL_ERROR)
    }

  suspend fun analysisDebug(params: AnalysisDebugParams): AnalysisDebugResult {
    val statusCode =
      if (params.targets.isNotEmpty()) {
        val debugFlags =
          listOf(BazelFlag.noBuild(), BazelFlag.starlarkDebug(), BazelFlag.starlarkDebugPort(params.port))
        val result = build(params.targets, params.originId, debugFlags)
        result.bazelStatus
      }
      else {
        BazelStatus.FATAL_ERROR
      }
    return AnalysisDebugResult(statusCode)
  }

  suspend fun run(params: RunParams): RunResult = runImpl(params)

  /**
   * If `debugArguments` is empty, run task will be executed normally without any debugging options
   */
  suspend fun runWithDebug(params: RunWithDebugParams): RunResult {
    val requestedDebugType = params.debug
    val debugArguments = generateRunArguments(requestedDebugType)
    val debugOptions = generateRunOptions(requestedDebugType)
    return runImpl(params.runParams, debugArguments, debugOptions)
  }

  private suspend fun runImpl(
    params: RunParams,
    additionalProgramArguments: List<String>? = null,
    additionalOptions: List<String>? = null,
  ): RunResult {
    val command =
      bazelRunner.buildBazelCommand(workspaceContext) {
        run(params.target) {
          options.add(BazelFlag.color(true))
          if (!params.checkVisibility) {
            options.add(BazelFlag.checkVisibility(false))
          }
          additionalOptions?.let { options.addAll(it) }
          additionalProgramArguments?.let { programArguments.addAll(it) }
          params.environmentVariables?.let { environment.putAll(it) }
          params.arguments?.let { programArguments.addAll(it) }
          params.additionalBazelParams?.let { additionalBazelOptions.addAll(it.trim().split(" ")) }
          ptyTermSize = ConsoleService.getInstance(project).ptyTermSize(params.originId)
        }
      }
    val result = runWithBepServer(command, params.originId, pidDeferred = params.pidDeferred)
    return RunResult(statusCode = result.processResult.bazelStatus, originId = params.originId)
  }

  /**
   * If `debugArguments` is empty, test task will be executed normally without any debugging options
   */
  suspend fun testWithDebug(params: TestParams): TestResult {
    val debugArguments =
      if (params.debug != null) {
        val requestedDebugType = params.debug
        generateRunArguments(requestedDebugType)
      }
      else {
        null
      }

    return testImpl(params, debugArguments)
  }

  private suspend fun testImpl(params: TestParams, additionalProgramArguments: List<String>?): TestResult {
    val targetsSpec = TargetCollection(params.targets, emptyList())
    // Use the already available workspaceContext
    val command =
      when (val instrumentationFilter = params.coverageInstrumentationFilter) {
        null -> bazelRunner.buildBazelCommand(workspaceContext) { test() }

        else ->
          bazelRunner.buildBazelCommand(workspaceContext) { coverage() }.also {
            it.options.add(BazelFlag.combinedReportLcov())
            if (workspaceContext.deriveInstrumentationFilterFromTargets) {
              it.options.add(BazelFlag.instrumentationFilter(instrumentationFilter))
            }
          }
      }

    val debugType = params.debug

    if (debugType is DebugType.JDWP) {
      command.options.add(BazelFlag.javaTestDebug())
    }

    if (debugType is DebugType.GoDlv) {
      command.options.addAll(
        listOf(
          BazelFlag.runUnder(
            "dlv --listen=127.0.0.1:${debugType.port} --headless=true --api-version=2 --check-go-version=false --only-same-user=false exec",
          ),
          "--compilation_mode=dbg",
          "--dynamic_mode=off",
        ),
      )
    }

    params.additionalBazelParams?.let { additionalParams ->
      (command as HasAdditionalBazelOptions).additionalBazelOptions.addAll(additionalParams.split(" "))
    }

    if (params.useJetBrainsTestRunner) {
      // Ensure streamed test output for live UI in IDE
      ensureTestOutputStreamed(command)
    }

    params.testFilter?.let { testFilter ->
      command.options.add(BazelFlag.testFilter(testFilter))
    }

    params.environmentVariables?.let { (command as HasEnvironment).environment.putAll(it) }
    params.arguments?.let { (command as HasProgramArguments).programArguments.addAll(it) }
    additionalProgramArguments?.let { (command as HasProgramArguments).programArguments.addAll(it) }
    command.options.add(BazelFlag.buildEventBinaryPathConversion(false))
    with(command as HasMultipleTargets) {
      targets.addAll(targetsSpec.values)
      excludedTargets.addAll(targetsSpec.excludedValues)
    }

    // TODO: handle multiple targets
    val result = runWithBepServer(command, params.originId)
    return TestResult(statusCode = result.processResult.bazelStatus, originId = params.originId)
  }

  private suspend fun build(
    allTargets: List<Label>,
    originId: String,
    additionalArguments: List<String> = emptyList(),
  ): BazelProcessResult {
    // you must compute all targets  before passing them to bound
    val command =
      bazelRunner.buildBazelCommand(workspaceContext) {
        build {
          options.addAll(additionalArguments)
          targets.addAll(allTargets)
          ptyTermSize = ConsoleService.getInstance(project).ptyTermSize(originId)
        }
      }

    return runWithBepServer(command, originId).processResult
  }

  private fun ensureTestOutputStreamed(command: BazelCommand) {
    // Add to the beginning to make overriding possible
    command.options.addFirst(BazelFlag.testOutputStreamed())
  }

  suspend fun buildTargetsWithBep(
    targetsSpec: TargetCollection,
    extraFlags: List<String> = emptyList(),
    originId: String?,
    shouldLogInvocation: Boolean,
  ): BepBuildResult {
    val command =
      bazelRunner.buildBazelCommand(workspaceContext) {
        build {
          options.addAll(extraFlags)
          targets.addAll(targetsSpec.values)
          excludedTargets.addAll(targetsSpec.excludedValues)
          ptyTermSize = originId?.let { ConsoleService.getInstance(project).ptyTermSize(originId) }
        }
      }

    return runWithBepServer(command, originId, shouldLogInvocation = shouldLogInvocation)
  }
}
