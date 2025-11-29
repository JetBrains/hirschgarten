package org.jetbrains.bazel.jvm.run

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.jetbrains.bazel.flow.sync.bazelPaths.BazelBinPathService
import org.jetbrains.bazel.logger.BspClientTestNotifier
import org.jetbrains.bazel.run.BazelProcessHandler
import org.jetbrains.bazel.server.bep.TestXmlParser
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.readText

// See: https://bazel.build/reference/test-encyclopedia
private const val BAZEL_TEST_FILTER_ENV = "TESTBRIDGE_TEST_ONLY"

/**
 * See [ScriptPathBeforeRunTaskProvider], we have to pass [env] to the script process manually
 * because Bazel doesn't include environment variable setup in the generated script.
 */
suspend fun runWithScriptPath(
  scriptPath: Path,
  project: Project,
  originId: UUID,
  pidDeferred: CompletableDeferred<Long?>,
  handler: BazelProcessHandler,
  env: Map<String, String>,
  testFilter: String? = null,
) {
  val commandLine =
    GeneralCommandLine()
      .withExePath(scriptPath.toString())
      // don't inherit IntelliJ's environment variables as the script should be self-contained
      .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.NONE)
      .withEnvironment(env)
  if (testFilter != null) {
    commandLine.environment[BAZEL_TEST_FILTER_ENV] = testFilter
  }

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
          scriptHandler.destroyProcess()
        }
      }
    },
  )

  scriptHandler.startNotify()
  runInterruptible(Dispatchers.IO) {
    pidDeferred.complete(scriptHandler.process.pid())
    scriptHandler.waitFor()
    findXmlOutputAndReport(scriptPath, project, originId)
  }
}

private fun findXmlOutputAndReport(scriptPath: Path, project: Project, originId: UUID) {
  val scriptContent = scriptPath.readText()

  val execRoot = BazelBinPathService.getInstance(project).bazelExecPath ?: return
  val xmlPath =
    TEST_XML_OUTPUT_FILE_REGEX
      .find(scriptContent)
      ?.groups
      ?.get("path")
      ?.value ?: return
  val absoluteXmlPath = Path.of(execRoot, xmlPath).toUri().toString()

  val taskHandler = BazelTaskEventsService.getInstance(project)
  val testNotifier = BspClientTestNotifier(taskHandler, originId.toString())

  TestXmlParser(testNotifier).parseAndReport(absoluteXmlPath)
}

private val TEST_XML_OUTPUT_FILE_REGEX = Regex("XML_OUTPUT_FILE=(?<path>\\S+)")
