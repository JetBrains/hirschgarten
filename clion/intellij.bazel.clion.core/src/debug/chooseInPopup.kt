package org.jetbrains.bazel.clion.debug

import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.util.NlsSafe
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.Nls

/**
 * Shows a list popup with speed search and waits for the choice.
 *
 * The caller is canceled when the user closes the popup without a choice.
 */
internal suspend fun <T> chooseInPopup(
  project: Project,
  title: @Nls String,
  candidates: List<T>,
  textFor: (T) -> @NlsSafe String,
): T = withContext(Dispatchers.EDT) {
  suspendCancellableCoroutine { continuation ->
    JBPopupFactory.getInstance()
      .createListPopup(ChoicePopupStep(continuation, title, candidates, textFor))
      .showCenteredInCurrentWindow(project)
  }
}

private class ChoicePopupStep<T>(
  private val continuation: CancellableContinuation<T>,
  title: @Nls String,
  candidates: List<T>,
  private val textFor: (T) -> @NlsSafe String,
) : BaseListPopupStep<T>(title, candidates) {

  override fun getTextFor(value: T): String = textFor(value)

  override fun onChosen(selectedValue: T, finalChoice: Boolean): PopupStep<*>? {
    continuation.resumeWith(Result.success(selectedValue))
    return super.onChosen(selectedValue, true)
  }

  override fun isSpeedSearchEnabled(): Boolean = true

  override fun canceled() {
    continuation.cancel()
    super.canceled()
  }
}
