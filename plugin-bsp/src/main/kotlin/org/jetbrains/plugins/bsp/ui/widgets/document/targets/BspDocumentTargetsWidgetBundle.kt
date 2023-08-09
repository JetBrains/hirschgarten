package org.jetbrains.plugins.bsp.ui.widgets.document.targets

import com.intellij.DynamicBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "messages.widgets.BspDocumentTargetsWidgetBundle"

internal object BspDocumentTargetsWidgetBundle : DynamicBundle(BUNDLE) {
  @JvmStatic
  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
    getMessage(key, *params)
}
