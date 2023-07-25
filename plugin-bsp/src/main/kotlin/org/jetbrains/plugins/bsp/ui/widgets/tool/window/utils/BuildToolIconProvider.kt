package org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils

import javax.swing.Icon

public interface BuildToolIconProvider {
  /**
   * @return name of the tool corresponding to this classifier
   */
  public fun name(): String

  /**
   * @return icon the tool window should use
   */
  public fun icon(): Icon
}
