package org.jetbrains.bazel.languages.projectview.quickfix

import com.intellij.analysis.AnalysisScope
import com.intellij.codeInsight.daemon.impl.HighlightVisitorBasedInspection
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ex.GlobalInspectionToolWrapper
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.CommandProcessor
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

/**
 * Runs Inspect Code on the file of the fixture and applies the fix with [name] to the problem at the caret.
 * Inspect Code runs the annotators through [HighlightVisitorBasedInspection], so this is the path of the Inspect Code view.
 */
private fun CodeInsightTestFixture.applyInspectCodeFix(name: String) {
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
    val fix = checkNotNull(descriptor.fixes?.singleOrNull { it.name == name }) {
      "Inspect Code has no fix '$name'. Fixes: ${descriptor.fixes?.map { it.name }}"
    }
    CommandProcessor.getInstance().executeCommand(project, {
      when {
        fix.startInWriteAction() -> WriteAction.run<Throwable> { fix.applyFix(project, descriptor) }
        else -> fix.applyFix(project, descriptor)
      }
    }, name, null)
  }
  finally {
    context.cleanup()
  }
}
