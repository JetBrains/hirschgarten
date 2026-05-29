package org.jetbrains.bazel.jvm.run

import com.intellij.debugger.impl.attach.JavaDebuggerAttachUtil
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.bazel.run.BazelProcessHandler
import org.jetbrains.bazel.server.bep.TestXmlParser
import org.jetbrains.bazel.sync.environment.projectCtx
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService
import org.jetbrains.bazel.util.BspClientTestNotifier
import org.jetbrains.bsp.protocol.TaskId
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// See: https://bazel.build/reference/test-encyclopedia
private const val BAZEL_TEST_FILTER_ENV = "TESTBRIDGE_TEST_ONLY"

/**
 * See [ScriptPathBeforeRunTaskProvider], we have to pass [env] to the script process manually
 * because Bazel doesn't include environment variable setup in the generated script.
 */
internal suspend fun runWithScriptPath(
  taskId: TaskId,
  scriptPath: Path,
  project: Project,
  pidDeferred: CompletableDeferred<Long?>,
  handler: BazelProcessHandler,
  env: Map<String, String>,
  additionalScriptParameters: List<String>,
  isTest: Boolean,
  testFilter: String?,
  processHandlerCreated: suspend (OSProcessHandler) -> Unit,
) {
  val parentEnvironment = if (isTest) {
    // Bazel tests don't receive the full environment because of sandboxing
    GeneralCommandLine.ParentEnvironmentType.NONE
  } else {
    GeneralCommandLine.ParentEnvironmentType.CONSOLE
  }
  val commandLine =
    GeneralCommandLine()
      .withExePath(scriptPath.toString())
      .withParentEnvironmentType(parentEnvironment)
      .withEnvironment(env)
      .withParameters(additionalScriptParameters)
  if (testFilter != null) {
    commandLine.environment[BAZEL_TEST_FILTER_ENV] = testFilter
  }

  val scriptHandler = OSProcessHandler(commandLine)
  scriptHandler.addProcessListener(
    object : ProcessListener {
      private val ansiEscapeDecoder = AnsiEscapeDecoder()

      override fun onTextAvailable(e: ProcessEvent, outputType: Key<*>) {
        ansiEscapeDecoder.escapeText(e.text, outputType) { text, type ->
          handler.notifyTextAvailable(text, type)
        }
      }
    },
  )

  // necessary for terminating the debug process from scriptHandler when handler.destroyProcess() is called
  handler.addProcessListener(
    object : ProcessListener {
      override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {
        if (willBeDestroyed) {
          scriptHandler.destroyProcess()
        }
      }
    },
  )

  processHandlerCreated(scriptHandler)

  scriptHandler.startNotify()

  val pid: Long = scriptHandler.process.pid()
  // We run the bash script generated via --script_path, which then starts the JVM via Unix's exec: https://en.wikipedia.org/wiki/Exec_(system_call)
  // Since the PID of the bash process is the same as that of the JVM, we wait for bash script completion before returning the pid to IDEA
  withTimeoutOrNull(5.seconds) {
    while (scriptHandler.process.isAlive) {
      if (JavaDebuggerAttachUtil.isAttachable(pid.toString())) break
      delay(50.milliseconds)
    }
  }

  runInterruptible(Dispatchers.IO) {
    pidDeferred.complete(pid)
    scriptHandler.waitFor()
    findXmlOutputAndReport(taskId, scriptPath, project)
  }
}

private fun findXmlOutputAndReport(taskId: TaskId, scriptPath: Path, project: Project) {
  val scriptContent = scriptPath.readText()

  val execRoot = project.projectCtx.bazelExecPath ?: return
  val xmlPath =
    TEST_XML_OUTPUT_FILE_REGEX
      .find(scriptContent)
      ?.groups
      ?.get("path")
      ?.value ?: return
  val absoluteXmlPath = Path.of(execRoot, xmlPath)

  val taskHandler = BazelTaskEventsService.getInstance(project)
  val testNotifier = BspClientTestNotifier(taskHandler)

  TestXmlParser(testNotifier).parseAndReport(taskId /* ??? */, absoluteXmlPath)
}

private val TEST_XML_OUTPUT_FILE_REGEX = Regex("XML_OUTPUT_FILE=(?<path>\\S+)")
