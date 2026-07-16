package org.jetbrains.bazel.server.sync

import com.intellij.execution.process.OSProcessUtil.killProcess
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.bazelrunner.BazelCommand
import org.jetbrains.bazel.bazelrunner.BazelLog
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
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.languages.projectview.deriveInstrumentationFilterFromTargets
import org.jetbrains.bazel.server.BazelTestFileNames
import org.jetbrains.bazel.server.bep.AnalysisCacheInvalidationParser
import org.jetbrains.bazel.server.bep.BepBuildResult
import org.jetbrains.bazel.server.bep.BepReader
import org.jetbrains.bazel.server.bep.BepServer
import org.jetbrains.bazel.server.bep.analysisPhaseTimeMsOrNull
import org.jetbrains.bazel.server.bep.toActionCacheStats
import org.jetbrains.bazel.server.diagnostics.DiagnosticsService
import org.jetbrains.bsp.protocol.AnalysisDebugParams
import org.jetbrains.bsp.protocol.AnalysisDebugResult
import org.jetbrains.bsp.protocol.BazelInvocationContext
import org.jetbrains.bsp.protocol.BazelInvocationMetrics
import org.jetbrains.bsp.protocol.BazelTaskEventsHandler
import org.jetbrains.bsp.protocol.CompileParams
import org.jetbrains.bsp.protocol.CompileResult
import org.jetbrains.bsp.protocol.RunParams
import org.jetbrains.bsp.protocol.RunResult
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.bsp.protocol.TestParams
import org.jetbrains.bsp.protocol.TestResult
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.path.deleteExisting

@ApiStatus.Internal
class ExecuteService(
  private val project: Project,
  private val workspaceRoot: Path,
  private val taskEventsHandler: BazelTaskEventsHandler,
  private val bazelRunner: BazelRunner,
  private val projectView: ProjectView,
  private val bazelPathsResolver: BazelPathsResolver,
  private val rawProgressReporterProvider: RawProgressReporterProvider = RawProgressReporterProvider.current(),
) {
  private suspend fun runWithBepServer(
    command: BazelCommand,
    taskId: TaskId,
    context: BazelInvocationContext,
    pidDeferred: CompletableDeferred<Long?>? = null,
  ): BepBuildResult = rawProgressReporterProvider { rawProgressReporter ->
    coroutineScope {
      val eventFile: Path =
        Files.createTempFile("bazel-bep-output", null).toAbsolutePath()

      var bepReader: BepReader? = null
      try {
        command.useBes(eventFile)
        val executionDescriptor = command.buildExecutionDescriptor()
        val diagnosticsService = DiagnosticsService(workspaceRoot)
        val bepServer = BepServer(project, taskEventsHandler, diagnosticsService, taskId, bazelPathsResolver, rawProgressReporter)
        bepReader = BepReader(bepServer, eventFile)
        val bepReaderDeferred = async(Dispatchers.IO) {
          bepReader.start()
        }

        val bazelProcess = bazelRunner.runBazelCommand(
          executionDescriptor,
          taskId = taskId,
        )
        pidDeferred?.complete(bazelProcess.pid)
        val processResult = bazelProcess.waitAndGetResult()

        bepReader.bazelBuildFinished()
        bepReaderDeferred.await()

        bepServer.bepMetrics?.let { metrics ->
          BazelLog.write {
            println("Metrics:")
            println(metrics.toString())
          }
        }

        reportInvocationMetrics(context, bepServer, processResult)

        val bepOutput = bepServer.bepOutput
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

  /** Reports analysis-cache discards and action cache hit/miss stats for a finished invocation (FUS). */
  private fun reportInvocationMetrics(
    context: BazelInvocationContext,
    bepServer: BepServer,
    processResult: BazelProcessResult,
  ) {
    taskEventsHandler.onBazelInvocationMetrics(
      BazelInvocationMetrics(
        context = context,
        // BEP progress events are the primary source: Bazel routes its output through BEP when
        // --build_event_binary_path is set. Process streams are the fallback for output that arrives
        // before BEP starts, or in PTY mode where stderr is merged into stdout.
        analysisCacheInvalidation = bepServer.analysisCacheInvalidation
          ?: AnalysisCacheInvalidationParser.parse(processResult.stderrLines, processResult.stdoutLines),
        actionCacheStats = bepServer.bepMetrics?.toActionCacheStats(),
        analysisPhaseTimeMs = bepServer.bepMetrics?.analysisPhaseTimeMsOrNull(),
      ),
    )
  }

  suspend fun compile(params: CompileParams): CompileResult =
    if (params.targets.isNotEmpty()) {
      val result = build(params.targets, params.taskId, params.arguments ?: emptyList())
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
        val result = build(params.targets, params.taskId, debugFlags)
        result.bazelStatus
      }
      else {
        BazelStatus.FATAL_ERROR
      }
    return AnalysisDebugResult(statusCode)
  }

  suspend fun run(params: RunParams): RunResult = runImpl(params)

  private suspend fun runImpl(
    params: RunParams,
    additionalProgramArguments: List<String>? = null,
    additionalOptions: List<String>? = null,
  ): RunResult {
    val command =
      bazelRunner.buildBazelCommand(projectView) {
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
          ptyTermSize = project.service<PtyTerminalService>().ptyTermSize(params.taskId)
        }
      }
    val result = runWithBepServer(command, params.taskId, BazelInvocationContext.RUN, pidDeferred = params.pidDeferred)
    return RunResult(statusCode = result.processResult.bazelStatus, taskId = params.taskId)
  }

  suspend fun test(params: TestParams): TestResult {
    val targetsSpec = TargetCollection(params.targets, emptyList())
    val command =
      when (val instrumentationFilter = params.coverageInstrumentationFilter) {
        null -> bazelRunner.buildBazelCommand(projectView) { test() }

        else ->
          bazelRunner.buildBazelCommand(projectView) { coverage() }.also {
            it.options.add(BazelFlag.combinedReportLcov())
            if (projectView.deriveInstrumentationFilterFromTargets) {
              it.options.add(BazelFlag.instrumentationFilter(instrumentationFilter))
            }
          }
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
    command.options.add(BazelFlag.buildEventBinaryPathConversion(false))
    // Ensure all test xml, log, and lcov files are downloaded even when remote_download_outputs=minimal is set
    command.options.addAll(
      BazelTestFileNames.entries.map { BazelFlag.remoteDownloadRegex(it.filenameRegex) })
    with(command as HasMultipleTargets) {
      targets.addAll(targetsSpec.values)
      excludedTargets.addAll(targetsSpec.excludedValues)
    }

    // TODO: handle multiple targets
    val result = runWithBepServer(command, params.taskId, BazelInvocationContext.TEST)
    return TestResult(statusCode = result.processResult.bazelStatus)
  }

  private suspend fun build(
    allTargets: List<Label>,
    taskId: TaskId,
    additionalArguments: List<String> = emptyList(),
  ): BazelProcessResult {
    // you must compute all targets before passing them to bound
    val command =
      bazelRunner.buildBazelCommand(projectView) {
        build {
          options.addAll(additionalArguments)
          targets.addAll(allTargets)
          ptyTermSize = project.service<PtyTerminalService>().ptyTermSize(taskId)
        }
      }

    return runWithBepServer(command, taskId, BazelInvocationContext.BUILD).processResult
  }

  private fun ensureTestOutputStreamed(command: BazelCommand) {
    // Add to the beginning to make overriding possible
    command.options.addFirst(BazelFlag.testOutputStreamed())
  }

  suspend fun buildTargetsWithBep(
    targetsSpec: TargetCollection,
    extraFlags: List<String> = emptyList(),
    taskId: TaskId,
  ): BepBuildResult {
    val command =
      bazelRunner.buildBazelCommand(projectView) {
        build {
          options.addAll(extraFlags)
          targets.addAll(targetsSpec.values)
          excludedTargets.addAll(targetsSpec.excludedValues)
          ptyTermSize = project.service<PtyTerminalService>().ptyTermSize(taskId)
        }
      }

    return runWithBepServer(command, taskId, BazelInvocationContext.SYNC)
  }
}
