package org.jetbrains.bazel.flow.open

import com.intellij.build.events.MessageEvent
import com.intellij.openapi.components.serviceAsync
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.languages.projectview.ProjectViewService
import org.jetbrains.bazel.languages.projectview.imports.Import
import org.jetbrains.bazel.progress.syncConsole
import org.jetbrains.bazel.sync.ProjectPreSyncHook
import org.jetbrains.bazel.sync.withSubtask

internal class ReparseProjectViewFilePreSyncHook : ProjectPreSyncHook {
  override suspend fun onPreSync(environment: ProjectPreSyncHook.ProjectPreSyncHookEnvironment) {
    val project = environment.project
    environment.withSubtask(BazelPluginBundle.message("project.view.validate.subtask")) { taskId ->
      val projectViewService = project.serviceAsync<ProjectViewService>()
      projectViewService.forceReparseCurrentProjectViewFiles()
      val unresolvedRequiredImports = projectViewService
        .projectView
        .imports
        .filterIsInstance<Import.Unresolved>()
        .filter { it.isRequired }
      unresolvedRequiredImports.forEach {
        project.syncConsole.addDiagnosticMessage(
          taskId = taskId,
          path = it.position?.path,
          line = it.position?.startLine ?: -1,
          column = it.position?.startColumn ?: -1,
          message = BazelPluginBundle.message("project.view.import.unresolved", it.text),
          severity = MessageEvent.Kind.ERROR,
        )
      }
    }
  }
}
