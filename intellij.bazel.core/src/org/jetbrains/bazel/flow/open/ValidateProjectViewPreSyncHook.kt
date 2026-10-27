package org.jetbrains.bazel.flow.open

import com.intellij.build.events.MessageEvent
import com.intellij.openapi.util.text.LineColumn
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.util.startOffset
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.languages.projectview.ProjectViewService
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewProblem
import org.jetbrains.bazel.progress.syncConsole
import org.jetbrains.bazel.sync.ProjectPreSyncHook
import org.jetbrains.bazel.sync.ProjectSyncAbortException
import org.jetbrains.bazel.sync.withSubtask

internal class ValidateProjectViewPreSyncHook : ProjectPreSyncHook {
  override suspend fun onPreSync(environment: ProjectPreSyncHook.ProjectPreSyncHookEnvironment) {
    val project = environment.project
    environment.withSubtask(BazelPluginBundle.message("project.view.validate.subtask")) { taskId ->
      var hasErrors = false
      ProjectViewService
        .getInstance(project)
        .forceReparseCurrentProjectViewFiles { problem ->
          val start = problem.start()
          project.syncConsole.addDiagnosticMessage(
            taskId = taskId,
            path = problem.sourcePath,
            line = start?.line ?: -1,
            column = start?.column ?: -1,
            message = problem.message,
            severity = problem.severity.toMessageKind(),
          )
          if (problem.severity == ProjectViewProblem.Severity.Error) hasErrors = true
        }
      if (hasErrors) throw ProjectSyncAbortException()
    }
  }

  private fun ProjectViewProblem.start(): LineColumn? {
    val element = element ?: return null
    return StringUtil.offsetToLineColumn(element.containingFile.viewProvider.contents, element.startOffset)
  }

  private fun ProjectViewProblem.Severity.toMessageKind() = when (this) {
    ProjectViewProblem.Severity.Error -> MessageEvent.Kind.ERROR
    ProjectViewProblem.Severity.Warning, ProjectViewProblem.Severity.WeakWarning -> MessageEvent.Kind.WARNING
  }
}
