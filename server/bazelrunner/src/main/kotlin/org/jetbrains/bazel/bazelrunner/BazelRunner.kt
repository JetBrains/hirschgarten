package org.jetbrains.bazel.bazelrunner

import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.bazel.bazelrunner.outputs.ProcessSpawner
import org.jetbrains.bazel.bazelrunner.outputs.spawnProcessBlocking
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.SystemInfoProvider
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.logger.BspClientLogger
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import kotlin.io.path.pathString

/**
 * Runs Bazel commands with proper repository configuration.
 *
 * @param bazelInfo Required for determining correct repository injection method.
 *                  When bazelInfo is available, BazelRunner can choose between
 *                  --inject_repository (newer, preferred) vs --override_repository (fallback)
 *                  based on Bazel version and bzlmod support. Without bazelInfo,
 *                  repository injection may fail in newer Bazel versions.
 */
class BazelRunner(
  private val bspClientLogger: BspClientLogger?,
  val workspaceRoot: Path,
  var bazelInfo: BazelInfo? = null,
) {
  companion object {
    private val LOGGER = LoggerFactory.getLogger(BazelRunner::class.java)
  }

  inner class CommandBuilder(workspaceContext: WorkspaceContext) {
    private val bazelBinary = workspaceContext.bazelBinary?.pathString ?: "bazel"
    var inheritWorkspaceOptions = false

    fun clean(builder: BazelCommand.Clean.() -> Unit = {}) = BazelCommand.Clean(bazelBinary).apply { builder() }

    fun shutDown(builder: BazelCommand.ShutDown.() -> Unit = {}) = BazelCommand.ShutDown(bazelBinary).apply { builder() }

    fun info(builder: BazelCommand.Info.() -> Unit = {}) = BazelCommand.Info(bazelBinary).apply { builder() }

    fun version(builder: BazelCommand.Version.() -> Unit = {}) = BazelCommand.Version(bazelBinary).apply { builder() }

    fun run(target: Label, builder: BazelCommand.Run.() -> Unit = {}) =
      BazelCommand.Run(bazelBinary, target).apply { builder() }.also { inheritWorkspaceOptions = true }

    fun graph(builder: BazelCommand.ModGraph.() -> Unit = {}) = BazelCommand.ModGraph(bazelBinary).apply { builder() }

    fun path(builder: BazelCommand.ModPath.() -> Unit = {}) = BazelCommand.ModPath(bazelBinary).apply { builder() }

    fun showRepo(builder: BazelCommand.ModShowRepo.() -> Unit = {}) = BazelCommand.ModShowRepo(bazelBinary).apply { builder() }

    fun dumpRepoMapping(
      builder: BazelCommand.ModDumpRepoMapping.() -> Unit = {
      },
    ) = BazelCommand.ModDumpRepoMapping(bazelBinary).apply { builder() }

    fun query(allowManualTargetsSync: Boolean = true, builder: BazelCommand.Query.() -> Unit = {}) =
      BazelCommand.Query(bazelBinary, allowManualTargetsSync, SystemInfoProvider.getInstance()).apply { builder() }

    /** Special version of `query` for asking Bazel about files instead of a target */
    fun fileQuery(filePaths: List<Path>, builder: BazelCommand.QueryExpression.() -> Unit = {}): BazelCommand.QueryExpression {
      val fileString = filePaths.joinToString(prefix = "set(", separator = " ", postfix = ")")
      return queryExpression(fileString, builder)
    }

    /** Purest form of `query`, asking for exact string to execute instead of `Label`s */
    fun queryExpression(expression: String, builder: BazelCommand.QueryExpression.() -> Unit = {}) =
      BazelCommand.QueryExpression(bazelBinary, expression).apply { builder() }

    fun cquery(builder: BazelCommand.CQuery.() -> Unit = {}) =
      BazelCommand.CQuery(bazelBinary).apply { builder() }.also { inheritWorkspaceOptions = true }

    fun aquery(builder: BazelCommand.AQuery.() -> Unit = {}) =
      BazelCommand.AQuery(bazelBinary).apply { builder() }.also { inheritWorkspaceOptions = true }

    fun build(builder: BazelCommand.Build.() -> Unit = {}) =
      BazelCommand
        .Build(bazelBinary)
        .apply { builder() }
        .also { inheritWorkspaceOptions = true }

    fun mobileInstall(target: Label, builder: BazelCommand.MobileInstall.() -> Unit = {}) =
      BazelCommand
        .MobileInstall(bazelBinary, target)
        .apply {
          // --tool_tag is not supported by mobile-install
          this.options.clear()
          builder()
        }.also { inheritWorkspaceOptions = true }

    fun test(builder: BazelCommand.Test.() -> Unit = {}) =
      BazelCommand.Test(bazelBinary).apply { builder() }.also { inheritWorkspaceOptions = true }

    fun coverage(builder: BazelCommand.Coverage.() -> Unit = {}) =
      BazelCommand.Coverage(bazelBinary).apply { builder() }.also { inheritWorkspaceOptions = true }
  }

  fun buildBazelCommand(
    workspaceContext: WorkspaceContext,
    inheritProjectviewOptionsOverride: Boolean? = null,
    doBuild: CommandBuilder.() -> BazelCommand,
  ): BazelCommand {
    val commandBuilder = CommandBuilder(workspaceContext)
    val command = doBuild(commandBuilder)

    // These options are the same as in Google's Bazel plugin for IntelliJ
    // They make the output suitable for display in the console
    command.options.addAll(
      listOf(
        "--curses=no",
        "--color=yes",
        "--noprogress_in_terminal_title",
      ),
    )

    inheritProjectviewOptionsOverride?.let {
      commandBuilder.inheritWorkspaceOptions = it
    }

    if (commandBuilder.inheritWorkspaceOptions) {
      command.options.addAll(workspaceContext.buildFlags)
    }

    return command
  }

  fun runBazelCommand(
    command: BazelCommand,
    originId: String? = null,
    logProcessOutput: Boolean = true,
    serverPidFuture: CompletableFuture<Long>?,
    shouldLogInvocation: Boolean = true,
    createdProcessIdDeferred: CompletableDeferred<Long?>? = null,
  ): BazelProcess {
    val executionDescriptor = command.buildExecutionDescriptor()
    val finishCallback = executionDescriptor.finishCallback
    val processArgs = executionDescriptor.command

    val processSpawner = ProcessSpawner.getInstance()
    var environment = emptyMap<String, String>()

    // Run needs to be handled separately because the resulting process is not run in the sandbox
    if (command is BazelCommand.Run) {
      environment = command.environment
      logInvocation(processArgs, command.environment, workspaceRoot, originId, shouldLogInvocation = shouldLogInvocation)
    } else {
      logInvocation(processArgs, null, null, originId, shouldLogInvocation = shouldLogInvocation)
    }

    val process =
      processSpawner
        .spawnProcessBlocking(
          command = processArgs,
          environment = environment,
          redirectErrorStream = false,
          workDirectory = workspaceRoot.toString(),
        )
    createdProcessIdDeferred?.complete(process.pid)

    val outputLogger = bspClientLogger.takeIf { logProcessOutput }?.copy(originId = originId)

    return BazelProcess(
      process,
      outputLogger,
      serverPidFuture,
      finishCallback,
    )
  }

  private fun envToString(environment: Map<String, String>): String = environment.entries.joinToString(" ") { "${it.key}=${it.value}" }

  private fun logInvocation(
    processArgs: List<String>,
    processEnv: Map<String, String>?,
    directory: Path?,
    originId: String?,
    shouldLogInvocation: Boolean,
  ) {
    if (!shouldLogInvocation) return
    val envString = processEnv?.let { envToString(it) }
    val directoryString = directory?.let { "cd $it &&" }
    val processArgsString = processArgs.joinToString("' '", "'", "'")
    listOfNotNull("Invoking:", envString, directoryString, processArgsString)
      .joinToString(" ")
      .also { LOGGER.info(it) }
      .also { bspClientLogger?.copy(originId = originId)?.message(it) }
  }
}
