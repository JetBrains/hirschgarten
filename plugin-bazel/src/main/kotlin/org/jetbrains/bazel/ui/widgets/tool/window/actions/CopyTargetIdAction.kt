package org.jetbrains.bazel.ui.widgets.tool.window.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.util.ui.TextTransferable
import org.jetbrains.bazel.config.BspPluginBundle
import org.jetbrains.bazel.ui.widgets.tool.window.components.BuildTargetContainer
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo
import javax.swing.JComponent

sealed class CopyTargetIdAction : AnAction({ BspPluginBundle.message("widget.copy.target.id") }, AllIcons.Actions.Copy) {
  override fun actionPerformed(e: AnActionEvent) {
    getTargetInfo()?.copyIdToClipboard()
  }

  protected abstract fun getTargetInfo(): BuildTargetInfo?

  private fun BuildTargetInfo.copyIdToClipboard() {
    val clipboard = CopyPasteManager.getInstance()
    val transferable = TextTransferable(this.id.toShortString() as CharSequence)
    clipboard.setContents(transferable)
  }

  class FromContainer(private val container: BuildTargetContainer, component: JComponent) : CopyTargetIdAction() {
    init {
      registerCustomShortcutSet(CommonShortcuts.getCopy(), component)
    }

    override fun getTargetInfo(): BuildTargetInfo? = container.getSelectedBuildTarget()
  }

  class FromTargetInfo(private val targetInfo: BuildTargetInfo) : CopyTargetIdAction() {
    override fun getTargetInfo(): BuildTargetInfo = targetInfo
  }
}
