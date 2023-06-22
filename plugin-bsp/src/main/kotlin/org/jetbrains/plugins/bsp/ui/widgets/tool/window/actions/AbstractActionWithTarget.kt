package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.actionSystem.AnAction
import java.lang.ref.WeakReference

/**
 * The purpose of this class is to be able to pass {@link BuildTargetIdentifier} to {@link AnAction} instance without
 * leaking the data. Actions exist for the entire lifetime of the application so leaving here an actual reference would
 * create a leak.
 */
public abstract class AbstractActionWithTarget(text: String) : AnAction(text) {

    private var _target: WeakReference<BuildTargetIdentifier?> = WeakReference(null)

    public var target: BuildTargetIdentifier?
        get() = _target.get()
        set(value) {
            _target = WeakReference(value)
        }
}
