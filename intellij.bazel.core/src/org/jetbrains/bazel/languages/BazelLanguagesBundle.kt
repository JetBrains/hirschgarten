package org.jetbrains.bazel.languages

import com.intellij.DynamicBundle
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "messages.BazelLanguagesBundle"

@ApiStatus.Internal
object BazelLanguagesBundle : DynamicBundle(BUNDLE) {
  @Nls
  @JvmStatic
  fun message(
    @NonNls @PropertyKey(resourceBundle = BUNDLE) key: String,
    vararg params: Any,
  ): String = getMessage(key, *params)
}
