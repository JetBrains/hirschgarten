package org.jetbrains.bazel.config

import com.intellij.DynamicBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
const val BUNDLE = "messages.BazelPluginBundle"

object BazelPluginBundle : DynamicBundle(BUNDLE) {
  @JvmStatic
  fun message(
    @PropertyKey(resourceBundle = BUNDLE) key: String,
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
