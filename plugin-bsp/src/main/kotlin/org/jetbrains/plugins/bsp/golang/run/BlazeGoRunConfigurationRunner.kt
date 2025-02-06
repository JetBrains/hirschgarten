/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
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
package org.jetbrains.plugins.bsp.golang.run

import com.google.idea.blaze.base.command.BlazeCommandName

/** Go-specific run configuration runner.  */
class BlazeGoRunConfigurationRunner : BlazeCommandRunConfigurationRunner {
  private class ExecutableInfo(
    binary: java.io.File?,
    workingDir: java.io.File,
    args: com.google.common.collect.ImmutableList<String?>,
    envVars: com.google.common.collect.ImmutableMap<String?, String?>
  ) {
    val binary: java.io.File?
    val workingDir: java.io.File
    val args: com.google.common.collect.ImmutableList<String?>
    val envVars: com.google.common.collect.ImmutableMap<String?, String?>

    init {
      this.binary = binary
      this.workingDir = workingDir
      this.args = args
      this.envVars = envVars
    }
  }

  /** Converts to the native go plugin debug configuration state  */
  internal class BlazeGoDummyDebugProfileState(configuration: BlazeCommandRunConfiguration) :
    com.intellij.execution.configurations.RunProfileState {
    private val state: BlazeCommandRunConfigurationCommonState

    init {
      this.state =
        configuration.getHandlerStateIfType(BlazeCommandRunConfigurationCommonState::class.java)
      if (this.state == null) {
        throw com.intellij.execution.ExecutionException("Missing run configuration common state")
      }
    }

    private fun getParameters(executable: com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner.ExecutableInfo): com.google.common.collect.ImmutableList<String?> {
      return com.google.common.collect.ImmutableList.builder<String?>()
        .addAll(state.getExeFlagsState().getFlagsForExternalProcesses())
        .addAll(state.getTestArgs())
        .addAll(executable.args)
        .build()
    }

    private val testFilter: String?
      get() = state.getTestFilterForExternalProcesses()

    @kotlin.Throws(com.intellij.execution.ExecutionException::class)
    fun toNativeState(env: com.intellij.execution.runners.ExecutionEnvironment): GoApplicationRunningState {
      val executable: com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner.ExecutableInfo =
        com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner.BlazeGoDummyDebugProfileState.Companion.getExecutableInfo(
            env,
        )
      if (executable == null || com.intellij.openapi.util.text.StringUtil.isEmptyOrSpaces(executable.binary.getPath())) {
        throw com.intellij.execution.ExecutionException("Blaze output binary not found")
      }
      val project: com.intellij.openapi.project.Project = env.getProject()
      val projectData: BlazeProjectData =
        BlazeProjectDataManager.getInstance(project).getBlazeProjectData()
      if (projectData == null) {
        throw com.intellij.execution.ExecutionException("Project data not found. Please run blaze sync.")
      }
      val nativeConfig: GoApplicationConfiguration =
        GoApplicationRunConfigurationType.getInstance()
          .getConfigurationFactories()[0]
          .createTemplateConfiguration(
              project,
              getInstance.getInstance(project),
          ) as GoApplicationConfiguration
      nativeConfig.setKind(GoBuildingRunConfiguration.Kind.PACKAGE)
      // prevents binary from being deleted by
      // GoBuildingRunningState$ProcessHandler#processTerminated
      nativeConfig.setOutputDirectory(executable.binary.getParent())
      nativeConfig.setParams(com.intellij.util.execution.ParametersListUtil.join(getParameters(executable)))

      val envVarsState: com.intellij.execution.configuration.EnvironmentVariablesData =
        state.getUserEnvVarsState().getData()
      nativeConfig.setCustomEnvironment(envVarsState.getEnvs())
      nativeConfig.setPassParentEnvironment(envVarsState.isPassParentEnvs())

      nativeConfig.setWorkingDirectory(executable.workingDir.getPath())

      val customEnvironment: MutableMap<String?, String?> =
        java.util.HashMap<String?, String?>(nativeConfig.getCustomEnvironment())
      for (entry in executable.envVars.entries) {
        customEnvironment.put(entry.key, entry.value)
      }
      val testFilter = this.testFilter
      if (testFilter != null) {
        customEnvironment.put("TESTBRIDGE_TEST_ONLY", testFilter)
      }
      nativeConfig.setCustomEnvironment(customEnvironment)

      val module: com.intellij.openapi.module.Module? =
        getInstance.getInstance(project)
          .findModuleByName(BlazeDataStorage.WORKSPACE_MODULE_NAME)
      if (module == null) {
        throw com.intellij.execution.ExecutionException("Workspace module not found")
      }
      val nativeState: GoApplicationRunningState =
        object : GoApplicationRunningState(env, module, nativeConfig) {
          val isDebug: Boolean
            get() = true

          val buildingTarget: MutableList<GoCommandLineParameter>?
            get() = null

          override fun createBuildExecutor(): GoExecutor? {
            return null
          }
        }
      nativeState.setOutputFilePath(executable.binary.getPath())
      return nativeState
    }

    override fun execute(
      executor: com.intellij.execution.Executor?,
      runner: com.intellij.execution.runners.ProgramRunner<*>?
    ): com.intellij.execution.ExecutionResult? {
      return null
    }

    companion object {
      private fun getExecutableInfo(env: com.intellij.execution.runners.ExecutionEnvironment): com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner.ExecutableInfo {
        return env.getCopyableUserData<java.util.concurrent.atomic.AtomicReference<com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner.ExecutableInfo>?>(
            com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner.Companion.EXECUTABLE_KEY,
        ).get()
      }
    }
  }

  @kotlin.Throws(com.intellij.execution.ExecutionException::class)
  public override fun getRunProfileState(
    executor: com.intellij.execution.Executor?,
    env: com.intellij.execution.runners.ExecutionEnvironment
  ): com.intellij.execution.configurations.RunProfileState {
    val configuration: BlazeCommandRunConfiguration =
      BlazeCommandRunConfigurationRunner.getConfiguration(env)
    if (!BlazeCommandRunConfigurationRunner.isDebugging(env)
      || BlazeCommandName.BUILD.equals(BlazeCommandRunConfigurationRunner.getBlazeCommand(env))
    ) {
      return BlazeCommandRunProfileState(env)
    }
    env.putCopyableUserData<java.util.concurrent.atomic.AtomicReference<com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner.ExecutableInfo?>?>(
        com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner.Companion.EXECUTABLE_KEY,
        java.util.concurrent.atomic.AtomicReference<Any?>(),
    )
    return com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner.BlazeGoDummyDebugProfileState(
        configuration,
    )
  }

  public override fun executeBeforeRunTask(env: com.intellij.execution.runners.ExecutionEnvironment): Boolean {
    if (!BlazeCommandRunConfigurationRunner.isDebugging(env)
      || BlazeCommandName.BUILD.equals(BlazeCommandRunConfigurationRunner.getBlazeCommand(env))
    ) {
      return true
    }
    env.getCopyableUserData<java.util.concurrent.atomic.AtomicReference<com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner.ExecutableInfo?>?>(
        com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner.Companion.EXECUTABLE_KEY,
    ).set(null)
    try {
      val executableInfo: com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner.ExecutableInfo =
        com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner.Companion.getExecutableToDebug(env)
      env.getCopyableUserData<java.util.concurrent.atomic.AtomicReference<com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner.ExecutableInfo?>?>(
          com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner.Companion.EXECUTABLE_KEY,
      ).set(executableInfo)
      if (executableInfo.binary != null) {
        return true
      }
    } catch (e: com.intellij.execution.ExecutionException) {
      com.intellij.execution.runners.ExecutionUtil.handleExecutionError(
          env.getProject(), env.getExecutor().getToolWindowId(), env.getRunProfile(), e,
      )
      com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner.Companion.logger.info(e)
    }
    return false
  }

  companion object {
    // The actual Bazel shell script uses "1", which is considered as true.
    // For robustness, we include other common representations of true.
    private val TRUTHY_ENV_VALUES_LOWER: MutableList<String?> =
      kotlin.collections.mutableListOf<String?>("true", "1", "yes", "on")
    private val POP_UP_MESSAGE_ENABLE_SYMLINKS: String = """
          Please enable symlink support. Add the following lines to your .bazelrc file:
          startup --windows_enable_symlinks
          build --enable_runfiles
          
          Refer to the online documentation for Using Bazel on Windows: https://bazel.build/configure/windows.
          
          """.trimIndent()

    private val scriptPathEnabled: BoolExperiment = BoolExperiment("blaze.go.script.path.enabled.2", true)

    /** Used to store a runner to an [ExecutionEnvironment].  */
    private val EXECUTABLE_KEY: com.intellij.openapi.util.Key<java.util.concurrent.atomic.AtomicReference<com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner.ExecutableInfo?>?> =
      com.intellij.openapi.util.Key.create<java.util.concurrent.atomic.AtomicReference<com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner.ExecutableInfo?>?>(
          "blaze.debug.golang.executable",
      )

    private val logger: com.intellij.openapi.diagnostic.Logger =
      com.intellij.openapi.diagnostic.Logger.getInstance(com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner::class.java)

    @kotlin.Throws(com.intellij.execution.ExecutionException::class)
    private fun getSingleTarget(config: BlazeCommandRunConfiguration): Label {
      val targets: com.google.common.collect.ImmutableList<out TargetExpression?> = config.getTargets()
      if (targets.size != 1 || targets.get(0) !is Label) {
        throw com.intellij.execution.ExecutionException("Invalid configuration: doesn't have a single target label")
      }
      return targets.get(0) as Label
    }

    /**
     * Builds blaze go target and returns the output build artifact.
     *
     * @throws ExecutionException if the target cannot be debugged.
     */
    @kotlin.Throws(com.intellij.execution.ExecutionException::class)
    private fun getExecutableToDebug(env: com.intellij.execution.runners.ExecutionEnvironment): com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner.ExecutableInfo {
      val configuration: BlazeCommandRunConfiguration =
        BlazeCommandRunConfigurationRunner.getConfiguration(env)
      val project: com.intellij.openapi.project.Project? = configuration.getProject()
      val blazeProjectData: BlazeProjectData =
        BlazeProjectDataManager.getInstance(project).getBlazeProjectData()
      if (blazeProjectData == null) {
        throw com.intellij.execution.ExecutionException("Not synced yet, please sync project")
      }
      val label: Label =
        com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner.Companion.getSingleTarget(configuration)

      SaveUtil.saveAllFiles()
      BuildResultHelperProvider.createForLocalBuild(project).use { buildResultHelper ->
        val flags: com.google.common.collect.ImmutableList.Builder<String?> =
          com.google.common.collect.ImmutableList.builder<String?>()
        if (Blaze.getBuildSystemName(project) === BuildSystemName.Blaze) {
          // $ go tool compile
          //   -N    disable optimizations
          //   -l    disable inlining
          flags.add("--gc_goopt=-N").add("--gc_goopt=-l")
        } else {
          // bazel build adds these flags themselves with -c dbg
          // https://github.com/bazelbuild/rules_go/issues/741
          flags.add("--compilation_mode=dbg")
        }

        var scriptPath: java.util.Optional<java.nio.file.Path?> =
          java.util.Optional.empty<java.nio.file.Path?>()
        if (com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner.Companion.scriptPathEnabled.getValue()) {
          try {
            scriptPath = java.util.Optional.of<T?>(BlazeBeforeRunCommandHelper.createScriptPathFile())
            flags.add("--script_path=" + scriptPath.get())
          } catch (e: IOException) {
            // Could still work without script path.
            // Script path is only needed to parse arguments from target.
            com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner.Companion.logger.warn(
                "Failed to create script path file. Target arguments will not be parsed.",
                e,
            )
          }
        }

        val buildOperation: com.google.common.util.concurrent.ListenableFuture<BuildResult> =
          BlazeBeforeRunCommandHelper.runBlazeCommand(
              if (scriptPath.isPresent()) BlazeCommandName.RUN else BlazeCommandName.BUILD,
              configuration,
              buildResultHelper,
              flags.build(),
              com.google.common.collect.ImmutableList.of<E?>(
                  "--dynamic_mode=off",
                  "--test_sharding_strategy=disabled",
              ),
              BlazeInvocationContext.runConfigContext(
                  ExecutorType.fromExecutor(env.getExecutor()), configuration.getType(), true,
              ),
              "Building debug binary",
          )

        try {
          val result: BuildResult = buildOperation.get()
          if (result.outOfMemory()) {
            throw com.intellij.execution.ExecutionException("Out of memory while trying to build debug target")
          } else if (result.status === Status.BUILD_ERROR) {
            throw com.intellij.execution.ExecutionException("Build error while trying to build debug target")
          } else if (result.status === Status.FATAL_ERROR) {
            throw com.intellij.execution.ExecutionException(
                java.lang.String.format(
                    "Fatal error (%d) while trying to build debug target", result.exitCode,
                ),
            )
          }
        } catch (e: java.lang.InterruptedException) {
          buildOperation.cancel(true)
          throw com.intellij.execution.RunCanceledByUserException()
        } catch (e: CancellationException) {
          buildOperation.cancel(true)
          throw com.intellij.execution.RunCanceledByUserException()
        } catch (e: java.util.concurrent.ExecutionException) {
          throw com.intellij.execution.ExecutionException(e)
        }
        if (scriptPath.isPresent()) {
          if (!java.nio.file.Files.exists(scriptPath.get())) {
            throw com.intellij.execution.ExecutionException(
                String.format(
                    "No debugger executable script path file produced. Expected file at: %s",
                    scriptPath.get(),
                ),
            )
          }
          val workspaceRoot: WorkspaceRoot = WorkspaceRoot.fromProject(project)
          val blazeInfo: BlazeInfo =
            BlazeProjectDataManager.getInstance(project).getBlazeProjectData().getBlazeInfo()
          return com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner.Companion.parseScriptPathFile(
              workspaceRoot,
              blazeInfo.getExecutionRoot(),
              scriptPath.get(),
          )
        } else {
          val candidateFiles: MutableList<java.io.File>
          try {
            candidateFiles =
              LocalFileArtifact.getLocalFiles(
                  buildResultHelper.getBuildOutput(java.util.Optional.empty<T?>(), Interners.STRING)
                      .getDirectArtifactsForTarget(label, { file -> true }),
              )
                .stream()
                .filter({ obj: java.io.File? -> obj.canExecute() })
                .collect(java.util.stream.Collectors.toList())
          } catch (e: GetArtifactsException) {
            throw com.intellij.execution.ExecutionException(
                java.lang.String.format(
                    "Failed to get output artifacts when building %s: %s", label, e.getMessage(),
                ),
            )
          }
          if (candidateFiles.isEmpty()) {
            throw com.intellij.execution.ExecutionException(
                String.format("No output artifacts found when building %s", label),
            )
          }
          val binary: java.io.File? =
            com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner.Companion.findExecutable(
                label,
                candidateFiles,
            )
          if (binary == null) {
            throw com.intellij.execution.ExecutionException(
                String.format(
                    "More than 1 executable was produced when building %s; "
                      + "don't know which one to debug",
                    label,
                ),
            )
          }
          com.intellij.openapi.vfs.LocalFileSystem.getInstance()
            .refreshIoFiles(com.google.common.collect.ImmutableList.of<java.io.File?>(binary))
          val workingDir: java.io.File =
            com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner.Companion.getWorkingDirectory(
                WorkspaceRoot.fromProject(project),
                binary,
            )
          return com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner.ExecutableInfo(
              binary,
              workingDir,
              com.google.common.collect.ImmutableList.of<String?>(),
              com.google.common.collect.ImmutableMap.of<String?, String?>(),
          )
        }
      }
    }

    /**
     * Basic heuristic for choosing between multiple output files. Currently just looks for a filename
     * matching the target name.
     */
    private fun findExecutable(target: Label, outputs: MutableList<java.io.File>): java.io.File? {
      if (outputs.size == 1) {
        return outputs.get(0)
      }
      val name: String = com.intellij.util.PathUtil.getFileName(target.targetName().toString())
      for (file in outputs) {
        if (file.getName() == name) {
          return file
        }
      }
      return null
    }

    // Matches TEST_SRCDIR=<dir>
    private val TEST_SRCDIR: java.util.regex.Pattern = java.util.regex.Pattern.compile("TEST_SRCDIR=([^\\s]+)")

    // Matches RUNFILES_<NAME>=<value>
    private val RUNFILES_VAR: java.util.regex.Pattern =
      java.util.regex.Pattern.compile("RUNFILES_([A-Z_]+)=([^\\s]+)")

    // Matches TEST_TARGET=//<package_path>:<target>
    private val TEST_TARGET: java.util.regex.Pattern =
      java.util.regex.Pattern.compile("TEST_TARGET=//([^:]*):([^\\s]+)")

    // Matches a space-delimited arg list. Supports wrapping arg in single quotes.
    private val ARGS: java.util.regex.Pattern = java.util.regex.Pattern.compile("([^\']\\S*|\'.+?\')\\s*")

    @kotlin.Throws(com.intellij.execution.ExecutionException::class)
    private fun parseScriptPathFile(
      workspaceRoot: WorkspaceRoot, execRoot: java.io.File, scriptPath: java.nio.file.Path
    ): com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner.ExecutableInfo {
      val text: String?
      try {
        text = com.google.common.io.MoreFiles.asCharSource(scriptPath, java.nio.charset.StandardCharsets.UTF_8)
          .read()
      } catch (e: IOException) {
        throw com.intellij.execution.ExecutionException("Could not read script_path: " + scriptPath, e)
      }
      val lastLine: String = com.google.common.collect.Iterables.getLast<String>(
          com.google.common.base.Splitter.on('\n').split(text),
      )
      var args: MutableList<String?> = java.util.ArrayList<String?>()
      val argsMatcher: java.util.regex.Matcher =
        com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner.Companion.ARGS.matcher(lastLine.trim { it <= ' ' })
      while (argsMatcher.find()) {
        var arg: String = argsMatcher.group(1)
        // Strip arg quotes
        arg = com.intellij.openapi.util.text.StringUtil.trimLeading(
            com.intellij.openapi.util.text.StringUtil.trimEnd(
                arg,
                '\'',
            ),
            '\'',
        )
        args.add(arg)
      }
      val envVars: com.google.common.collect.ImmutableMap.Builder<String?, String?> =
        com.google.common.collect.ImmutableMap.builder<String?, String?>()
      val binary: java.io.File
      val workingDir: java.io.File
      val testScrDir: java.util.regex.Matcher =
        com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner.Companion.TEST_SRCDIR.matcher(text)
      if (testScrDir.find()) {
        // Format is <wrapper-script> <executable> arg0 arg1 arg2 ... argN "@"
        if (args.size < 3) {
          throw com.intellij.execution.ExecutionException("Failed to parse args in script_path: " + scriptPath)
        }
        // Make paths used for runfiles discovery absolute as the working directory is changed below.
        envVars.put("TEST_SRCDIR", workspaceRoot.absolutePathFor(testScrDir.group(1)).toString())
        val runfilesVars: java.util.regex.Matcher =
          com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner.Companion.RUNFILES_VAR.matcher(text)
        while (runfilesVars.find()) {
          val envKey: String = String.format("RUNFILES_%s", runfilesVars.group(1))
          val envVal: String? = runfilesVars.group(2)
          if ("RUNFILES_MANIFEST_ONLY" == envKey
            && envVal != null && com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner.Companion.TRUTHY_ENV_VALUES_LOWER.contains(
                  envVal.trim { it <= ' ' }.lowercase(java.util.Locale.getDefault()),
              )
          ) {
            throw com.intellij.execution.ExecutionException(com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner.Companion.POP_UP_MESSAGE_ENABLE_SYMLINKS)
          }
          envVars.put(envKey, workspaceRoot.absolutePathFor(envVal).toString())
        }
        val workspaceName: String? = execRoot.getName()
        binary =
          java.nio.file.Paths.get(
              workspaceRoot.directory().getPath(),
              testScrDir.group(1),
              workspaceName,
              args.get(1),
          )
            .toFile()

        val testTarget: java.util.regex.Matcher =
          com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner.Companion.TEST_TARGET.matcher(text)
        if (testTarget.find()) {
          val packagePath: String = testTarget.group(1)
          workingDir =
            java.nio.file.Paths.get(
                workspaceRoot.directory().getPath(),
                testScrDir.group(1),
                workspaceName,
                packagePath,
            )
              .toFile()
        } else {
          workingDir = workspaceRoot.directory()
        }

        // Remove everything except the args.
        args = args.subList(2, args.size - 1)
      } else {
        // Format is <executable> [arg0 arg1 arg2 ... argN] "@"
        if (args.size < 2) {
          throw com.intellij.execution.ExecutionException("Failed to parse args in script_path: " + scriptPath)
        }
        binary = java.io.File(args.get(0))
        workingDir =
          com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner.Companion.getWorkingDirectory(
              workspaceRoot,
              binary,
          )
        // Remove everything except the args.
        args = args.subList(1, args.size - 1)
      }
      return com.google.idea.blaze.golang.run.BlazeGoRunConfigurationRunner.ExecutableInfo(
          binary,
          workingDir,
          com.google.common.collect.ImmutableList.copyOf<String?>(args),
          envVars.build(),
      )
    }

    /**
     * Similar to [com.google.idea.blaze.python.run.BlazePyRunConfigurationRunner].
     *
     *
     * Working directory should be the runfiles directory of the debug executable.
     *
     *
     * If the runfiles directory does not exist (unlikely) fall back to workspace root.
     */
    private fun getWorkingDirectory(root: WorkspaceRoot, executable: java.io.File): java.io.File {
      val workspaceName: String = root.directory().getName()
      val expectedPath: java.io.File = java.io.File(executable.getPath() + ".runfiles", workspaceName)
      if (FileOperationProvider.getInstance().isDirectory(expectedPath)) {
        return expectedPath
      }
      return root.directory()
    }
  }
}
