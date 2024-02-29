package org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils

import com.intellij.ui.components.fields.ExtendableTextComponent
import java.awt.Component
import javax.swing.Icon

public sealed class TextComponentExtension(
  private val beforeText: Boolean,
) : ExtendableTextComponent.Extension {
  override fun isIconBeforeText(): Boolean = beforeText

  public class Indicator(
    private val trueIcon: Icon,
    private val falseIcon: Icon,
    private val predicate: () -> Boolean,
    beforeText: Boolean = false,
  ) : TextComponentExtension(beforeText) {
    override fun getIcon(hovered: Boolean): Icon =
      if (predicate()) trueIcon else falseIcon

    override fun getActionOnClick(): Runnable? = null

    override fun isSelected(): Boolean = false
  }

  public class Switch(
    private val icon: Icon,
    private val valueGetter: () -> Boolean,
    private val valueSetter: (Boolean) -> Unit,
    private val parentComponent: Component,
    beforeText: Boolean = false,
  ) : TextComponentExtension(beforeText) {
    override fun getIcon(hovered: Boolean): Icon = icon

    override fun getActionOnClick(): Runnable =
      Runnable {
        valueSetter(!valueGetter())
        parentComponent.repaint() // button selection will not update otherwise
      }

    override fun isSelected(): Boolean = valueGetter()
  }
}