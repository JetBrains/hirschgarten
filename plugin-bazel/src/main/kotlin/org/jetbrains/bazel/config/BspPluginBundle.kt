package org.jetbrains.bazel.config

import com.intellij.DynamicBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BSP_BUNDLE = "messages.BspPluginBundle"

object BspPluginBundle : DynamicBundle(BSP_BUNDLE) {
  @JvmStatic
  fun message(
    @PropertyKey(resourceBundle = BSP_BUNDLE) key: String,
    vararg params: Any,
    trimMultipleSpaces: Boolean = true,
  ): String {
    val originalMessage = getMessage(key, *params)
    return if (trimMultipleSpaces) {
      originalMessage.replace("\\s+".toRegex(), " ")
    } else {
      originalMessage
    }
  }
}
