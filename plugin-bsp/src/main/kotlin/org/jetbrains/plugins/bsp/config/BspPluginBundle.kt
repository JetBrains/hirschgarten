package org.jetbrains.plugins.bsp.config

import com.intellij.DynamicBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "messages.BspPluginBundle"

internal object BspPluginBundle : DynamicBundle(BUNDLE) {
  @JvmStatic
  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
    getMessage(key, *params)

  @JvmStatic
  fun messageVerbose(
    @PropertyKey(resourceBundle = BUNDLE) key: String,
    isVerbose: Boolean = false,
    vararg params: Any,
  ): String {
    val updatedKey = if (isVerbose && !key.endsWith(".verbose")) "$key.verbose" else key
    return getMessage(updatedKey, *params)
  }
}
