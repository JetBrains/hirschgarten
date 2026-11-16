package org.jetbrains.bazel.bazelrunner

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.GeneralCommandLine.ParentEnvironmentType
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.openapi.components.service
import org.jetbrains.bazel.commons.BazelHost
import org.jetbrains.bazel.commons.BazelHostPty
import org.jetbrains.bazel.commons.spawnProcessBlocking
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.sync.environment.BazelApplicationContextService
import java.nio.file.Path

private const val PTY_COLUMNS = 512
private const val PTY_ROWS = 25

internal class DefaultBazelProcessLauncher(private val workspaceRoot: Path, private val parentEnvironment: Map<String, String>) :
  BazelProcessLauncher {
  private val host = BazelHost.of(workspaceRoot)

  override fun launchProcess(executionDescriptor: BazelCommandExecutionDescriptor): Process {
    val usePty = executionDescriptor.enablePty && service<BazelApplicationContextService>().enablePty && BazelFeatureFlags.usePty
    // PtyCommandLine sets the environment variable `TERM` if not set already in the environment provided. To avoid having different
    // values for the environment variables in our bazel invocations, we `TERM` to a sensible value so that PtyCommandLine will not
    // change the environment we set.
    // While normally nothing should depend on the value `TERM`, we have to work around bazel issue
    // https://github.com/bazelbuild/bazel/issues/29956 which is not fixed in all versions of bazel in use.
    val environment = mapOf("TERM" to "xterm-256color") +
                      parentEnvironment +
                      BazelProxyForwarding.environment() +
                      executionDescriptor.environment

    if (!host.isLocal) {
      // Bazel runs in another environment than the IDE, for example in WSL. The IDE starts it there with the Eel API.
      return host.spawnProcessBlocking(
        command = executionDescriptor.command,
        workingDirectory = workspaceRoot,
        environment = environment,
        pty = if (usePty) BazelHostPty(columns = PTY_COLUMNS, rows = PTY_ROWS) else null,
      )
    }

    val commandLine = if (usePty) {
      PtyCommandLine(executionDescriptor.command)
        .withConsoleMode(true)
        .withInitialColumns(PTY_COLUMNS)
        .withInitialRows(PTY_ROWS)
    }
    else {
      GeneralCommandLine(executionDescriptor.command)
    }
    commandLine.withParentEnvironmentType(ParentEnvironmentType.NONE)
    commandLine.withEnvironment(environment)
    commandLine.withWorkingDirectory(workspaceRoot)
    commandLine.withRedirectErrorStream(false)

    return commandLine.createProcess()
  }
}

internal object DefaultBazelProcessLauncherProvider : BazelProcessLauncherProvider {
  override fun createBazelProcessLauncher(
    workspaceRoot: Path,
    parentEnvironment: Map<String, String>,
  ): BazelProcessLauncher = DefaultBazelProcessLauncher(workspaceRoot, parentEnvironment)
}
