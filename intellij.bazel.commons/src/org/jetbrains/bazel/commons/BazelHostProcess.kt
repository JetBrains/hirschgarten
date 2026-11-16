package org.jetbrains.bazel.commons

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.execution.process.OSProcessUtil
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.runBlockingMaybeCancellable
import com.intellij.platform.eel.EelExecApi
import com.intellij.platform.eel.convertToJVMProcess
import com.intellij.platform.eel.environmentVariables
import com.intellij.platform.eel.provider.asEelPath
import com.intellij.platform.eel.provider.toEelApi
import com.intellij.platform.eel.spawnProcess
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.annotations.ApiStatus
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.cancellation.CancellationException

private val LOG = logger<BazelHost>()

/**
 * The size of the pseudo-terminal of a process.
 */
@ApiStatus.Internal
data class BazelHostPty(val columns: Int, val rows: Int)

/**
 * Starts [command] on this host, which is not the machine of the IDE.
 * For the machine of the IDE, use [com.intellij.execution.configurations.GeneralCommandLine] instead.
 *
 * The caller supplies all values in the form of the host:
 * [command] and [environment] hold host paths, see [BazelHost.toHostPath].
 * Only [workingDirectory] is an IDE path, and it must be a directory on this host.
 * The process gets exactly [environment], it does not inherit the environment of the IDE or of a shell.
 *
 * With [pty], the process runs in a pseudo-terminal, and its error stream is a part of its output stream.
 */
@ApiStatus.Internal
suspend fun BazelHost.spawnProcess(
  command: List<String>,
  workingDirectory: Path?,
  environment: Map<String, String>,
  pty: BazelHostPty? = null,
): Process {
  check(!isLocal) { "Use GeneralCommandLine to start a process on the machine of the IDE" }
  require(command.isNotEmpty()) { "The command is empty" }
  val exec = descriptor.toEelApi().exec
  return exec
    .spawnProcess(command.first())
    .args(command.drop(1))
    .workingDirectory(workingDirectory?.asEelPath())
    .env(environment)
    .interactionOptions(pty?.let { EelExecApi.Pty(it.columns, it.rows) })
    .eelIt()
    .convertToJVMProcess()
}

/** The blocking variant of [spawnProcess]. Do not call it on the event dispatch thread. */
@ApiStatus.Internal
fun BazelHost.spawnProcessBlocking(
  command: List<String>,
  workingDirectory: Path?,
  environment: Map<String, String>,
  pty: BazelHostPty? = null,
): Process = runBlockingMaybeCancellable { spawnProcess(command, workingDirectory, environment, pty) }

/**
 * Kills [process], which the IDE started on this host.
 *
 * On the machine of the IDE, this kills the whole process tree when [killTree] is set.
 * On another host, the process ID of [process] is an ID of that host.
 * The tools of the IDE for a process tree would read it as an ID of the machine of the IDE and kill an unrelated process.
 * So this function kills only [process] itself there, and the host passes the end of the process to its children.
 */
@ApiStatus.Internal
fun BazelHost.killProcess(process: Process, killTree: Boolean) {
  if (isLocal && killTree) {
    OSProcessUtil.killProcessTree(process)
  }
  else {
    process.destroyForcibly()
  }
}

/**
 * Kills the process with the ID [pid] on this host. Returns `true` if the host reports success.
 */
@ApiStatus.Internal
suspend fun BazelHost.killProcess(pid: Long): Boolean {
  if (isLocal) {
    OSProcessUtil.killProcess(Math.toIntExact(pid))
    return true
  }
  return try {
    descriptor.toEelApi().exec.processManagement.kill(pid)
  }
  catch (e: CancellationException) {
    throw e
  }
  catch (e: Exception) {
    LOG.warn("Cannot kill process $pid on $descriptor", e)
    false
  }
}

/**
 * Makes this command line start its process on [host].
 * The function changes nothing for the machine of the IDE.
 *
 * For another host, the process starts through the Eel API, and the classes that take a [GeneralCommandLine],
 * such as [com.intellij.execution.process.OSProcessHandler], work as before.
 * The caller supplies the executable, the parameters and the environment in the form of the host, see [BazelHost.toHostPath].
 * The working directory stays an IDE path, and it must be a directory on the host.
 *
 * A process on another host cannot inherit the environment of the IDE.
 * With [inheritHostEnvironment], it gets the environment of a login shell on the host,
 * and the variables of this command line come on top.
 */
@ApiStatus.Internal
fun <T : GeneralCommandLine> T.onBazelHost(host: BazelHost, inheritHostEnvironment: Boolean = false): T {
  if (host.isLocal) return this
  val pty = (this as? PtyCommandLine)?.ptyOptions?.let { BazelHostPty(it.initialColumns, it.initialRows) }
  setProcessCreator { builder ->
    runBlockingMaybeCancellable {
      val hostEnvironment =
        if (inheritHostEnvironment) host.descriptor.toEelApi().exec.environmentVariables().eelIt().await() else emptyMap()
      host.spawnProcess(
        command = builder.command(),
        workingDirectory = builder.directory()?.let { Path.of(it.path) },
        environment = hostEnvironment + builder.environment(),
        pty = pty,
      )
    }.also { process -> feedInputFile(builder, process) }
  }
  return this
}

/**
 * Does what [GeneralCommandLine.withInput] asks for: the process reads the file as its standard input.
 * A process on another host cannot open a file of the IDE machine, so the IDE copies the content of the file to the process.
 */
private fun feedInputFile(builder: ProcessBuilder, process: Process) {
  val input = builder.redirectInput()
  if (input.type() != ProcessBuilder.Redirect.Type.READ) return
  val file = input.file() ?: return
  AppExecutorUtil.getAppExecutorService().execute {
    try {
      process.outputStream.use { stdin -> Files.copy(file.toPath(), stdin) }
    }
    catch (e: IOException) {
      LOG.warn("Cannot pass $file to the standard input of a process on another host", e)
    }
  }
}
