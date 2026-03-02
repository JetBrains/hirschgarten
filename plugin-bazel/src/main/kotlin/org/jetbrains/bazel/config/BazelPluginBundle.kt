package org.jetbrains.bazel.config

import com.intellij.DynamicBundle
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE: String = "messages.BazelPluginBundle"

@ApiStatus.Internal
object BazelPluginBundle : DynamicBundle(BUNDLE) {
  @JvmStatic
  @Nls
  fun message(
    @PropertyKey(resourceBundle = BUNDLE) key: String,
    vararg params: Any,
    trimMultipleSpaces: Boolean = true,
  ): String {
    val originalMessage = getMessage(key, *params)
    return if (trimMultipleSpaces) {
      @Suppress("HardCodedStringLiteral")
      originalMessage.replace("\\s+".toRegex(), " ")
    } else {
      originalMessage
    }
  }
}
