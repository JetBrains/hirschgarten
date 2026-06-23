package org.jetbrains.bazel.bazelrunner

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.GeneralCommandLine.ParentEnvironmentType
import com.intellij.execution.configurations.PtyCommandLine
import java.nio.file.Path

internal class DefaultBazelProcessLauncher(private val workspaceRoot: Path, private val parentEnvironment: Map<String, String>) :
  BazelProcessLauncher {
  override fun launchProcess(executionDescriptor: BazelCommandExecutionDescriptor): Process {
    val ptyTermSize = executionDescriptor.ptyTermSize
    val commandLine = if (ptyTermSize != null) {
      PtyCommandLine(executionDescriptor.command).withConsoleMode(true).withInitialColumns(ptyTermSize.columns)
        .withInitialRows(ptyTermSize.rows)
    }
    else {
      GeneralCommandLine(executionDescriptor.command)
    }
    commandLine.withParentEnvironmentType(ParentEnvironmentType.NONE)
    // PtyCommandLine sets the environment variable `TERM` if not set already in the environment provided. To avoid having different
    // values for the environment variables in our bazel invocations, we `TERM` to a sensible value so that PtyCommandLine will not
    // change the environment we set.
    // While normally nothing should depend on the value `TERM`, we have to work around bazel issue
    // https://github.com/bazelbuild/bazel/issues/29956 which is not fixed in all versions of bazel in use.
    commandLine.withEnvironment(mapOf("TERM" to "xterm-256color") + parentEnvironment + executionDescriptor.environment)
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
