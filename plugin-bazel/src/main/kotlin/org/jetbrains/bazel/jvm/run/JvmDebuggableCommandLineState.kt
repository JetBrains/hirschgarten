package org.jetbrains.bazel.jvm.run

import com.intellij.debugger.DefaultDebugEnvironment
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RemoteConnection
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.util.Key
import org.jetbrains.bazel.run.BazelCommandLineStateBase
import org.jetbrains.bazel.run.BazelProcessHandler
import org.jetbrains.bsp.protocol.DebugType

abstract class JvmDebuggableCommandLineState(environment: ExecutionEnvironment, private val port: Int) :
  BazelCommandLineStateBase(environment) {
  fun createDebugEnvironment(environment: ExecutionEnvironment): DefaultDebugEnvironment {
    // create remote connection with client mode
    val remoteConnection =
      RemoteConnection(
        true,
        "localhost",
        port.toString(), // this is the port used
        false,
      )

    return DefaultDebugEnvironment(
      environment,
      this,
      remoteConnection,
      true,
    )
  }

  val debugType: DebugType
    get() = DebugType.JDWP(port)

  fun debugWithScriptPath(
    workingDirectory: String?,
    scriptPath: String,
    handler: BazelProcessHandler,
  ) {
    val commandLine = GeneralCommandLine().withWorkDirectory(workingDirectory).withExePath(scriptPath)
    val scriptHandler = OSProcessHandler(commandLine)
    scriptHandler.addProcessListener(
      object : ProcessListener {
        override fun onTextAvailable(e: ProcessEvent, outputType: Key<*>) {
          val type = outputType as? ProcessOutputType ?: ProcessOutputType.STDOUT
          handler.notifyTextAvailable(e.text, type)
        }
      },
    )

    scriptHandler.startNotify()
    scriptHandler.waitFor()
  }
}
