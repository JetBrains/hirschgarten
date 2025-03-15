/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.bazel.ogRun.confighandler

import com.google.common.base.Preconditions
import com.google.common.base.Verify

import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.idea.blaze.base.async.executor.BlazeExecutor
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.TextConsoleBuilderImpl
import com.intellij.execution.filters.UrlFilter
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.ogRun.BlazeCommandRunConfiguration
import org.jetbrains.bazel.ogRun.state.BlazeCommandRunConfigurationCommonState
import org.jetbrains.bazel.ogRun.testlogs.BlazeTestResultFinderStrategy
import java.io.OutputStream

/**
 * Generic runner for [BlazeCommandRunConfiguration]s, used as a fallback in the case where no
 * other runners are more relevant.
 */
class BlazeCommandGenericRunConfigurationRunner : BlazeCommandRunConfigurationRunner {
  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
    BlazeCommandRunProfileState(environment)

  override fun executeBeforeRunTask(environment: ExecutionEnvironment?): Boolean {
    // Don't execute any tasks.
    return true
  }

  /** [RunProfileState] for generic blaze commands.  */
  class BlazeCommandRunProfileState(environment: ExecutionEnvironment) : CommandLineState(environment) {
    private val configuration: BlazeCommandRunConfiguration = getConfiguration(environment)
    private val handlerState: BlazeCommandRunConfigurationCommonState = configuration.getHandler().state as BlazeCommandRunConfigurationCommonState
    val project = environment.project
    private val consoleFilters: List<Filter?> = listOf(
      UrlFilter(),
      ToolWindowTaskIssueOutputFilter.createWithDefaultParsers(
        project,
        WorkspaceRoot.fromProject(project),
        BlazeInvocationContext.ContextType.RunConfiguration,
      ),
    )

    @Throws(ExecutionException::class)
    override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
      val result = super.execute(executor, runner) as DefaultExecutionResult
      return SmRunnerUtils.attachRerunFailedTestsAction(result)
    }

    @Throws(ExecutionException::class)
    override fun startProcess(): ProcessHandler {
      val project = configuration.project
      val importSettings: BlazeImportSettings =
        checkNotNull(
          BlazeImportSettingsManager.getInstance(project).getImportSettings(),
        )
      val projectViewSet: ProjectViewSet =
        checkNotNull(ProjectViewManager.getInstance(project).getProjectViewSet())
      val context: BlazeContext = BlazeContext.create()
      val invoker: BuildInvoker =
        Blaze.getBuildSystemProvider(project).getBuildSystem().getBuildInvoker(project, context)
      val workspaceRoot: WorkspaceRoot = WorkspaceRoot.fromImportSettings(importSettings)
      return invoker.createBuildResultHelper().use { buildResultHelper ->
        val blazeCommand: BlazeCommand.Builder =
          getBlazeCommand(
            project,
            ExecutorType.fromExecutor(environment.executor),
            invoker,
            listOf(buildResultHelper.getBuildFlags()),
            context,
          )
        return@use if (this.isTest) {
          getProcessHandlerForTests(
            project,
            invoker,
            buildResultHelper,
            blazeCommand,
            workspaceRoot,
            context,
          )
        } else {
          getProcessHandlerForNonTests(
            project,
            invoker,
            buildResultHelper,
            blazeCommand,
            workspaceRoot,
            context,
          )
        }
      }
    }

    private val genericProcessHandler: ProcessHandler
      get() =
        object : ProcessHandler() {
          override fun destroyProcessImpl() {
            notifyProcessTerminated(BLAZE_BUILD_INTERRUPTED)
          }

          override fun detachProcessImpl() {
            ApplicationManager
              .getApplication()
              .executeOnPooledThread(Runnable { this.notifyProcessDetached() })
          }

          override fun detachIsDefault(): Boolean = false

          override fun getProcessInput(): OutputStream? = null
        }

    @Throws(ExecutionException::class)
    private fun getScopedProcessHandler(
      project: Project?,
      blazeCommand: BlazeCommand,
      workspaceRoot: WorkspaceRoot,
    ): ProcessHandler {
      val commandLine: GeneralCommandLine = GeneralCommandLine(blazeCommand.toList())
      val envVarState = handlerState.userEnvVarsState.data
      commandLine.withEnvironment(envVarState.envs)
      commandLine.withParentEnvironmentType(
        if (envVarState.isPassParentEnvs()) {
          GeneralCommandLine.ParentEnvironmentType.CONSOLE
        } else {
          GeneralCommandLine.ParentEnvironmentType.NONE
        },
      )
      return ScopedBlazeProcessHandler(
        project,
        commandLine,
        workspaceRoot,
        object : ScopedProcessHandlerDelegate {
          override fun onBlazeContextStart(context: BlazeContext) {
            context
              .push(
                ProblemsViewScope(
                  project,
                  BlazeUserSettings.getInstance().getShowProblemsViewOnRun(),
                ),
              ).push(IdeaLogScope())
          }

          override fun createProcessListeners(context: BlazeContext?): List<ProcessListener?>? {
            val outputStream: LineProcessingOutputStream =
              LineProcessingOutputStream.of(
                BlazeConsoleLineProcessorProvider.getAllStderrLineProcessors(context),
              )
            return listOf<ProcessListener?>(LineProcessingProcessAdapter(outputStream))
          }
        },
      )
    }

    @Throws(ExecutionException::class)
    private fun getProcessHandlerForNonTests(
      project: Project,
      invoker: BuildInvoker,
      buildResultHelper: BuildResultHelper?,
      blazeCommandBuilder: BlazeCommand.Builder,
      workspaceRoot: WorkspaceRoot,
      context: BlazeContext,
    ): ProcessHandler {
      if (invoker.getCommandRunner().canUseCli()) {
        return getScopedProcessHandler(project, blazeCommandBuilder.build(), workspaceRoot)
      }
      val processHandler = this.genericProcessHandler
      val consoleView = consoleBuilder.console
      context.addOutputSink(PrintOutput::class.java, WritingOutputSink(consoleView))
      consoleBuilder = object : TextConsoleBuilderImpl(project) {
        override fun createConsole(): ConsoleView = consoleView
      }
      addConsoleFilters(*consoleFilters.toTypedArray<Filter?>())

      val envVars = handlerState.userEnvVarsState.data.envs
      val blazeBuildOutputsListenableFuture: ListenableFuture<BlazeBuildOutputs?> =
        BlazeExecutor
          .getInstance()
          .submit(
            {
              invoker
                .getCommandRunner()
                .run(project, blazeCommandBuilder, buildResultHelper, context, envVars)
            },
          )
      Futures.addCallback<V?>(
        blazeBuildOutputsListenableFuture,
        object : FutureCallback<BlazeBuildOutputs?> {
          override fun onSuccess(blazeBuildOutputs: BlazeBuildOutputs?) {
            processHandler.detachProcess()
          }

          override fun onFailure(throwable: Throwable) {
            context.handleException(throwable.message, throwable)
            processHandler.detachProcess()
          }
        },
        BlazeExecutor.getInstance().executor,
      )

      processHandler.addProcessListener(
        object : ProcessAdapter() {
          override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {
            if (willBeDestroyed) {
              context.setCancelled()
            }
          }
        },
      )
      return processHandler
    }

    @Throws(ExecutionException::class)
    private fun getProcessHandlerForTests(
      project: Project,
      invoker: BuildInvoker,
      buildResultHelper: BuildResultHelper,
      blazeCommandBuilder: BlazeCommand.Builder,
      workspaceRoot: WorkspaceRoot,
      context: BlazeContext,
    ): ProcessHandler {
      val testResultFinderStrategy: BlazeTestResultFinderStrategy =
        if (!invoker.getCommandRunner().canUseCli()) {
          BlazeTestResultHolder()
        } else {
          LocalBuildEventProtocolTestFinderStrategy(buildResultHelper)
        }
      var testUiSession: BlazeTestUiSession? = null
      if (BlazeTestEventsHandler.targetsSupported(project, configuration.targets)) {
        testUiSession =
          BlazeTestUiSession.create(
            List
              .builder<String?>()
              .addAll(listOf(buildResultHelper.getBuildFlags()))
              .add("--runs_per_test=1")
              .add("--flaky_test_attempts=1")
              .build(),
            testResultFinderStrategy,
          )
      }
      if (testUiSession != null) {
        val consoleView: ConsoleView =
          SmRunnerUtils.getConsoleView(
            project,
            configuration,
            environment.executor,
            testUiSession,
          )
        consoleBuilder = object : TextConsoleBuilderImpl(project) {
          override fun createConsole(): ConsoleView = consoleView
        }
        context.addOutputSink(PrintOutput::class.java, WritingOutputSink(consoleView))
      }
      addConsoleFilters(*consoleFilters.toTypedArray<Filter?>())
      val envVars = handlerState.userEnvVarsState.data.envs

      if (invoker.getCommandRunner().canUseCli()) {
        // If we can use the CLI, that means we will run through Bazel (as opposed to a raw process handler)
        // When running `bazel test`, bazel will not forward the environment to the tests themselves -- we need to use
        // the --test_env flag for that. Therefore, we convert all the env vars to --test_env flags here.
        for (env in envVars.entries) {
          blazeCommandBuilder.addBlazeFlags(BlazeFlags.TEST_ENV, String.format("%s=%s", env.key, env.value))
        }

        return getScopedProcessHandler(project, blazeCommandBuilder.build(), workspaceRoot)
      }
      return getCommandRunnerProcessHandlerForTests(
        project,
        invoker,
        buildResultHelper,
        blazeCommandBuilder,
        testResultFinderStrategy,
        context,
      )
    }

    private fun getCommandRunnerProcessHandlerForTests(
      project: Project?,
      invoker: BuildInvoker,
      buildResultHelper: BuildResultHelper?,
      blazeCommandBuilder: BlazeCommand.Builder?,
      testResultFinderStrategy: BlazeTestResultFinderStrategy?,
      context: BlazeContext,
    ): ProcessHandler {
      val processHandler = this.genericProcessHandler
      val envVars = handlerState.userEnvVarsState.data.envs
      val blazeTestResultsFuture: ListenableFuture<BlazeTestResults?> =
        BlazeExecutor
          .getInstance()
          .submit(
            {
              invoker
                .getCommandRunner()
                .runTest(project, blazeCommandBuilder, buildResultHelper, context, envVars)
            },
          )
      Futures.addCallback<V?>(
        blazeTestResultsFuture,
        object : FutureCallback<BlazeTestResults?> {
          override fun onSuccess(blazeTestResults: BlazeTestResults) {
            // The command-runners allow using a remote BES for parsing the test results, so we
            // use a BlazeTestResultHolder to store the test results for the IDE to find/read
            // later. The LocalTestResultFinderStrategy won't work here since it writes/reads the
            // test results to a local file.
            Verify.verify(testResultFinderStrategy is BlazeTestResultHolder)
            (testResultFinderStrategy as BlazeTestResultHolder).setTestResults(blazeTestResults)
            processHandler.detachProcess()
          }

          override fun onFailure(throwable: Throwable) {
            context.handleException(throwable.message, throwable)
            Verify.verify(testResultFinderStrategy is BlazeTestResultHolder)
            (testResultFinderStrategy as BlazeTestResultHolder)
              .setTestResults(BlazeTestResults.NO_RESULTS)
            processHandler.detachProcess()
          }
        },
        BlazeExecutor.getInstance().executor,
      )

      processHandler.addProcessListener(
        object : ProcessAdapter() {
          override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {
            if (willBeDestroyed) {
              context.setCancelled()
              Verify.verify(testResultFinderStrategy is BlazeTestResultHolder)
              (testResultFinderStrategy as BlazeTestResultHolder)
                .setTestResults(BlazeTestResults.NO_RESULTS)
            }
          }
        },
      )
      return processHandler
    }

    private fun getBlazeCommand(
      project: Project?,
      executorType: ExecutorType?,
      invoker: BuildInvoker?,
      testHandlerFlags: List<String?>,
      context: BlazeContext?,
    ): BlazeCommand.Builder {
      val projectViewSet: ProjectViewSet? =
        Preconditions.checkNotNull<T?>(ProjectViewManager.getInstance(project).getProjectViewSet())

      val extraBlazeFlags: MutableList<String?> = ArrayList<String?>(testHandlerFlags)
      var command: BlazeCommandName? = this.command
      if (executorType == ExecutorType.COVERAGE) {
        command = BlazeCommandName.COVERAGE
      }

      return BlazeCommand
        .builder(invoker, command, project)
        .addTargets(configuration.targets)
        .addBlazeFlags(
          BlazeFlags.blazeFlags(
            project,
            projectViewSet,
            this.command,
            context,
            BlazeInvocationContext.runConfigContext(
              executorType,
              configuration.getType(),
              false,
            ),
          ),
        ).addBlazeFlags(extraBlazeFlags)
        .addBlazeFlags(handlerState.blazeFlagsState.flagsForExternalProcesses)
        .addExeFlags(handlerState.exeFlagsState.flagsForExternalProcesses)
    }

    private val command: BlazeCommandName
      get() = handlerState.commandState.getCommand()

    private val isTest: Boolean
      get() = BlazeCommandName.TEST.equals(this.command)

    companion object {
      private const val BLAZE_BUILD_INTERRUPTED = 8

      private fun getConfiguration(environment: ExecutionEnvironment): BlazeCommandRunConfiguration =
        BlazeCommandRunConfigurationRunner.getConfiguration(environment)
    }
  }

  private class WritingOutputSink(private val console: ConsoleView) : OutputSink<PrintOutput?> {
    public override fun onOutput(output: PrintOutput): Propagation {
      // Add ANSI support to the console to view colored output
      console.print(
        output.getText().replaceAll("\u001B\\[[;\\d]*m", "") + "\n",
        if (output.getOutputType() === OutputType.ERROR) {
          ConsoleViewContentType.ERROR_OUTPUT
        } else {
          ConsoleViewContentType.NORMAL_OUTPUT
        },
      )
      return Propagation.Continue
    }
  }
}
