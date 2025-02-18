package org.jetbrains.bazel.ui.widgets

import com.intellij.openapi.application.EDT
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.ui.list.buildTargetPopup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.label.Label
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun List<Label>.chooseTarget(editor: Editor?): Label? {
  if (editor == null) return firstOrNull() // Can't show the popup without a parent component
  if (isEmpty()) return null
  if (size == 1) return first()

  return withContext(Dispatchers.EDT) {
    suspendCoroutine { continuation ->
      lateinit var popup: JBPopup
      var continuationResumed = false

      buildTargetPopup(
        items = this@chooseTarget,
        presentationProvider = { target: Label ->
          TargetPresentation.builder(target.toString()).icon(BazelPluginIcons.bazel).presentation()
        },
        processor = { targetId ->
          if (!continuationResumed) {
            continuationResumed = true
            continuation.resume(targetId)
          }
          popup.closeOk(null)
        },
      ).setTitle(BazelPluginBundle.message("widget.target.choose"))
        .setCancelCallback {
          if (!continuationResumed) {
            continuationResumed = true
            continuation.resume(null)
          }
          true
        }
        // Don't close upon selection, because otherwise the cancel callback is called before the item selection callback
        .setCloseOnEnter(false)
        .setCancelOnClickOutside(true)
        .createPopup()
        .also { popup = it }
        .showInBestPositionFor(editor)
    }
  }
}
