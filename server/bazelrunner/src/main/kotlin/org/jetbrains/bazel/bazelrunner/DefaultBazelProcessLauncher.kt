package org.jetbrains.bazel.bazelrunner

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PtyCommandLine
import org.jetbrains.bazel.server.bsp.info.BspInfo
import org.jetbrains.bazel.server.bsp.utils.InternalAspectsResolver
import java.nio.file.Path

class DefaultBazelProcessLauncher(private val workspaceRoot: Path) : BazelProcessLauncher {
  override fun launchProcess(executionDescriptor: BazelCommandExecutionDescriptor): Process {
    val ptyTermSize = executionDescriptor.ptyTermSize
    val commandLine = if (ptyTermSize != null) {
      PtyCommandLine(executionDescriptor.command).withConsoleMode(true).withInitialColumns(ptyTermSize.columns)
        .withInitialRows(ptyTermSize.rows)
    }
    else {
      GeneralCommandLine(executionDescriptor.command)
    }
    commandLine.withWorkingDirectory(workspaceRoot)
    commandLine.withEnvironment(executionDescriptor.environment)
    commandLine.withRedirectErrorStream(false)

    return commandLine.createProcess()
  }
}

object DefaultBazelProcessLauncherProvider : BazelProcessLauncherProvider {
  override fun createBazelProcessLauncher(
    workspaceRoot: Path,
    bspInfo: BspInfo,
    aspectsResolver: InternalAspectsResolver,
    bazelInfoResolver: BazelInfoResolver,
  ): BazelProcessLauncher = DefaultBazelProcessLauncher(workspaceRoot)
}
