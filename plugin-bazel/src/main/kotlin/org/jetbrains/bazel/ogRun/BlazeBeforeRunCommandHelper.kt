/*
 * Copyright 2017 The Bazel Authors. All rights reserved.
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
package org.jetbrains.bazel.ogRun

import com.google.idea.blaze.base.async.executor.ProgressiveTaskWithProgressIndicator

/**
 * Runs a blaze build command associated with a [configuration][BlazeCommandRunConfiguration],
 * typically as a 'before run configuration' task. The flags in requiredExtraBlazeFlags appear in
 * the command line after flags a user specifies in their configuration and will override those
 * flags. The flags in overridableExtraBlazeFlags appear before, and can be overridden by
 * user-specified flags.
 */
object BlazeBeforeRunCommandHelper {
  private const val TASK_TITLE = "Blaze before run task"

  // A vm option overriding the directory used for the Bazel run script.
  private const val BAZEL_RUN_SCRIPT_VM_OVERRIDE = "bazel.run.script.path"

  /**
   * Kicks off the blaze task, returning a corresponding [ListenableFuture].
   *
   *
   * Runs the blaze command on the targets specified in the given `configuration`.
   */
  fun runBlazeCommand(
    commandName: BlazeCommandName?,
    configuration: BlazeCommandRunConfiguration,
    buildResultHelper: BuildResultHelper,
    requiredExtraBlazeFlags: MutableList<String?>?,
    overridableExtraBlazeFlags: MutableList<String?>?,
    invocationContext: BlazeInvocationContext,
    progressMessage: String?,
  ): com.google.common.util.concurrent.ListenableFuture<BuildResult?> =
    runBlazeCommand(
      commandName,
      configuration,
      buildResultHelper,
      requiredExtraBlazeFlags,
      overridableExtraBlazeFlags,
      invocationContext,
      progressMessage,
      configuration.targets,
    )

  /**
   * Runs the given blaze command on the given list of `targets` instead of retrieving the
   * targets from the run `configuration`.
   */
  fun runBlazeCommand(
    commandName: BlazeCommandName?,
    configuration: BlazeCommandRunConfiguration,
    buildResultHelper: BuildResultHelper,
    requiredExtraBlazeFlags: MutableList<String?>?,
    overridableExtraBlazeFlags: MutableList<String?>?,
    invocationContext: BlazeInvocationContext,
    progressMessage: String?,
    targets: com.google.common.collect.List<Label?>?,
  ): com.google.common.util.concurrent.ListenableFuture<BuildResult?> {
    val project: com.intellij.openapi.project.Project = configuration.project
    val handlerState: BlazeCommandRunConfigurationCommonState =
      configuration.getHandler().state as BlazeCommandRunConfigurationCommonState
    val workspaceRoot: WorkspaceRoot? = WorkspaceRoot.fromProject(project)
    val projectViewSet: ProjectViewSet? = ProjectViewManager.getInstance(project).getProjectViewSet()

    val binaryPath: String? =
      if (handlerState.getBlazeBinaryState().getBlazeBinary() != null) {
        handlerState.getBlazeBinaryState().getBlazeBinary()
      } else {
        Blaze.getBuildSystemProvider(project).getBinaryPath(project)
      }

    return ProgressiveTaskWithProgressIndicator
      .builder(project, TASK_TITLE)
      .submitTaskWithResult(
        object : ScopedTask<BuildResult?>() {
          protected override fun execute(context: BlazeContext): BuildResult {
            context
              .push(
                Builder(
                  project,
                  Task(project, TASK_TITLE, Task.Type.BEFORE_LAUNCH),
                ).setPopupBehavior(
                  BlazeUserSettings.getInstance().getShowBlazeConsoleOnRun(),
                ).setIssueParsers(
                  BlazeIssueParser.defaultIssueParsers(
                    project,
                    workspaceRoot,
                    invocationContext.type(),
                  ),
                ).build(),
              ).push(
                ProblemsViewScope(
                  project,
                  BlazeUserSettings.getInstance().getShowProblemsViewOnRun(),
                ),
              )

            context.output(StatusOutput(progressMessage))

            val command: BlazeCommand.Builder =
              BlazeCommand
                .builder(binaryPath, commandName, project)
                .addTargets(targets)
                .addBlazeFlags(overridableExtraBlazeFlags)
                .addBlazeFlags(
                  BlazeFlags.blazeFlags(
                    project,
                    projectViewSet,
                    BlazeCommandName.BUILD,
                    context,
                    invocationContext,
                  ),
                ).addBlazeFlags(
                  handlerState.getBlazeFlagsState().getFlagsForExternalProcesses(),
                ).addBlazeFlags(requiredExtraBlazeFlags)
                .addBlazeFlags(buildResultHelper.getBuildFlags())

            val exitCode: Int =
              ExternalTask
                .builder(workspaceRoot)
                .addBlazeCommand(command.build())
                .context(context)
                .stderr(
                  LineProcessingOutputStream.of(
                    BlazeConsoleLineProcessorProvider.getAllStderrLineProcessors(
                      context,
                    ),
                  ),
                ).build()
                .run()
            return BuildResult.fromExitCode(exitCode)
          }
        },
      )
  }

  /** Creates a temporary output file to write the shell script to.  */
  @kotlin.Throws(IOException::class)
  fun createScriptPathFile(): java.nio.file.Path {
    val tempDir: java.nio.file.Path?
    val dirPath: String? = java.lang.System.getProperty(BAZEL_RUN_SCRIPT_VM_OVERRIDE)
    if (com.google.common.base.Strings
        .isNullOrEmpty(dirPath)
    ) {
      tempDir = TempDirectoryProvider.getInstance().getTempDirectory()
    } else {
      tempDir =
        java.nio.file.Paths
          .get(dirPath)
    }

    val tempFile: java.nio.file.Path =
      FileOperationProvider.getInstance().createTempFile(tempDir, "blaze-script-", "")
    tempFile.toFile().deleteOnExit()
    return tempFile
  }
}
