package org.jetbrains.bazel.clion.run.debug

import com.intellij.execution.ExecutionException
import com.intellij.execution.RunCanceledByUserException
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.clion.shared.run.debug.CcDebugInfoChoice
import org.jetbrains.bazel.label.Label

private const val DEFAULT_PROJECT_VIEW_NAME = ".projectview"

internal enum class CcDebugInfoDecision { CONTINUE, RETRY_WITH_DBG }

/**
 * Asks the user how to continue after [checkCcDebugInfo] found issues. Does not ask if the user
 * dismissed the dialog for the project. Throws [RunCanceledByUserException] if the user aborts
 * the debug session.
 *
 * @param result null if the check did not run
 * @param allowRetry false hides the option to retry with `--compilation_mode=dbg`
 */
@Throws(ExecutionException::class)
internal suspend fun resolveDebugInfoIssues(
  project: Project,
  label: Label,
  result: CcDebugInfoCheck.Result?,
  allowRetry: Boolean,
): CcDebugInfoDecision {
  if (result == null || result.issues.isEmpty() || isCcDebugInfoDialogDismissed(project)) {
    return CcDebugInfoDecision.CONTINUE
  }

  // the quick fix writes to the project view file, thus it needs the file
  val projectViewFile = findProjectViewFile(project)
  val canUseDbg = allowRetry && projectViewFile != null && result.isFixableByCompilationMode

  return when (askCcDebugInfoChoice(project, label, result, projectViewFile?.name ?: DEFAULT_PROJECT_VIEW_NAME, canUseDbg)) {
    CcDebugInfoChoice.DEBUG_ANYWAY -> CcDebugInfoDecision.CONTINUE
    CcDebugInfoChoice.ALWAYS_DEBUG -> {
      dismissCcDebugInfoDialog(project)
      CcDebugInfoDecision.CONTINUE
    }
    CcDebugInfoChoice.USE_DBG -> CcDebugInfoDecision.RETRY_WITH_DBG
    CcDebugInfoChoice.OPEN_PROJECT_VIEW -> {
      if (projectViewFile != null) openProjectViewAtBuildFlags(project, projectViewFile)
      throw RunCanceledByUserException()
    }
    CcDebugInfoChoice.CANCEL -> throw RunCanceledByUserException()
  }
}
