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
  private val workspaceContextProvider: WorkspaceContextProvider,
  private val bspClientLogger: BspClientLogger?,
  val workspaceRoot: Path?,
) {
  companion object {
    private val LOGGER = LogManager.getLogger(BazelRunner::class.java)
  }

  fun commandBuilder(): BazelRunnerCommandBuilder = BazelRunnerCommandBuilder(this)

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

  fun runBazelCommand(
    command: String,
    flags: List<String>,
    arguments: List<String>,
    environment: Map<String, String>,
    originId: String?,
    parseProcessOutput: Boolean,
    useBuildFlags: Boolean = true,
  ): BazelProcess {
    val workspaceContext = workspaceContextProvider.currentWorkspaceContext()
    val usedBuildFlags = if (useBuildFlags) buildFlags(workspaceContext) else emptyList()
    val relativeDotBspFolderPath = workspaceContext.dotBazelBspDirPath.value
    val defaultFlags = listOf(repositoryOverride(Constants.ASPECT_REPOSITORY, relativeDotBspFolderPath.pathString))
    val processArgs =
      listOf(bazel(workspaceContext)) + command + usedBuildFlags + defaultFlags + flags + arguments
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
    processEnv: Map<String, String>,
    originId: String?,
  ) {
    "Invoking: ${envToString(processEnv)} ${processArgs.joinToString(" ")}"
      .also { LOGGER.info(it) }
      .also { bspClientLogger?.copy(originId = originId)?.message(it) }
  }

  private fun bazel(workspaceContext: WorkspaceContext): String = workspaceContext.bazelBinary.value.toString()

  private fun buildFlags(workspaceContext: WorkspaceContext): List<String> =
    workspaceContext.buildFlags.values + workspaceContext.extraFlags
}
