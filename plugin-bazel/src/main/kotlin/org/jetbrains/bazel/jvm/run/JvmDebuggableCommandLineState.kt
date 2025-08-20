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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.jetbrains.bazel.logger.BspClientTestNotifier
import org.jetbrains.bazel.run.BazelCommandLineStateBase
import org.jetbrains.bazel.run.BazelProcessHandler
import org.jetbrains.bazel.server.bep.TestXmlParser
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService
import org.jetbrains.bsp.protocol.DebugType
import java.io.File
import java.nio.file.Path

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

  suspend fun debugWithScriptPath(
    workingDirectory: String?,
    scriptPath: String,
    handler: BazelProcessHandler,
  ) {
    val commandLine = GeneralCommandLine().withWorkingDirectory(workingDirectory?.let { Path.of(it) }).withExePath(scriptPath)
    val scriptHandler = OSProcessHandler(commandLine)
    scriptHandler.addProcessListener(
      object : ProcessListener {
        override fun onTextAvailable(e: ProcessEvent, outputType: Key<*>) {
          val type = outputType as? ProcessOutputType ?: ProcessOutputType.STDOUT
          handler.notifyTextAvailable(e.text, type)
        }
      },
    )

    // necessary for terminating the debug process from scriptHandler when handler.destroyProcess() is called
    handler.addProcessListener(
      object : ProcessListener {
        override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {
          if (willBeDestroyed) {
            handler.notifyTextAvailable("Debug process will stop", ProcessOutputType.STDOUT)
            scriptHandler.destroyProcess()
          }
        }
      },
    )

    scriptHandler.startNotify()
    runInterruptible(Dispatchers.IO) {
      scriptHandler.waitFor()
      findXmlOutputAndReport(scriptPath)
    }
  }

  private fun findXmlOutputAndReport(scriptPath: String) {
    val scriptContent = File(scriptPath).readText()

    val xmlPath = xmlPathPattern.find(scriptContent)?.groups?.get("path")?.value ?: return
    val absoluteXmlPath = Path.of(environment.project.basePath, xmlPath).toUri().toString()

    val taskHandler = BazelTaskEventsService.getInstance(environment.project)
    val testNotifier = BspClientTestNotifier(taskHandler, originId.toString())

    TestXmlParser(testNotifier).parseAndReport(absoluteXmlPath)
  }
}

private val xmlPathPattern = Regex("XML_OUTPUT_FILE=(?<path>\\S+)")
