package org.jetbrains.bazel.languages.projectview.quickfix

import com.intellij.analysis.AnalysisScope
import com.intellij.codeInsight.daemon.impl.HighlightVisitorBasedInspection
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.SuppressIntentionAction
import com.intellij.codeInspection.SuppressIntentionActionFromFix
import com.intellij.codeInspection.ex.GlobalInspectionToolWrapper
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.psi.PsiElement
import com.intellij.testFramework.InspectionTestUtil
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.createGlobalContextForTool
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

/**
 * Applies the quick fix with [name] at the caret of [before] in the editor and in Inspect Code. Both results must be [after].
 */
internal fun CodeInsightTestFixture.checkQuickFix(
  name: String,
  before: String,
  after: String,
  inputEndsWithNewLine: Boolean = true,
) {
  val input = before.trimIndent() + if (inputEndsWithNewLine) "\n" else ""
  val expected = after.trimIndent() + "\n"
  timeoutRunBlocking(30.seconds) {
    withContext(Dispatchers.EDT) {
      configureByText(".bazelproject", input)
      launchAction(findSingleIntention(name))
      checkResult(expected)

      configureByText(".bazelproject", input)
      applyInspectCodeFix(name)
      checkResult(expected)
    }
  }
}

private fun CodeInsightTestFixture.applyInspectCodeFix(name: String) = applyInInspectCode { descriptor ->
  checkNotNull(descriptor.fixes?.filterIsInstance<LocalQuickFix>()?.singleOrNull { it.name == name }) {
    "Inspect Code has no fix '$name'. Fixes: ${descriptor.fixes?.map { it.name }}"
  }
}

/**
 * Runs Inspect Code on the file of the fixture and applies the fix that [selectFix] gives for the problem at the caret, if any.
 */
internal fun CodeInsightTestFixture.applyInInspectCode(selectFix: (ProblemDescriptor) -> LocalQuickFix?) = withInspectCodeProblemAtCaret { descriptor ->
  val fix = selectFix(descriptor) ?: return@withInspectCodeProblemAtCaret
  inCommand(fix.name, fix.startInWriteAction()) { fix.applyFix(project, descriptor) }
}

/**
 * The suppress actions that the Inspect Code view offers for the problem at the caret.
 */
internal fun CodeInsightTestFixture.inspectCodeSuppressActionsAtCaret(): List<SuppressIntentionAction> {
  var actions = emptyList<SuppressIntentionAction>()
  withInspectCodeProblemAtCaret { descriptor -> actions = inspectCodeSuppressActions(checkNotNull(descriptor.psiElement)) }
  return actions
}

/**
 * Runs the suppress action with [text] of the Inspect Code view for the problem at the caret.
 */
internal fun CodeInsightTestFixture.suppressInInspectCode(text: String) = withInspectCodeProblemAtCaret { descriptor ->
  val element = checkNotNull(descriptor.psiElement)
  val actions = inspectCodeSuppressActions(element)
  val action = checkNotNull(actions.singleOrNull { it.text == text }) { "Inspect Code has no suppress action '$text'. Actions: ${actions.map { it.text }}" }
  inCommand(text, action.startInWriteAction()) { action.invoke(project, null, element) }
}

/**
 * Like the Inspect Code view, converts the batch suppress actions of the annotator tool and gives them only the element of the problem.
 */
private fun CodeInsightTestFixture.inspectCodeSuppressActions(element: PsiElement): List<SuppressIntentionAction> = HighlightVisitorBasedInspection()
  .getBatchSuppressActions(element)
  .map(SuppressIntentionActionFromFix::convertBatchToSuppressIntentionAction)
  .filter { it.isAvailable(project, null, element) }

/**
 * Runs Inspect Code on the file of the fixture and gives [action] the problem at the caret.
 * Inspect Code runs the annotators through [HighlightVisitorBasedInspection], so this is the path of the Inspect Code view.
 */
private fun CodeInsightTestFixture.withInspectCodeProblemAtCaret(action: (ProblemDescriptor) -> Unit) {
  val toolWrapper = GlobalInspectionToolWrapper(HighlightVisitorBasedInspection())
  val scope = AnalysisScope(file)
  val context = createGlobalContextForTool(scope, project, listOf(toolWrapper))
  try {
    InspectionTestUtil.runTool(toolWrapper, scope, context)
    val caret = caretOffset
    val descriptor = context
      .getPresentation(toolWrapper)
      .problemDescriptors
      .filterIsInstance<ProblemDescriptor>()
      .single { it.psiElement?.textRange?.containsOffset(caret) == true }
    action(descriptor)
  }
  finally {
    context.cleanup()
  }
}

private fun CodeInsightTestFixture.inCommand(name: String, inWriteAction: Boolean, action: () -> Unit) {
  CommandProcessor.getInstance().executeCommand(project, {
    when {
      inWriteAction -> WriteAction.run<Throwable> { action() }
      else -> action()
    }
  }, name, null)
}
