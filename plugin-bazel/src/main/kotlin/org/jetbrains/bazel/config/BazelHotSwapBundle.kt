package org.jetbrains.bazel.config

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
const val HOTSWAP_BUNDLE = "messages.BazelHotSwapBundle"

object BazelHotSwapBundle : DynamicBundle(HOTSWAP_BUNDLE) {
  @Nls
  @JvmStatic
  fun message(
    @NonNls @PropertyKey(resourceBundle = HOTSWAP_BUNDLE) key: String,
    vararg params: Any,
  ): String = getMessage(key, *params)
}
