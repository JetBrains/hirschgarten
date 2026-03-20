package org.jetbrains.bazel.ui.widgets.tool.window.utils

import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bsp.protocol.BuildTarget

// TODO: remove this completely after refactoring action system
@ApiStatus.Internal
interface LoadedTargetActionsProvider {

  fun onActionsInit(
    project: Project,
    group: DefaultActionGroup,
    target: BuildTarget,
    includeTargetNameInText: Boolean,
    callerPsiElement: PsiElement?,
  )

  companion object {
    val ep: ExtensionPointName<LoadedTargetActionsProvider> = ExtensionPointName.create("org.jetbrains.bazel.loadedTargetActionsProvider")
  }
}
