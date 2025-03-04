package org.jetbrains.bazel.run.coverage.ideStarter

import com.intellij.coverage.CoverageDataManager
import com.intellij.openapi.application.readAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import com.intellij.psi.PsiDocumentManager

class AssertCoverageCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  companion object {
    const val PREFIX = CMD_PREFIX + "assertCoverage"
  }

  override suspend fun doExecute(context: PlaybackContext) {
    val project = context.project
    val editor = readAction { checkNotNull(FileEditorManager.getInstance(project).selectedTextEditor) }
    val psiFile = readAction { checkNotNull(PsiDocumentManager.getInstance(project).getPsiFile(editor.document)) }
    val coverageDataManager = CoverageDataManager.getInstance(project)
    val suite = checkNotNull(coverageDataManager.currentSuitesBundle) { "No coverage suite selected" }
    val annotator = suite.getAnnotator(project)

    val expectedCoverageString = extractCommandArgument(PREFIX)
    val actualCoverageString = annotator.getFileCoverageInformationString(psiFile, suite, coverageDataManager)
    check(expectedCoverageString == actualCoverageString) {
      "Expected coverage string: '$expectedCoverageString', actual: '$actualCoverageString'"
    }
  }
}
