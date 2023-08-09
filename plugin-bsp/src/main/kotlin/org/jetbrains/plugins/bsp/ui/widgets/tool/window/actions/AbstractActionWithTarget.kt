package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import com.intellij.openapi.actionSystem.AnAction
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import java.lang.ref.WeakReference

/**
 * The purpose of this class is to be able to pass {@link BuildTargetIdentifier} to {@link AnAction} instance without
 * leaking the data. Actions exist for the entire lifetime of the application so leaving here an actual reference would
 * create a leak.
 */
public abstract class AbstractActionWithTarget(text: String) : AnAction(text) {
  private var _target: WeakReference<BuildTargetId?> = WeakReference(null)

  public var target: BuildTargetId?
    get() = _target.get()
    set(value) {
      _target = WeakReference(value)
    }
}
