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
    commandLine.withEnvironment(parentEnvironment + executionDescriptor.environment)
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
