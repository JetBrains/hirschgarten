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
package org.jetbrains.bazel.run2.confighandler

import com.google.common.base.Preconditions
import com.google.common.collect.ImmutableList
import com.google.idea.blaze.base.async.process.LineProcessingOutputStream
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
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.WorkspaceRoot
import org.jetbrains.bazel.commons.command.BlazeCommandName
import org.jetbrains.bazel.run2.BlazeCommandRunConfiguration
import org.jetbrains.bazel.run2.ExecutorType
import org.jetbrains.bazel.run2.state.BlazeCommandRunConfigurationCommonState

/**
 * Generic runner for [BlazeCommandRunConfiguration]s, used as a fallback in the case where no
 * other runners are more relevant.
 */
class BlazeCommandGenericRunConfigurationRunner : BlazeCommandRunConfigurationRunner {
  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
    BlazeCommandRunProfileState(environment)

  override fun executeBeforeRunTask(environment: ExecutionEnvironment): Boolean {
    // Don't execute any tasks.
    return true
  }

  /** [RunProfileState] for generic blaze commands.  */
  class BlazeCommandRunProfileState(environment: ExecutionEnvironment) : CommandLineState(environment) {
    private val configuration: BlazeCommandRunConfiguration = getConfiguration(environment)
    private val handlerState: BlazeCommandRunConfigurationCommonState = configuration.getHandler().state as BlazeCommandRunConfigurationCommonState
    private val consoleFilters: ImmutableList<Filter>

    init {
      val project = environment.project
      this.consoleFilters =
        ImmutableList.of(
          UrlFilter(),
          ToolWindowTaskIssueOutputFilter.createWithDefaultParsers(
            project,
            WorkspaceRoot.fromProject(project),
            BlazeInvocationContext.ContextType.RunConfiguration,
          ),
        )
    }

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
      val projectViewSet: ProjectViewSet = checkNotNull(ProjectViewManager.getInstance(project).getProjectViewSet())
      val context: BlazeContext = BlazeContext.create()
      val invoker: BuildInvoker? = Blaze.getBuildSystemProvider(project).getBuildSystem().getBuildInvoker(project)
      val workspaceRoot: WorkspaceRoot = WorkspaceRoot.fromImportSettings(importSettings)
      val blazeCommand: BlazeCommand.Builder =
        getBlazeCommand(
          project,
          ExecutorType.fromExecutor(getEnvironment().getExecutor()),
          invoker,
          ImmutableList.of<String>(),
          context,
        )
      return if (this.isTest) {
        getProcessHandlerForTests(project, blazeCommand, workspaceRoot, context)
      } else {
        getScopedProcessHandler(project, blazeCommand.build(), workspaceRoot)
      }
    }

    @Throws(ExecutionException::class)
    private fun getScopedProcessHandler(
      project: Project?,
      blazeCommand: BlazeCommand,
      workspaceRoot: WorkspaceRoot,
    ): ProcessHandler {
      val commandLine: GeneralCommandLine = GeneralCommandLine(blazeCommand.toList())
      val envVarState = handlerState.userEnvVarsState.getData()
      commandLine.withEnvironment(envVarState.getEnvs())
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

          override fun createProcessListeners(context: BlazeContext?): ImmutableList<ProcessListener?> {
            val outputStream: LineProcessingOutputStream? =
              LineProcessingOutputStream.of(
                BlazeConsoleLineProcessorProvider.getAllStderrLineProcessors(context),
              )
            return ImmutableList.of<ProcessListener?>(LineProcessingProcessAdapter(outputStream))
          }
        },
      )
    }

    @Throws(ExecutionException::class)
    private fun getProcessHandlerForTests(
      project: Project,
      blazeCommandBuilder: BlazeCommand.Builder,
      workspaceRoot: WorkspaceRoot,
      context: BlazeContext,
    ): ProcessHandler {
      // TODO: find proper place to close the result helper (same for BlazeCidrLauncher)
      val buildResultHelper: BuildResultHelperBep = BuildResultHelperBep()
      val testResultFinderStrategy: LocalBuildEventProtocolTestFinderStrategy =
        LocalBuildEventProtocolTestFinderStrategy(buildResultHelper.getOutputFile())

      if (BlazeTestEventsHandler.targetsSupported(project, configuration.targets)) {
        val testUiSession: BlazeTestUiSession =
          BlazeTestUiSession.create(
            ImmutableList
              .builder<String>()
              .add("--runs_per_test=1")
              .add("--flaky_test_attempts=1")
              .addAll(buildResultHelper.getBuildFlags())
              .build(),
            testResultFinderStrategy,
          )

        val consoleView: ConsoleView =
          SmRunnerUtils.getConsoleView(
            project,
            configuration,
            environment.executor,
            testUiSession,
          )
        consoleBuilder =
          object : TextConsoleBuilderImpl(project) {
            override fun createConsole(): ConsoleView = consoleView
          }
        context.addOutputSink(PrintOutput::class.java, WritingOutputSink(consoleView))

        blazeCommandBuilder.addBlazeFlags(testUiSession.getBlazeFlags())
      }

      addConsoleFilters(*consoleFilters.toTypedArray<Filter?>())

      // When running `bazel test`, bazel will not forward the environment to the tests themselves -- we need to use
      // the --test_env flag for that. Therefore, we convert all the env vars to --test_env flags here.
      for (env in handlerState.userEnvVarsState
        .getData()
        .getEnvs()
        .entries) {
        blazeCommandBuilder.addBlazeFlags(BlazeFlags.TEST_ENV, String.format("%s=%s", env.key, env.value))
      }

      return getScopedProcessHandler(project, blazeCommandBuilder.build(), workspaceRoot)
    }

    private fun getBlazeCommand(
      project: Project,
      executorType: ExecutorType,
      invoker: BuildInvoker,
      testHandlerFlags: ImmutableList<String>,
      context: BlazeContext,
    ): BlazeCommand.Builder {
      val projectViewSet: ProjectViewSet =
        Preconditions.checkNotNull<T>(ProjectViewManager.getInstance(project).getProjectViewSet())

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
        .addBlazeFlags(handlerState.blazeFlagsState.getFlagsForExternalProcesses())
        .addExeFlags(handlerState.exeFlagsState.getFlagsForExternalProcesses())
    }

    private val command: BlazeCommandName?
      get() = handlerState.commandState.command

    private val isTest: Boolean
      get() = BlazeCommandName.TEST == this.command

    companion object {
      private const val BLAZE_BUILD_INTERRUPTED = 8

      private fun getConfiguration(environment: ExecutionEnvironment): BlazeCommandRunConfiguration =
        BlazeCommandRunConfigurationRunner.getConfiguration(environment)
    }
  }

  @JvmRecord
  private data class WritingOutputSink(val console: ConsoleView) : OutputSink<PrintOutput?> {
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
