package org.jetbrains.bazel.ui.widgets.tool.window.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.util.ui.TextTransferable
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bsp.protocol.BuildTarget
import javax.swing.JComponent

sealed class CopyTargetIdAction : AnAction({ BazelPluginBundle.message("widget.copy.target.id") }, AllIcons.Actions.Copy) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    getTargetInfo()?.copyIdToClipboard(project)
  }

  protected abstract fun getTargetInfo(): BuildTarget?

  private fun BuildTarget.copyIdToClipboard(project: Project) {
    val clipboard = CopyPasteManager.getInstance()
    val transferable = TextTransferable(this.id.toShortString(project) as CharSequence)
    clipboard.setContents(transferable)
  }

  abstract class FromContainer(component: JComponent) : CopyTargetIdAction() {
    init {
      registerCustomShortcutSet(CommonShortcuts.getCopy(), component)
    }
  }

  class FromTargetInfo(private val targetInfo: BuildTarget) : CopyTargetIdAction() {
    override fun getTargetInfo(): BuildTarget = targetInfo
  }
}
