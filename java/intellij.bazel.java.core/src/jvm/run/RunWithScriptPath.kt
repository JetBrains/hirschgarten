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
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.run.BazelProcessHandler
import org.jetbrains.bazel.run.commandLine.scriptPathCommandLine
import org.jetbrains.bazel.server.bep.TestXmlParser
import org.jetbrains.bazel.sync.workspace.languages.jvm.JAVA_TEST_KIND
import org.jetbrains.bazel.sync.workspace.snapshot.workspaceSnapshotOrNull
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService
import org.jetbrains.bazel.util.BspClientTestNotifier
import org.jetbrains.bsp.protocol.TaskId
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

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
  isPassParentEnvs: Boolean,
  testFilter: String?,
  processHandlerCreated: suspend (OSProcessHandler) -> Unit,
) {
  val commandLine = scriptPathCommandLine(scriptPath, env, isTest, isPassParentEnvs, testFilter, additionalScriptParameters)

  runJvmCommandLine(commandLine, pidDeferred, handler, processHandlerCreated)
  runInterruptible(Dispatchers.IO) {
    findXmlOutputAndReport(taskId, scriptPath, project)
  }
}

/**
 * Starts [commandLine] and copies its output to [handler].
 * When [handler] is destroyed, the process is destroyed too.
 * [pidDeferred] gets the pid when the JVM can take an attach, or after 5 seconds.
 * The function returns when the process ends.
 */
@ApiStatus.Internal
suspend fun runJvmCommandLine(
  commandLine: GeneralCommandLine,
  pidDeferred: CompletableDeferred<Long?>,
  handler: BazelProcessHandler,
  processHandlerCreated: suspend (OSProcessHandler) -> Unit,
) {
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
  }
}

private fun findXmlOutputAndReport(taskId: TaskId, scriptPath: Path, project: Project) {
  val scriptContent = scriptPath.readText()

  val execRoot = project.workspaceSnapshotOrNull?.bazelInfo?.execRoot ?: return
  val xmlPath =
    TEST_XML_OUTPUT_FILE_REGEX
      .find(scriptContent)
      ?.groups
      ?.get("path")
      ?.value ?: return
  val absoluteXmlPath = execRoot.resolve(xmlPath)

  val taskHandler = BazelTaskEventsService.getInstance(project)
  val testNotifier = BspClientTestNotifier(taskHandler)

  TestXmlParser(testNotifier).parseAndReport(
    taskId, /* ??? */
    absoluteXmlPath,
    JAVA_TEST_KIND,
  )
}

private val TEST_XML_OUTPUT_FILE_REGEX = Regex("XML_OUTPUT_FILE=(?<path>\\S+)")
