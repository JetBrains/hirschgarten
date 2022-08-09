package org.jetbrains.plugins.bsp.ui.widgets.toolwindow.all.targets

import com.intellij.DynamicBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "messages.widgets.BspAllTargetsWidgetBundle"

internal object BspAllTargetsWidgetBundle : DynamicBundle(BUNDLE) {

  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
    getMessage(key, *params)
}
