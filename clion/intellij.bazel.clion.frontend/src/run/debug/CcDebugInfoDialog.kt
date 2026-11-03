package org.jetbrains.bazel.clion.frontend.run.debug

import com.intellij.openapi.application.EDT
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.OptionAction
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.rows
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.Nls
import org.jetbrains.bazel.clion.frontend.BazelCLionFrontendBundle
import org.jetbrains.bazel.clion.shared.run.debug.CcDebugInfoChoice
import org.jetbrains.bazel.clion.shared.run.debug.CcDebugInfoRequest
import javax.swing.Action
import javax.swing.JComponent

private const val USE_DBG_EXIT_CODE = DialogWrapper.NEXT_USER_EXIT_CODE
private const val DEBUG_ANYWAY_EXIT_CODE = DialogWrapper.NEXT_USER_EXIT_CODE + 1
private const val OPEN_PROJECT_VIEW_EXIT_CODE = DialogWrapper.NEXT_USER_EXIT_CODE + 2
private const val ALWAYS_DEBUG_EXIT_CODE = DialogWrapper.NEXT_USER_EXIT_CODE + 3

private const val DETAILS_ROWS = 10

// with the default word wrap, the long instruction makes the dialog narrow and tall
private const val INSTRUCTION_LINE_LENGTH = 100

/** Asks the user to configure the project for debugging. The backend found targets without debug info. */
internal suspend fun showCcDebugInfoDialog(project: Project, request: CcDebugInfoRequest): CcDebugInfoChoice {
  return withContext(Dispatchers.EDT) {
    val dialog = CcDebugInfoDialog(project, request)
    dialog.show()

    when (dialog.exitCode) {
      OPEN_PROJECT_VIEW_EXIT_CODE -> CcDebugInfoChoice.OPEN_PROJECT_VIEW
      USE_DBG_EXIT_CODE -> CcDebugInfoChoice.USE_DBG
      DEBUG_ANYWAY_EXIT_CODE -> CcDebugInfoChoice.DEBUG_ANYWAY
      ALWAYS_DEBUG_EXIT_CODE -> CcDebugInfoChoice.ALWAYS_DEBUG
      else -> CcDebugInfoChoice.CANCEL
    }
  }
}

private class CcDebugInfoDialog(project: Project, private val request: CcDebugInfoRequest) : DialogWrapper(project) {

  @Suppress("DialogTitleCapitalization") // dbg is a Bazel value
  private val useDbgAction = DialogWrapperExitAction(BazelCLionFrontendBundle.message("debug.info.dialog.use.dbg"), USE_DBG_EXIT_CODE)
  private val debugAnywayAction = DebugAnywayAction()

  init {
    title = BazelCLionFrontendBundle.message("debug.info.dialog.title")

    // the quick fix is the primary action if there is one
    (if (request.canUseDbg) useDbgAction else debugAnywayAction).putValue(DEFAULT_ACTION, true)

    init()
  }

  override fun createCenterPanel(): JComponent = panel {
    row {
      text(BazelCLionFrontendBundle.message("debug.info.dialog.header", request.targetName))
    }
    row {
      text(issuesMessage(request), INSTRUCTION_LINE_LENGTH)
    }

    row {
      text(BazelCLionFrontendBundle.message("debug.info.dialog.instruction", request.projectViewFileName), INSTRUCTION_LINE_LENGTH) {
        close(OPEN_PROJECT_VIEW_EXIT_CODE)
      }
    }.topGap(TopGap.SMALL)

    collapsibleGroup(BazelCLionFrontendBundle.message("debug.info.dialog.details")) {
      row {
        textArea()
          .rows(DETAILS_ROWS)
          .align(Align.FILL)
          .applyToComponent {
            isEditable = false
            font = EditorFontType.getGlobalPlainFont()
            text = request.details.joinToString("\n")
            caretPosition = 0
          }
      }.resizableRow()
    }.apply {
      expanded = request.expandDetails
    }
  }

  override fun createActions(): Array<Action> {
    return listOfNotNull(useDbgAction.takeIf { request.canUseDbg }, cancelAction).toTypedArray()
  }

  // to continue without a fix is the alternative to the actions on the right
  override fun createLeftSideActions(): Array<Action> {
    return arrayOf(debugAnywayAction)
  }

  /** Continues the debug session. The option also hides the dialog for the project from now on. */
  private inner class DebugAnywayAction :
    DialogWrapperExitAction(BazelCLionFrontendBundle.message("debug.info.dialog.debug.anyway"), DEBUG_ANYWAY_EXIT_CODE), OptionAction {

    override fun getOptions(): Array<Action> {
      return arrayOf(DialogWrapperExitAction(BazelCLionFrontendBundle.message("debug.info.dialog.debug.always"), ALWAYS_DEBUG_EXIT_CODE))
    }
  }
}

/** Counts the targets without debug info. Names the compilation mode as the cause if a target is known to be built without dbg. */
private fun issuesMessage(request: CcDebugInfoRequest): @Nls String {
  return when (request.targetsWithoutDbgCount) {
    0 -> BazelCLionFrontendBundle.message("debug.info.dialog.targets", request.targetCount)
    request.targetCount -> BazelCLionFrontendBundle.message("debug.info.dialog.targets.all.not.dbg", request.targetCount)
    else -> BazelCLionFrontendBundle.message(
      "debug.info.dialog.targets.some.not.dbg",
      request.targetCount,
      request.targetsWithoutDbgCount,
    )
  }
}
