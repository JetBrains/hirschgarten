package org.jetbrains.bazel.clion.run.debug

import com.intellij.cidr.debugger.profiles.CidrDebugProfile
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.util.system.LowLevelLocalMachineAccess
import com.intellij.util.system.OS
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.impl.XDebugSessionImpl
import com.jetbrains.cidr.cpp.execution.debugger.backend.CLionGdbDebugProfileType
import com.jetbrains.cidr.cpp.execution.debugger.backend.CLionLldbDebugProfileType
import com.jetbrains.cidr.cpp.toolchains.CPPEnvironment
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains
import com.jetbrains.cidr.execution.CidrCommandLineState
import com.jetbrains.cidr.execution.CidrConsoleBuilder
import com.jetbrains.cidr.execution.CidrLauncher
import com.jetbrains.cidr.execution.TrivialInstaller
import com.jetbrains.cidr.execution.TrivialRunParameters
import com.jetbrains.cidr.execution.debugger.CidrLocalDebugProcess
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver
import com.jetbrains.cidr.execution.debugger.backend.dap.profile.DapDebugProfileState
import com.jetbrains.cidr.execution.debugger.backend.dap.profile.DapDebugProfileType
import com.jetbrains.cidr.execution.runOnEDT
import org.jetbrains.bazel.clion.sync.CC_TEST_KIND
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.run.commandLine.scriptPathCommandLine
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.state.AbstractGenericTestState
import org.jetbrains.bazel.run.state.GenericRunConfigurationState
import org.jetbrains.bazel.run.state.HasTestFilter
import org.jetbrains.bazel.run.task.BazelTestTaskListener
import org.jetbrains.bazel.server.bep.TestXmlParser
import org.jetbrains.bazel.sync.environment.bazelProjectInfo
import org.jetbrains.bazel.sync.workspace.snapshot.workspaceSnapshotOrNull
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService
import org.jetbrains.bazel.util.BspClientTestNotifier
import org.jetbrains.bazel.utils.ExecutableInfo
import org.jetbrains.bsp.protocol.TaskGroupId
import java.nio.file.Path

private val PROC_CWD: Path = Path.of("/proc", "self", "cwd")

// the environment does not affect the debugger
private val DEBUG_ENVIRONMENT = CPPEnvironment(CPPToolchains.Toolchain(OS.CURRENT))

internal class CcDebugCommandLineState(
  environment: ExecutionEnvironment,
  val bazelState: GenericRunConfigurationState,
) : CidrCommandLineState(environment, CcCidrLauncher(environment, bazelState)) {

  // set by the CcDebugRunner
  var targetInfo: CcDebugTargetInfo? = null
  var executionInfo: ExecutableInfo? = null
  var debugProfile: CidrDebugProfile<*>? = null
}

private class CcCidrLauncher(private val environment: ExecutionEnvironment, private val bazelState: GenericRunConfigurationState) :
  CidrLauncher() {

  override fun getProject(): Project = environment.project

  override fun createDebugProcess(state: CommandLineState, session: XDebugSession): XDebugProcess {
    val ccState = state as CcDebugCommandLineState

    // the CcDebugRunner sets these before the debug process starts
    val targetInfo = requireNotNull(ccState.targetInfo)
    val executionInfo = requireNotNull(ccState.executionInfo)
    var debugProfile = requireNotNull(ccState.debugProfile)

    // TODO: when can these be null and what to do then?
    val executionRoot = requireNotNull(project.workspaceSnapshotOrNull?.bazelInfo?.execRoot)
    val workspaceRoot = requireNotNull(project.bazelProjectInfo.projectRootDir?.toNioPathOrNull())

    if (OS.CURRENT == OS.macOS) {
      // on macos the debugger resolves debug symbols relative to the working directory
      debugProfile = debugProfile.withDapWorkingDir(executionRoot)
    }

    val debugDriver = debugProfile.createDriverConfiguration(
      project = project,
      isElevated = false,
      isEmulateTerminal = false,
      environment = DEBUG_ENVIRONMENT,
    )

    // external paths must be mapped before the workspace root, since one is a prefix of the other
    val sourceMappings = buildMap {
      // /proc/self/cwd mappings used on linux
      putLocalRepoMapping(PROC_CWD, workspaceRoot, targetInfo.repoMapping)
      put(PROC_CWD.resolve("external"), executionRoot.resolve("external"))
      put(PROC_CWD, workspaceRoot)

      // execution root mappings used on Windows
      put(executionRoot.resolve("external"), executionRoot.resolve("external")) // required to specialize the one below
      put(executionRoot, workspaceRoot)

      // relative mappings used on macOS
      putLocalRepoMapping(Path.of(""), workspaceRoot, targetInfo.repoMapping)
      put(Path.of("external"), executionRoot.resolve("external"))
      put(Path.of(""), workspaceRoot)
    }

    val isTest = bazelState is AbstractGenericTestState<*>
    val commandLine = scriptPathCommandLine(
      scriptPath = executionInfo.binary,
      env = executionInfo.envVars + bazelState.env.envs,  // custom environment variables are not preserved by the script file
      isTest = isTest,
      isPassParentEnvs = bazelState.env.isPassParentEnvs,
      testFilter = (bazelState as? HasTestFilter)?.testFilter,
      additionalScriptParameters = executionInfo.args,
    ).withWorkingDirectory(executionInfo.workingDir)

    val parameters = TrivialRunParameters(debugDriver, TrivialInstaller(commandLine))
    val process = runOnEDT {
      if (isTest) {
        createDebugProcessForTest(parameters, session, executionInfo)
      }
      else {
        CidrLocalDebugProcess(parameters, session, state.consoleBuilder)
      }
    }
    process.postCommand { driver ->
      when (debugProfile.type.getId()) {
        CLionLldbDebugProfileType.ID -> configureLldbDriver(driver, sourceMappings)
        CLionGdbDebugProfileType.ID -> configureGdbDriver(driver, sourceMappings)
        DapDebugProfileType.ID -> configureDapDriver(driver, sourceMappings)
      }
    }

    return process
  }

  private fun createDebugProcessForTest(
    parameters: TrivialRunParameters,
    session: XDebugSession,
    executionInfo: ExecutableInfo,
  ): CidrLocalDebugProcess {
    val process = CidrLocalDebugProcess(parameters, session, CcTestConsoleBuilder(environment))
    val testConsole = process.getConsole() as? SMTRunnerConsoleView
    val properties = testConsole?.properties as? SMTRunnerConsoleProperties
    val restartAction = properties?.createRerunFailedTestsAction(testConsole)
    restartAction?.let { (session as? XDebugSessionImpl)?.addRestartActions(restartAction) }

    val processHandler = process.processHandler
    processHandler.addProcessListener(
      object : ProcessListener {
        override fun processTerminated(event: ProcessEvent) {
          val taskHandler = BazelTaskEventsService.getInstance(project)
          val runListener = BazelTestTaskListener(processHandler, null)
          val taskGroupId = TaskGroupId(environment.toString())
          taskHandler.saveListener(taskGroupId, runListener)
          try {
            val testXml = executionInfo.testXml ?: return
            TestXmlParser(BspClientTestNotifier(taskHandler)).parseAndReport(taskGroupId.task("test"), testXml, CC_TEST_KIND)
          }
          finally {
            taskHandler.removeListener(taskGroupId)
          }
        }
      },
    )
    return process
  }

  override fun createProcess(state: CommandLineState): ProcessHandler? {
    throw UnsupportedOperationException("run is handled by the generic Bazel handler")
  }
}

private class CcTestConsoleBuilder(private val environment: ExecutionEnvironment) : CidrConsoleBuilder(environment.project, null, null) {
  override fun createConsole(): ConsoleView {
    val configuration = BazelRunConfiguration.get(environment)
    val properties = configuration.createTestConsoleProperties(environment.executor)
    return createConsole(configuration.type, properties)
  }
}

private fun MutableMap<Path, Path>.putLocalRepoMapping(execroot: Path, workspace: Path, repoMapping: RepoMapping) {
  for ([name, path] in repoMapping.getLocalRepositories().localRepositories) {
    put(execroot.resolve("external", name), workspace.resolve(path))
  }
}

private fun configureGdbDriver(driver: DebuggerDriver, sourceMappings: Map<Path, Path>) {
  for ([from, to] in sourceMappings) {
    driver.executeInterpreterCommand("set substitute-path \"$from\" \"$to\"")
  }
}

private fun configureLldbDriver(driver: DebuggerDriver, sourceMappings: Map<Path, Path>) {
  val command = buildString {
    append("settings set --global -- target.source-map")
    for ((from, to) in sourceMappings) {
      append(" \"").append(from).append("\" \"").append(to).append('"')
    }
  }

  driver.executeInterpreterCommand(command)
}

/**
 * Gives a DAP debug profile without a debugger working directory the [executionRoot] as that directory.
 * On macOS, the binary records its object files relative to the execution root. LLDB finds them relative
 * to its working directory.
 */
private fun CidrDebugProfile<*>.withDapWorkingDir(executionRoot: Path): CidrDebugProfile<*> {
  val dapState = state as? DapDebugProfileState ?: return this
  
  // user has set a custom working directory
  if (!dapState.debuggerWorkingDir.isNullOrBlank()) return this

  val newState = DapDebugProfileState()
  newState.copyFrom(dapState)
  newState.debuggerWorkingDir = executionRoot.toString()

  @Suppress("UNCHECKED_CAST")
  return (this as CidrDebugProfile<DapDebugProfileState>).copy(state = newState)
}

private fun configureDapDriver(driver: DebuggerDriver, sourceMappings: Map<Path, Path>) {
  // the DAP driver gives an IDE path the debugger path of the first mapping with the longest IDE path,
  // and only a relative debugger path matches both `lib/impl.cc` and the `./lib/hdr.h` that the compiler records for a header
  val relativeFirst = sourceMappings.entries.sortedBy { it.key.isAbsolute }
  for ([index, mapping] in relativeFirst.withIndex()) {
    driver.addPathMapping(index, mapping.key.toString(), mapping.value.toString())
  }
}

