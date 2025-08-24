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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.jetbrains.bazel.flow.sync.bazelPaths.BazelBinPathService
import org.jetbrains.bazel.logger.BspClientTestNotifier
import org.jetbrains.bazel.run.BazelCommandLineStateBase
import org.jetbrains.bazel.run.BazelProcessHandler
import org.jetbrains.bazel.server.bep.TestXmlParser
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService
import org.jetbrains.bsp.protocol.DebugType
import java.io.File
import java.nio.file.Path

// Longer timeout to account for Bazel build
private const val DEBUGGER_ATTACH_TIMEOUT: Long = 3 * 60 * 1000

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
      DEBUGGER_ATTACH_TIMEOUT,
    )
  }

  val debugType: DebugType
    get() = DebugType.JDWP(port)

  suspend fun debugWithScriptPath(
    workingDirectory: String?,
    scriptPath: String,
    pidDeferred: CompletableDeferred<Long?>,
    handler: BazelProcessHandler,
  ) {
    val commandLine =
      GeneralCommandLine()
        .withWorkingDirectory(workingDirectory?.let { Path.of(it) })
        .withExePath(scriptPath)
        // don't inherit IntelliJ's environment variables as the script should be self-contained
        .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.NONE)

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
      pidDeferred.complete(scriptHandler.process.pid())
      scriptHandler.waitFor()
      findXmlOutputAndReport(scriptPath)
    }
  }

  private fun findXmlOutputAndReport(scriptPath: String) {
    val scriptContent = File(scriptPath).readText()

    val execRoot = BazelBinPathService.getInstance(environment.project).bazelExecPath ?: return
    val xmlPath =
      TEST_XML_OUTPUT_FILE_REGEX
        .find(scriptContent)
        ?.groups
        ?.get("path")
        ?.value ?: return
    val absoluteXmlPath = Path.of(execRoot, xmlPath).toUri().toString()

    val taskHandler = BazelTaskEventsService.getInstance(environment.project)
    val testNotifier = BspClientTestNotifier(taskHandler, originId.toString())

    TestXmlParser(testNotifier).parseAndReport(absoluteXmlPath)
  }
}

private val TEST_XML_OUTPUT_FILE_REGEX = Regex("XML_OUTPUT_FILE=(?<path>\\S+)")
