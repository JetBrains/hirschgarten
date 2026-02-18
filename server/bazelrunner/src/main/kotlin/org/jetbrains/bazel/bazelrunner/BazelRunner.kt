package org.jetbrains.bazel.bazelrunner

import org.jetbrains.bazel.bazelrunner.params.BazelFlag.color
import org.jetbrains.bazel.bazelrunner.params.BazelFlag.curses
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.BazelTaskEventsHandler
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.bsp.protocol.asLogger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.pathString

/**
 * Runs Bazel commands with proper repository configuration.
 */
class BazelRunner(
  private val taskEventsHandler: BazelTaskEventsHandler?,
  val workspaceRoot: Path,
  val bazelProcessLauncher: BazelProcessLauncher = DefaultBazelProcessLauncher(workspaceRoot),
) {
  companion object {
    private val LOGGER = LoggerFactory.getLogger(BazelRunner::class.java)
  }

  class CommandBuilder(workspaceContext: WorkspaceContext) {
    private val bazelBinary = workspaceContext.bazelBinary?.pathString ?: "bazel"
    var inheritWorkspaceOptions = false

    fun clean(builder: BazelCommand.Clean.() -> Unit = {}) = BazelCommand.Clean(bazelBinary).apply { builder() }

    fun shutDown(builder: BazelCommand.ShutDown.() -> Unit = {}) = BazelCommand.ShutDown(bazelBinary).apply { builder() }

    fun info(builder: BazelCommand.Info.() -> Unit = {}) = BazelCommand.Info(bazelBinary).apply { builder() }

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
      BazelCommand.Query(bazelBinary, allowManualTargetsSync).apply { builder() }

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

    if (command.ptyTermSize != null) {
      command.options.addAll(
        listOf(
          curses(true),
          color(true),
        ),
      )
    }
    else {
      command.options.addAll(
        listOf(
          curses(false),
          color(true),
          "--noprogress_in_terminal_title",
        ),
      )
    }

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
    taskId: TaskId?,
    logProcessOutput: Boolean = true,
  ): BazelProcess = runBazelCommand(command.buildExecutionDescriptor(), taskId, logProcessOutput)

  fun runBazelCommand(
    executionDescriptor: BazelCommandExecutionDescriptor,
    taskId: TaskId?,
    logProcessOutput: Boolean = true,
  ): BazelProcess {
    val finishCallback = executionDescriptor.finishCallback
    val processArgs = executionDescriptor.command
    val environment = executionDescriptor.environment

    val outputLogger = taskId?.let { taskEventsHandler.takeIf { logProcessOutput }?.asLogger(taskId) }
    if (outputLogger != null) {
      val log = "${envToString(environment)} ${processArgs.joinToString(" ")}"
      outputLogger.message(log)
    }

    val process = bazelProcessLauncher.launchProcess(executionDescriptor)

    return BazelProcess(
      process,
      outputLogger,
      finishCallback,
    ).also {
      it.writeBazelLog {
        appendLine(executionDescriptor.command.joinToString(" "))
        appendLine(envToString(environment))
      }
    }
  }

  private fun envToString(environment: Map<String, String>): String = environment.entries.joinToString(" ") { "${it.key}=${it.value}" }
}
