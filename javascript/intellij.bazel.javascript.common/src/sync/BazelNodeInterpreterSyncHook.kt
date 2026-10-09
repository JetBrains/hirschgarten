package org.jetbrains.bazel.javascript.sync

import com.intellij.ide.util.PropertiesComponent
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef
import com.intellij.javascript.nodejs.interpreter.local.NodeJsLocalInterpreter
import com.intellij.javascript.nodejs.interpreter.local.NodeJsLocalInterpreterManager
import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.progress.syncConsole
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.withSubtask
import java.nio.file.Path
import kotlin.io.path.exists

/**
 * Selects the Node.js runtime of the Bazel `rules_nodejs` toolchain as the project Node.js interpreter,
 * so that IDE features relying on Node.js (TypeScript service, JS test runners, ...) use the same version as Bazel.
 */
internal class BazelNodeInterpreterSyncHook : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHook.ProjectSyncHookEnvironment) {
    val platform = NodePlatform.host() ?: return
    val externalDir = environment.server.bazelInfo.outputBase.resolve("external")
    val toolchain = withContext(Dispatchers.IO) { findBazelNodeToolchain(externalDir, platform) } ?: return

    environment.withSubtask("Configure Node.js interpreter") { taskId ->
      val message =
        if (selectBazelNodeInterpreter(environment.project, toolchain.nodeBinary)) {
          "Using Node.js interpreter of Bazel toolchain '${toolchain.repositoryName}': ${toolchain.nodeBinary}"
        }
        else {
          "Keeping the Node.js interpreter selected in settings, Bazel toolchain interpreter is available at ${toolchain.nodeBinary}"
        }
      environment.project.syncConsole.addMessage(taskId, message)
    }
  }
}

private const val LAST_SELECTED_INTERPRETER_KEY = "bazel.javascript.selectedNodeInterpreter"

/**
 * Registers [nodeBinary] as a local Node.js interpreter and selects it for [project].
 *
 * Returns `false` without changing the selection if the user switched to another interpreter after a previous sync
 * selected the Bazel one.
 */
private suspend fun selectBazelNodeInterpreter(project: Project, nodeBinary: Path): Boolean {
  val interpreter = NodeJsLocalInterpreter(nodeBinary.toString())
  val properties = PropertiesComponent.getInstance(project)
  val lastSelectedPath = properties.getValue(LAST_SELECTED_INTERPRETER_KEY)
  // The previous runtime disappears when Bazel downloads another Node.js version or after `bazel clean --expunge`.
  // The IDE may then have switched to another interpreter on its own, which is not a choice to preserve.
  val lastSelectedIsStale = lastSelectedPath != null && withContext(Dispatchers.IO) { !Path.of(lastSelectedPath).exists() }

  return edtWriteAction {
    val interpreterManager = NodeJsInterpreterManager.getInstance(project)
    val current = interpreterManager.interpreter
    val currentPath = NodeJsLocalInterpreter.tryCast(current)?.interpreterSystemIndependentPath
    if (lastSelectedPath != null && !lastSelectedIsStale && current != null && currentPath != lastSelectedPath) {
      return@edtWriteAction false
    }

    val localInterpreterManager = NodeJsLocalInterpreterManager.getInstance()
    val knownInterpreters =
      localInterpreterManager.interpreters.filterNot { lastSelectedIsStale && it.interpreterSystemIndependentPath == lastSelectedPath }
    localInterpreterManager.interpreters = if (interpreter in knownInterpreters) knownInterpreters else knownInterpreters + interpreter

    interpreterManager.setInterpreterRef(NodeJsInterpreterRef.create(interpreter))
    properties.setValue(LAST_SELECTED_INTERPRETER_KEY, interpreter.interpreterSystemIndependentPath)
    true
  }
}
