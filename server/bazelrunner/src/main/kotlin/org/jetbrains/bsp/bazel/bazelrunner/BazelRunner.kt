package org.jetbrains.bsp.bazel.bazelrunner

import org.apache.logging.log4j.LogManager
import org.jetbrains.bsp.bazel.bazelrunner.params.BazelFlag.repositoryOverride
import org.jetbrains.bsp.bazel.commons.Constants
import org.jetbrains.bsp.bazel.logger.BspClientLogger
import org.jetbrains.bsp.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.bazel.workspacecontext.WorkspaceContextProvider
import org.jetbrains.bsp.bazel.workspacecontext.extraFlags
import java.nio.file.Path
import kotlin.io.path.pathString

class BazelRunner(
  val workspaceContextProvider: WorkspaceContextProvider,
  private val bspClientLogger: BspClientLogger?,
  val workspaceRoot: Path?,
) {
  companion object {
    private val LOGGER = LogManager.getLogger(BazelRunner::class.java)
  }

  fun buildAndRunCommand(originId: String? = null, parseProcessOutput: Boolean = true, builder: BazelRunnerCommandBuilder.() -> Unit): BazelProcess {
    val command = BazelRunnerCommandBuilder(this).apply(builder).build()
    return runBazelCommand(command, originId, parseProcessOutput)
  }

  fun runBazelCommand(command: BazelCommand, originId: String? = null, parseProcessOutput: Boolean = true): BazelProcess {
    val processArgs = command.makeCommandLine()
    val processBuilder = ProcessBuilder(processArgs)
    workspaceRoot?.let { processBuilder.directory(it.toFile()) }


    // Run needs to be handled separately because the resulting process is not run in the sandbox
    if (command is BazelCommand.Run) {
      command.workingDirectory?.let { processBuilder.directory(it.toFile()) }
      processBuilder.environment() += command.environment
      logInvocation(processArgs, command.environment, command.workingDirectory, originId)
    } else {
      logInvocation(processArgs, null, null, originId)
    }

    val process = processBuilder.start()
    val outputLogger = bspClientLogger.takeIf { parseProcessOutput }?.copy(originId = originId)

    return BazelProcess(
      process,
      outputLogger
    )
  }

  fun runBazelCommandBes(
    command: String,
    flags: List<String>,
    arguments: List<String>,
    environment: Map<String, String>,
    originId: String?,
    eventTextFile: Path,
  ): BazelProcess {
    fun besFlags() =
      listOf(
        "--build_event_binary_file=${eventTextFile.toAbsolutePath()}",
        "--bes_outerr_buffer_size=10",
        "--isatty=true",
      )

    // TODO https://youtrack.jetbrains.com/issue/BAZEL-617
    return runBazelCommand(
      command,
      flags = besFlags() + flags,
      arguments,
      environment,
      originId,
      true,
    )
  }

  fun prepareBazelCommand(
    command: String,
    flags: List<String>,
    arguments: List<String>,
    useBuildFlags: Boolean = true,
  ): List<String> {
    val workspaceContext = workspaceContextProvider.currentWorkspaceContext()
    val usedBuildFlags = if (useBuildFlags) buildFlags(workspaceContext) else emptyList()
    val relativeDotBspFolderPath = workspaceContext.dotBazelBspDirPath.value
    val defaultFlags = listOf(repositoryOverride(Constants.ASPECT_REPOSITORY, relativeDotBspFolderPath.pathString))
    val processArgs =
      listOf(bazel(workspaceContext)) + command + usedBuildFlags + defaultFlags + flags + arguments
    return processArgs
  }

  fun runBazelCommand(
    command: String,
    flags: List<String>,
    arguments: List<String>,
    environment: Map<String, String>,
    originId: String?,
    parseProcessOutput: Boolean,
    useBuildFlags: Boolean = true,
  ): BazelProcess {
    val processArgs = prepareBazelCommand(command, flags, arguments, useBuildFlags)
    logInvocation(processArgs, environment, originId)
    val processBuilder = ProcessBuilder(processArgs)
    processBuilder.environment() += environment
    val outputLogger = bspClientLogger.takeIf { parseProcessOutput }?.copy(originId = originId)
    workspaceRoot?.let { processBuilder.directory(it.toFile()) }
    val process = processBuilder.start()
    return BazelProcess(
      process,
      outputLogger,
    )
  }

  private fun envToString(environment: Map<String, String>): String = environment.entries.joinToString(" ") { "${it.key}=${it.value}" }

  private fun logInvocation(
    processArgs: List<String>,
    processEnv: Map<String, String>?,
    directory: Path?,
    originId: String?,
  ) {
    "Invoking: ${processEnv?.let { envToString(it)} } ${directory?.let { "cd $it &&" }} ${processArgs.joinToString("' '", "'", "'")}"
      .also { LOGGER.info(it) }
      // TODO: does it make sense to also log here?
      .also { bspClientLogger?.copy(originId = originId)?.message(it) }
  }

  private fun bazel(workspaceContext: WorkspaceContext): String = workspaceContext.bazelBinary.value.toString()

  private fun buildFlags(workspaceContext: WorkspaceContext): List<String> =
    workspaceContext.buildFlags.values + workspaceContext.extraFlags
}
