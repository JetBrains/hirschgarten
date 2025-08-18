package org.jetbrains.bazel.run.commandLine

import com.intellij.coverage.CoverageExecutor
import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.run.BazelCommandLineStateBase
import org.jetbrains.bazel.run.BazelProcessHandler
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.state.AbstractGenericTestState
import org.jetbrains.bazel.run.task.BazelTestTaskListener
import org.jetbrains.bazel.sync.workspace.BazelWorkspaceResolveService
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.taskEvents.BazelTaskListener
import org.jetbrains.bazel.utils.filterPathsThatDontContainEachOther2
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.TestParams
import java.nio.file.Path

class BazelTestCommandLineState(environment: ExecutionEnvironment, val state: AbstractGenericTestState<*>) :
  BazelCommandLineStateBase(environment) {
  var coverageReportListener: ((Path) -> Unit)? = null

  private val configuration = environment.runProfile as BazelRunConfiguration

  override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult = executeWithTestConsole(executor)

  override fun createAndAddTaskListener(handler: BazelProcessHandler): BazelTaskListener =
    BazelTestTaskListener(handler, coverageReportListener)

  override suspend fun startBsp(
    server: JoinedBuildServer,
    pidDeferred: CompletableDeferred<Long?>,
    handler: BazelProcessHandler,
  ) {
    if (configuration.targets.isEmpty()) {
      throw ExecutionException(BazelPluginBundle.message("bsp.run.error.cannotRun"))
    }

    val coverageInstrumentationFilter =
      if (environment.executor.id == CoverageExecutor.EXECUTOR_ID) {
        getCoverageInstrumentationFilter(configuration.project)
      } else {
        null
      }

    val targets = configuration.targets
    // TODO: add pidDeferred to TestParams
    val params =
      TestParams(
        targets = configuration.targets,
        originId = originId.toString(),
        workingDirectory = state.workingDirectory,
        arguments = transformProgramArguments(state.programArguments),
        environmentVariables = state.env.envs,
        coverageInstrumentationFilter = coverageInstrumentationFilter,
        testFilter = state.testFilter,
        additionalBazelParams = state.additionalBazelParams,
      )
    server.buildTargetTest(params)
  }

  private fun getCoverageInstrumentationFilter(project: Project): String {
    val packages =
      project.targetUtils
        .allTargets()
        .map { it.packagePath.pathSegments }
        .toSet()
        .filterPathsThatDontContainEachOther2()
    if (packages.isEmpty() || packages.singleOrNull() == emptyList<String>()) {
      // Cover all packages
      return "^//"
    }
    return "^//(${packages.joinToString("|") { it.joinToString("/") }})[/:]"
  }
}
