package org.jetbrains.bazel.clion.run.debug

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.CcDebugInfo
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.OptionAction
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsSafe
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.rows
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.TestOnly
import org.jetbrains.bazel.clion.BazelCLionCoreBundle
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import javax.swing.Action
import javax.swing.JComponent

private const val DISMISSED_KEY = "bazel.cc.debug.info.dialog.dismissed"

private const val USE_DBG_EXIT_CODE = DialogWrapper.NEXT_USER_EXIT_CODE
private const val DEBUG_ANYWAY_EXIT_CODE = DialogWrapper.NEXT_USER_EXIT_CODE + 1
private const val OPEN_PROJECT_VIEW_EXIT_CODE = DialogWrapper.NEXT_USER_EXIT_CODE + 2
private const val ALWAYS_DEBUG_EXIT_CODE = DialogWrapper.NEXT_USER_EXIT_CODE + 3

private const val DETAILS_ROWS = 10

// with the default word wrap, the long instruction makes the dialog narrow and tall
private const val INSTRUCTION_LINE_LENGTH = 100

private val TEST_HANDLER_KEY = Key.create<CcDebugInfoDialogTestHandler>("bazel.cc.debug.info.dialog.test.handler")

@ApiStatus.Internal
enum class CcDebugInfoChoice { OPEN_PROJECT_VIEW, USE_DBG, DEBUG_ANYWAY, CANCEL }

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

/**
 * Asks the user to configure the project for debugging after the [CcDebugInfoCheck] found targets without debug info.
 *
 * @param projectViewFileName null hides the link to the project view file
 * @param canUseDbg true shows the option to set `--compilation_mode=dbg`
 */
internal suspend fun showCcDebugInfoDialog(
  project: Project,
  mainTarget: Label,
  result: CcDebugInfoCheck.Result,
  projectViewFileName: @NlsSafe String,
  canUseDbg: Boolean,
): CcDebugInfoChoice {
  project.getUserData(TEST_HANDLER_KEY)?.let { return it.choose(result, canUseDbg) }

  return withContext(Dispatchers.EDT) {
    val dialog = CcDebugInfoDialog(project, mainTarget, result, projectViewFileName, canUseDbg)
    dialog.show()

    when (dialog.exitCode) {
      OPEN_PROJECT_VIEW_EXIT_CODE -> CcDebugInfoChoice.OPEN_PROJECT_VIEW
      USE_DBG_EXIT_CODE -> CcDebugInfoChoice.USE_DBG
      DEBUG_ANYWAY_EXIT_CODE -> CcDebugInfoChoice.DEBUG_ANYWAY
      ALWAYS_DEBUG_EXIT_CODE -> {
        PropertiesComponent.getInstance(project).setValue(DISMISSED_KEY, true)
        CcDebugInfoChoice.DEBUG_ANYWAY
      }
      else -> CcDebugInfoChoice.CANCEL
    }
  }
}

private class CcDebugInfoDialog(
  private val project: Project,
  private val mainTarget: Label,
  private val result: CcDebugInfoCheck.Result,
  private val projectViewFileName: @NlsSafe String,
  private val canUseDbg: Boolean,
) : DialogWrapper(project) {

  @Suppress("DialogTitleCapitalization") // dbg is a Bazel value
  private val useDbgAction = DialogWrapperExitAction(BazelCLionCoreBundle.message("debug.info.dialog.use.dbg"), USE_DBG_EXIT_CODE)
  private val debugAnywayAction = DebugAnywayAction()

  init {
    title = BazelCLionCoreBundle.message("debug.info.dialog.title")

    // the quick fix is the primary action if there is one
    (if (canUseDbg) useDbgAction else debugAnywayAction).putValue(DEFAULT_ACTION, true)

    init()
  }

  override fun createCenterPanel(): JComponent = panel {
    row {
      text(BazelCLionCoreBundle.message("debug.info.dialog.header", mainTarget.toShortString(project)))
    }
    row {
      text(issuesMessage(result), INSTRUCTION_LINE_LENGTH)
    }

    row {
      text(BazelCLionCoreBundle.message("debug.info.dialog.instruction", projectViewFileName), INSTRUCTION_LINE_LENGTH) {
        close(OPEN_PROJECT_VIEW_EXIT_CODE)
      }
    }.topGap(TopGap.SMALL)

    collapsibleGroup(BazelCLionCoreBundle.message("debug.info.dialog.details")) {
      row {
        textArea()
          .rows(DETAILS_ROWS)
          .align(Align.FILL)
          .applyToComponent {
            isEditable = false
            font = EditorFontType.getGlobalPlainFont()
            text = result.issues.joinToString("\n")
            caretPosition = 0
          }
      }.resizableRow()
    }.apply {
      // with dbg the cause is in the flags, thus the details are the only hint
      expanded = !result.isFixableByCompilationMode
    }
  }

  override fun createActions(): Array<Action> {
    return listOfNotNull(useDbgAction.takeIf { canUseDbg }, cancelAction).toTypedArray()
  }

  // to continue without a fix is the alternative to the actions on the right
  override fun createLeftSideActions(): Array<Action> {
    return arrayOf(debugAnywayAction)
  }

  /** Continues the debug session. The option also hides the dialog for the project from now on. */
  private inner class DebugAnywayAction :
    DialogWrapperExitAction(BazelCLionCoreBundle.message("debug.info.dialog.debug.anyway"), DEBUG_ANYWAY_EXIT_CODE), OptionAction {

    override fun getOptions(): Array<Action> {
      return arrayOf(DialogWrapperExitAction(BazelCLionCoreBundle.message("debug.info.dialog.debug.always"), ALWAYS_DEBUG_EXIT_CODE))
    }
  }
}

/** Counts the targets without debug info. Names the compilation mode as the cause if a target is known to be built without dbg. */
private fun issuesMessage(result: CcDebugInfoCheck.Result): @Nls String {
  val targets = result.issues.map { it.label }.distinct()

  // a transition can set the compilation mode of a target, thus one build can mix the modes
  val withoutDbg = result.issues
    .filter { it.compilationMode == CcDebugInfo.CompilationMode.FASTBUILD || it.compilationMode == CcDebugInfo.CompilationMode.OPT }
    .map { it.label }
    .distinct()

  return when (withoutDbg.size) {
    0 -> BazelCLionCoreBundle.message("debug.info.dialog.targets", targets.size)
    targets.size -> BazelCLionCoreBundle.message("debug.info.dialog.targets.all.not.dbg", targets.size)
    else -> BazelCLionCoreBundle.message("debug.info.dialog.targets.some.not.dbg", targets.size, withoutDbg.size)
  }
}
