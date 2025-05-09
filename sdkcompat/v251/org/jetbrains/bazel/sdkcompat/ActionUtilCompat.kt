package org.jetbrains.bazel.sdkcompat

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ActionUtil

object ActionUtilCompat {
  fun performAction(action: AnAction, e: AnActionEvent) {
    ActionUtil.performActionDumbAwareWithCallbacks(action, e)
  }
}
