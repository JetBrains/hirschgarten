package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import ch.epfl.scala.bsp4j.BuildTarget
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.util.ui.TextTransferable
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetBundle
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.BuildTargetContainer
import javax.swing.JComponent

public class CopyTargetIdAction(
  private val container: BuildTargetContainer,
  component: JComponent,
) : AnAction({ BspAllTargetsWidgetBundle.message("widget.copy.target.id") }, AllIcons.Actions.Copy) {
  init {
    registerCustomShortcutSet(CommonShortcuts.getCopy(), component)
  }

  override fun actionPerformed(e: AnActionEvent) {
    container.getSelectedBuildTarget()?.copyIdToClipboard()
  }

  private fun BuildTarget.copyIdToClipboard() {
    val clipboard = CopyPasteManager.getInstance()
    val transferable = TextTransferable(this.id.uri as CharSequence)
    clipboard.setContents(transferable)
  }
}
