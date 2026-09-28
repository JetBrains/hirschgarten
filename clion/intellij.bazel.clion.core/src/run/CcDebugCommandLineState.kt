package org.jetbrains.bazel.clion.run

import com.intellij.cidr.debugger.profiles.CidrDebugProfileManager
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.util.system.OS
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.jetbrains.cidr.cpp.execution.debugger.backend.CLionGdbDebugProfileType
import com.jetbrains.cidr.cpp.execution.debugger.backend.CLionLldbDebugProfileType
import com.jetbrains.cidr.cpp.toolchains.CPPEnvironment
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains
import com.jetbrains.cidr.execution.CidrCommandLineState
import com.jetbrains.cidr.execution.CidrLauncher
import com.jetbrains.cidr.execution.TrivialInstaller
import com.jetbrains.cidr.execution.TrivialRunParameters
import com.jetbrains.cidr.execution.debugger.CidrLocalDebugProcess
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver
import com.jetbrains.cidr.execution.runOnEDT
import org.jetbrains.bazel.run.state.GenericRunState
import org.jetbrains.bazel.sync.environment.projectCtx
import org.jetbrains.bazel.utils.ExecutableInfo
import java.nio.file.Path

private val PROC_CWD: Path = Path.of("/proc", "self", "cwd")

// the environment does not affect the debugger
private val DEBUG_ENVIRONMENT = CPPEnvironment(CPPToolchains.Toolchain(OS.CURRENT))

internal class CcDebugCommandLineState(
  environment: ExecutionEnvironment,
  val bazelState: GenericRunState,
) : CidrCommandLineState(environment, CcCidrLauncher(environment.project, bazelState)) {

  // set by org.jetbrains.bazel.clion.run.CcDebugRunner
  var executionInfo: ExecutableInfo? = null
}

private class CcCidrLauncher(private val project: Project, private val bazelState: GenericRunState) : CidrLauncher() {

  override fun getProject(): Project = project

  override fun createDebugProcess(state: CommandLineState, session: XDebugSession): XDebugProcess {
    val ccState = state as CcDebugCommandLineState

    // should always be set by org.jetbrains.bazel.clion.run.CcDebugRunner before this is reached
    val executionInfo = requireNotNull(ccState.executionInfo)

    // should always be present due to org.jetbrains.bazel.clion.run.CcRunHandler.DebugProfileEnabler
    val debugProfile = requireNotNull(CidrDebugProfileManager.getInstance().getCurrentDebugProfile(project))

    // TODO: when can these be null and what to do then?
    val executionRoot = requireNotNull(project.projectCtx.bazelExecPath)
    val workspaceRoot = requireNotNull(project.projectCtx.projectRootDir?.toNioPathOrNull())

    val debugDriver = debugProfile.createDriverConfiguration(
      project = project,
      isElevated = false,
      isEmulateTerminal = false,
      environment = DEBUG_ENVIRONMENT,
    )

    // external paths must be mapped before the workspace root, since one is a prefix of the other
    val sourceMappings = mapOf(
      // /proc/self/cwd mappings used on linux
      PROC_CWD.resolve("external") to executionRoot.resolve("external"),
      PROC_CWD to workspaceRoot,

      // execution root mappings used on Windows
      executionRoot.resolve("external") to executionRoot.resolve("external"),
      executionRoot to workspaceRoot,

      // relative mappings used on macOS
      Path.of("external") to executionRoot.resolve("external"),
    )

    val prentEnvironment = if (bazelState.env.isPassParentEnvs) {
      GeneralCommandLine.ParentEnvironmentType.CONSOLE
    } else {
      GeneralCommandLine.ParentEnvironmentType.NONE
    }

    val commandLine = GeneralCommandLine(executionInfo.binary.toString())
      .withWorkingDirectory(executionInfo.workingDir)
      .withEnvironment(executionInfo.envVars)
      .withEnvironment(bazelState.env.envs) // custom environment variables are not preserved by the script file
      .withParentEnvironmentType(prentEnvironment)
      .withParameters(executionInfo.args)

    val parameters = TrivialRunParameters(debugDriver, TrivialInstaller(commandLine))

    val process = runOnEDT {
      CidrLocalDebugProcess(parameters, session, state.consoleBuilder)
    }

    process.postCommand { driver ->
      when (debugProfile.type.getId()) {
        CLionLldbDebugProfileType.ID -> configureLldbDriver(driver, sourceMappings)
        CLionGdbDebugProfileType.ID -> configureGdbDriver(driver, sourceMappings)
      }
    }

    return process
  }

  override fun createProcess(state: CommandLineState): ProcessHandler? {
    throw UnsupportedOperationException("run is handled by the generic Bazel handler")
  }
}

private fun configureGdbDriver(driver: DebuggerDriver, sourceMappings: Map<Path, Path>) {
  for ((from, to) in sourceMappings) {
    driver.executeInterpreterCommand("set substitute-path \"$from\" \"$to\"")
  }
}

private fun configureLldbDriver(driver: DebuggerDriver, sourceMappings: Map<Path, Path>) {
  val command = buildString {
    append("settings set --global target.source-map")
    for ((from, to) in sourceMappings) {
      append(" \"").append(from).append("\" \"").append(to).append('"')
    }
  }

  driver.executeInterpreterCommand(command)
}
