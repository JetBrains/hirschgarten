package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.util.ui.TextTransferable
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.BuildTargetContainer
import javax.swing.JComponent

public class CopyTargetIdAction(
  private val container: BuildTargetContainer,
  component: JComponent,
) : AnAction({ BspPluginBundle.message("widget.copy.target.id") }, AllIcons.Actions.Copy) {
  init {
    registerCustomShortcutSet(CommonShortcuts.getCopy(), component)
  }

  override fun actionPerformed(e: AnActionEvent) {
    container.getSelectedNode()?.idToCopy()?.copyToClipboard()
  }

  private fun String.copyToClipboard() {
    val clipboard = CopyPasteManager.getInstance()
    val transferable = TextTransferable(this as CharSequence)
    clipboard.setContents(transferable)
  }
}
