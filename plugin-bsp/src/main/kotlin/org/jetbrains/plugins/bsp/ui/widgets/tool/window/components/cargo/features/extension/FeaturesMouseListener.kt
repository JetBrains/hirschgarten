package org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.cargo.features.extension

import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.codeInsight.hints.presentation.mouseButton
import java.awt.event.MouseEvent
import java.awt.event.MouseListener

public class FeaturesMouseListener(
  private val container: FeaturesContainer,
) : MouseListener {
  override fun mouseClicked(e: MouseEvent?) {
    if (e?.mouseButton == MouseButton.Left) {
      container.toggleFeatureIfSelected()
    }
  }

  override fun mousePressed(e: MouseEvent?) { /* nothing to do */ }

  override fun mouseReleased(e: MouseEvent?) { /* nothing to do */ }

  override fun mouseEntered(e: MouseEvent?) { /* nothing to do */ }

  override fun mouseExited(e: MouseEvent?) { /* nothing to do */ }
}
