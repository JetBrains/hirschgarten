package org.jetbrains.bazel.languages.starlark

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
const val BUNDLE = "messages.StarlarkBundle"

object StarlarkBundle : DynamicBundle(BUNDLE) {
  @Nls
  @JvmStatic
  fun message(@NonNls @PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String =
    getMessage(key, *params)
}
