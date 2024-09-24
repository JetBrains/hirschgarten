package org.jetbrains.bsp.bazel.bazelrunner

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import org.apache.logging.log4j.LogManager
import org.jetbrains.bsp.bazel.bazelrunner.params.BazelFlag.repositoryOverride
import org.jetbrains.bsp.bazel.commons.Constants
import org.jetbrains.bsp.bazel.logger.BspClientLogger
import org.jetbrains.bsp.bazel.workspacecontext.WorkspaceContextProvider
import org.jetbrains.bsp.bazel.workspacecontext.extraFlags
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import kotlin.io.path.pathString

class BazelRunner(
  val workspaceContextProvider: WorkspaceContextProvider,
  private val bspClientLogger: BspClientLogger?,
  val workspaceRoot: Path?,
) {
  companion object {
    private val LOGGER = LogManager.getLogger(BazelRunner::class.java)
  }

  inner class CommandBuilder {
    private val workspaceContext = workspaceContextProvider.currentWorkspaceContext()
    private val bazelBinary = workspaceContext.bazelBinary.value.pathString
    var inheritWorkspaceOptions = false

    fun clean(builder: BazelCommand.Clean.() -> Unit = {}) = BazelCommand.Clean(bazelBinary).apply { builder() }

    fun info(builder: BazelCommand.Info.() -> Unit = {}) = BazelCommand.Info(bazelBinary).apply { builder() }

    fun version(builder: BazelCommand.Version.() -> Unit = {}) = BazelCommand.Version(bazelBinary).apply { builder() }

    fun run(target: BuildTargetIdentifier, builder: BazelCommand.Run.() -> Unit = {}) =
      BazelCommand.Run(bazelBinary, target).apply { builder() }.also { inheritWorkspaceOptions = true }

    fun graph(builder: BazelCommand.ModGraph.() -> Unit = {}) = BazelCommand.ModGraph(bazelBinary).apply { builder() }

    fun path(builder: BazelCommand.ModPath.() -> Unit = {}) = BazelCommand.ModPath(bazelBinary).apply { builder() }

    fun showRepo(builder: BazelCommand.ModShowRepo.() -> Unit = {}) = BazelCommand.ModShowRepo(bazelBinary).apply { builder() }

    fun query(builder: BazelCommand.Query.() -> Unit = {}) = BazelCommand.Query(bazelBinary).apply { builder() }

    fun cquery(builder: BazelCommand.CQuery.() -> Unit = {}) =
      BazelCommand.CQuery(bazelBinary).apply { builder() }.also { inheritWorkspaceOptions = true }

    fun build(builder: BazelCommand.Build.() -> Unit = {}) =
      BazelCommand
        .Build(bazelBinary)
        .apply { builder() }
        .also { inheritWorkspaceOptions = true }

    fun mobileInstall(target: BuildTargetIdentifier, builder: BazelCommand.MobileInstall.() -> Unit = {}) =
      BazelCommand.MobileInstall(bazelBinary, target).apply { builder() }.also { inheritWorkspaceOptions = true }

    fun test(builder: BazelCommand.Test.() -> Unit = {}) =
      BazelCommand.Test(bazelBinary).apply { builder() }.also { inheritWorkspaceOptions = true }

    fun coverage(builder: BazelCommand.Coverage.() -> Unit = {}) =
      BazelCommand.Coverage(bazelBinary).apply { builder() }.also { inheritWorkspaceOptions = true }
  }

  fun buildBazelCommand(inheritProjectviewOptionsOverride: Boolean? = null, doBuild: CommandBuilder.() -> BazelCommand): BazelCommand {
    val commandBuilder = CommandBuilder()
    val command = doBuild(commandBuilder)
    val workspaceContext = workspaceContextProvider.currentWorkspaceContext()
    val relativeDotBspFolderPath = workspaceContext.dotBazelBspDirPath.value

    command.options.add(repositoryOverride(Constants.ASPECT_REPOSITORY, relativeDotBspFolderPath.pathString))
    // These options are the same as in Google's Bazel plugin for IntelliJ
    // They make the output suitable for display in the console
    command.options.addAll(
      listOf(
        "--curses=no",
        "--color=yes",
        "--noprogress_in_terminal_title",
      ),
    )

    val workspaceBazelOptions = workspaceContext.buildFlags.values + workspaceContext.extraFlags

    inheritProjectviewOptionsOverride?.let {
      commandBuilder.inheritWorkspaceOptions = it
    }

    if (commandBuilder.inheritWorkspaceOptions) {
      command.options.addAll(workspaceBazelOptions)
    }

    return command
  }

  fun runBazelCommand(
    command: BazelCommand,
    originId: String? = null,
    logProcessOutput: Boolean = true,
    serverPidFuture: CompletableFuture<Long>?,
  ): BazelProcess {
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
    val outputLogger = bspClientLogger.takeIf { logProcessOutput }?.copy(originId = originId)

    return BazelProcess(
      process,
      outputLogger,
      serverPidFuture,
    )
  }

  private fun envToString(environment: Map<String, String>): String = environment.entries.joinToString(" ") { "${it.key}=${it.value}" }

  private fun logInvocation(
    processArgs: List<String>,
    processEnv: Map<String, String>?,
    directory: Path?,
    originId: String?,
  ) {
    val envString = processEnv?.let { envToString(it) }
    val directoryString = directory?.let { "cd $it &&" }
    val processArgsString = processArgs.joinToString("' '", "'", "'")
    listOfNotNull("Invoking:", envString, directoryString, processArgsString)
      .joinToString(" ")
      .also { LOGGER.info(it) }
      .also { bspClientLogger?.copy(originId = originId)?.message(it) }
  }
}
