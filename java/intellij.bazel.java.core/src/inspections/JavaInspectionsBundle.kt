package org.jetbrains.bazel.inspections

import com.intellij.DynamicBundle
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE: String = "messages.JavaInspectionsBundle"

@ApiStatus.Internal
object JavaInspectionsBundle : DynamicBundle(BUNDLE) {
  @Nls fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) = getMessage(key, *params)
}
