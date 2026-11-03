package org.jetbrains.bazel.clion.run.debug

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.CcDebugInfo
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsSafe
import kotlinx.coroutines.channels.SendChannel
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.TestOnly
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.bazel.clion.shared.run.debug.CcDebugInfoChoice
import org.jetbrains.bazel.clion.shared.run.debug.CcDebugInfoRequest
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString

private const val DISMISSED_KEY = "bazel.cc.debug.info.dialog.dismissed"

private val TEST_HANDLER_KEY = Key.create<CcDebugInfoDialogTestHandler>("bazel.cc.debug.info.dialog.test.handler")

/** Answers the debug info dialog in a test. */
@ApiStatus.Internal
fun interface CcDebugInfoDialogTestHandler {

  /** @param canUseDbg true if the dialog shows the option to set `--compilation_mode=dbg` */
  fun choose(result: CcDebugInfoCheck.Result, canUseDbg: Boolean): CcDebugInfoChoice
}

/** Sets the [handler] that answers the debug info dialog of the project. Null shows the dialog again. */
@TestOnly
@ApiStatus.Internal
fun setCcDebugInfoDialogHandlerInTests(project: Project, handler: CcDebugInfoDialogTestHandler?) {
  project.putUserData(TEST_HANDLER_KEY, handler)
}

internal fun isCcDebugInfoDialogDismissed(project: Project): Boolean {
  return PropertiesComponent.getInstance(project).getBoolean(DISMISSED_KEY)
}

/** Hides the debug info dialog for the project from now on. */
internal fun dismissCcDebugInfoDialog(project: Project) {
  PropertiesComponent.getInstance(project).setValue(DISMISSED_KEY, true)
}

/**
 * Asks the user to configure the project for debugging after the [CcDebugInfoCheck] found targets without debug info.
 * The frontend shows the dialog.
 *
 * @param projectViewFileName the name of the project view file that the dialog links to
 * @param canUseDbg true shows the option to set `--compilation_mode=dbg`
 */
internal suspend fun askCcDebugInfoChoice(
  project: Project,
  mainTarget: Label,
  result: CcDebugInfoCheck.Result,
  projectViewFileName: @NlsSafe String,
  canUseDbg: Boolean,
): CcDebugInfoChoice {
  project.getUserData(TEST_HANDLER_KEY)?.let { return it.choose(result, canUseDbg) }

  // the repo mapping is in the workspace of the backend
  val targetName = mainTarget.toShortString(project)
  return project.serviceAsync<CcDebugInfoPromptService>().ask { answer ->
    buildCcDebugInfoRequest(targetName, result, projectViewFileName, canUseDbg, answer)
  }
}

/** Collects the information on the backend required by the frontend to show the dialog. */
@VisibleForTesting
@ApiStatus.Internal
fun buildCcDebugInfoRequest(
  targetName: @NlsSafe String,
  result: CcDebugInfoCheck.Result,
  projectViewFileName: @NlsSafe String,
  canUseDbg: Boolean,
  answer: SendChannel<CcDebugInfoChoice>,
): CcDebugInfoRequest {
  val targets = result.issues.map { it.label }.distinct()

  // a transition can set the compilation mode of a target, thus one build can mix the modes
  val withoutDbg = result.issues
    .filter { it.compilationMode == CcDebugInfo.CompilationMode.FASTBUILD || it.compilationMode == CcDebugInfo.CompilationMode.OPT }
    .map { it.label }
    .distinct()

  return CcDebugInfoRequest(
    targetName = targetName,
    targetCount = targets.size,
    targetsWithoutDbgCount = withoutDbg.size,
    details = result.issues.map { it.toString() },
    // with dbg the cause is in the flags, thus the details are the only hint
    expandDetails = !result.isFixableByCompilationMode,
    projectViewFileName = projectViewFileName,
    canUseDbg = canUseDbg,
    answer = answer,
  )
}
