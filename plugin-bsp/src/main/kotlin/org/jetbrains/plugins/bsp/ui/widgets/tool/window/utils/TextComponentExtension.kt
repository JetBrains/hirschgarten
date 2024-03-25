package org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils

import com.intellij.icons.AllIcons
import com.intellij.ui.components.fields.ExtendableTextComponent
import javax.swing.Icon
import javax.swing.JComponent

public sealed class TextComponentExtension(
  private val tooltip: String?,
  private val beforeText: Boolean,
) : ExtendableTextComponent.Extension {
  override fun isIconBeforeText(): Boolean = beforeText

  override fun getTooltip(): String? = tooltip

  /**
   * Non-clickable extension, showing one of two icons based on the given predicate.
   *
   * @property trueIcon The icon to display when the predicate evaluates to `true`
   * @property falseIcon The icon to display when the predicate evaluates to `false`
   * @property predicate The predicate function to determine the icon to display
   * @property tooltip The tooltip text to display when hovering over the indicator
   * @property beforeText `true` if the extension should appear before the text
   */
  public class Indicator(
    private val trueIcon: Icon,
    private val falseIcon: Icon,
    private val predicate: () -> Boolean,
    tooltip: String? = null,
    beforeText: Boolean = false,
  ) : TextComponentExtension(tooltip, beforeText) {
    override fun getIcon(hovered: Boolean): Icon =
      if (predicate()) trueIcon else falseIcon

    override fun getActionOnClick(): Runnable? = null

    override fun isSelected(): Boolean = false
  }

  /**
   * Clickable extension toggling a boolean variable.
   *
   * @property icon The icon to display in the switch component
   * @property valueGetter A function that retrieves the current value of the boolean
   * @property valueSetter A function that sets the value of the boolean
   * @property parentComponent The parent component of the switch (will be repainted after every value change)
   * @property tooltip The tooltip text to display when hovering over the switch
   * @property beforeText `true` if the extension should appear before the text
   */
  public class Switch(
    private val icon: Icon,
    private val valueGetter: () -> Boolean,
    private val valueSetter: (Boolean) -> Unit,
    private val parentComponent: JComponent,
    tooltip: String? = null,
    beforeText: Boolean = false,
  ) : TextComponentExtension(tooltip, beforeText) {
    override fun getIcon(hovered: Boolean): Icon = icon

    override fun getActionOnClick(): Runnable =
      Runnable {
        valueSetter(!valueGetter())
        parentComponent.repaint() // button selection will not update otherwise
      }

    override fun isSelected(): Boolean = valueGetter()
  }

  /**
   * Clickable extension clearing a text component.
   *
   * @property isEmpty A function that checks if the text component is empty
   * @property clearAction A function to be executed when the clear button is clicked
   * @property tooltip The tooltip text to display when hovering over the clear button
   */
  public class Clear(
    private val isEmpty: () -> Boolean,
    private val clearAction: () -> Unit,
    tooltip: String,
  ) : TextComponentExtension(tooltip, false) {
    override fun getIcon(hovered: Boolean): Icon? =
      when {
        isEmpty() -> null
        hovered -> AllIcons.Actions.CloseHovered
        else -> AllIcons.Actions.Close
      }

    override fun getActionOnClick(): Runnable = Runnable(clearAction)
  }
}
