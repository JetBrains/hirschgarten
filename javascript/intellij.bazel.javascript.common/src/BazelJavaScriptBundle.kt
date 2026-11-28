package org.jetbrains.bazel.javascript

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey

internal object BazelJavaScriptBundle {
  const val BUNDLE = "messages.BazelJavaScriptBundle"
  private val INSTANCE = DynamicBundle(BazelJavaScriptBundle::class.java, BUNDLE)

  @JvmStatic
  fun message(
    @PropertyKey(resourceBundle = BUNDLE)
    key: String,
    vararg params: Any?,
  ): @Nls String = INSTANCE.getMessage(key = key, params = params)
}
