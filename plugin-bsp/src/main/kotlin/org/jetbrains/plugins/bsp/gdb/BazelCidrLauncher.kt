package org.jetbrains.plugins.bsp.gdb

import ch.epfl.scala.bsp4j.CompileParams
import ch.epfl.scala.bsp4j.OutputPathsParams
import com.google.gson.JsonPrimitive
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.jetbrains.cidr.execution.CidrConsoleBuilder
import com.jetbrains.cidr.execution.CidrLauncher
import com.jetbrains.cidr.execution.TrivialInstaller
import com.jetbrains.cidr.execution.TrivialRunParameters
import com.jetbrains.cidr.execution.debugger.CidrLocalDebugProcess
import com.jetbrains.cidr.execution.runOnEDT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.utils.extractData
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.impl.server.connection.connection
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import java.util.UUID

class BazelCidrLauncher(val configuration: BspRunConfiguration, val env: ExecutionEnvironment) :
  CidrLauncher() {

  override fun createProcess(state: CommandLineState): ProcessHandler {
    TODO("Not yet implemented")
  }

  override fun createDebugProcess(state: CommandLineState, session: XDebugSession): XDebugProcess {

    val project=configuration.project
    val target = configuration.targets.firstOrNull() ?: throw ExecutionException("Cannot parse run configuration target.")
    // first for cpp program we need to build it again with debug mode
    val compileParams = CompileParams(listOf(target))
    compileParams.originId = UUID.randomUUID().toString()
    compileParams.arguments = getExtraDebugFlags(env)


    val executableToDebug = runBlocking {
      project.connection.runWithServer { server: JoinedBuildServer, capabilities: BazelBuildServerCapabilities ->
        val result =server.buildTargetCompile(compileParams).get()
        (result.data as JsonPrimitive).asString

      }
    }
    // todo: for now we just assume that it is lldb on mac
    val commandLine: GeneralCommandLine =
      GeneralCommandLine(executableToDebug.toString())
    // todo : add run flags
    //    commandLine.addParameters(handlerState.getExeFlagsState().getFlagsForExternalProcesses())
    //    commandLine.addParameters(handlerState.getTestArgs())
    //    updateCommandlineWithEnvironmentData(commandLine);

    val debuggerDriver = BazelLLDBDriverConfiguration(project, project.rootDir.toNioPathOrNull()!!)
    val parameters = TrivialRunParameters(debuggerDriver, TrivialInstaller(commandLine))

    state.consoleBuilder = CidrConsoleBuilder(configuration.getProject(), null, null)

    // state.addConsoleFilters(*getConsoleFilters().toTypedArray<Filter>())
    return runOnEDT { CidrLocalDebugProcess(parameters, session, state.consoleBuilder) }
  }

  override fun getProject(): Project = configuration.project


  private fun getExtraDebugFlags(env: ExecutionEnvironment): List<String> {
    val project=configuration.project
    // todo: handle gdb server
    val flagsBuilder = mutableListOf<String>()
    // todo: the condition should only be true when we are using lldb
    if (true) {
      //todo: original code was WorkspaceRoot.fromProject(env.getProject())
      flagsBuilder.add("--copt=-fdebug-compilation-dir=" + env.project.rootDir.path)

      if (SystemInfo.isMac) {
        flagsBuilder.add("--linkopt=-Wl,-oso_prefix,.")
      }
    }
    flagsBuilder.add("--compilation_mode=dbg")
    flagsBuilder.add("--copt=-O0")
    flagsBuilder.add("--copt=-g")
    flagsBuilder.add("--strip=never")
    flagsBuilder.add("--dynamic_mode=off")
    //flagsBuilder.addAll(BlazeGDBServerProvider.getOptionalFissionArguments())

    return flagsBuilder
  }


}
