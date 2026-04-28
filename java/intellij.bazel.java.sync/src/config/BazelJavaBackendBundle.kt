package org.jetbrains.bazel.config

import com.intellij.DynamicBundle
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey

// RC: this will become backend at some point, thats why it's called backend
internal object BazelJavaBackendBundle {
  const val BUNDLE = "messages.BazelJavaBackendBundle"
  private val INSTANCE = DynamicBundle(BazelJavaBackendBundle::class.java, BUNDLE)

  @JvmStatic
  fun message(
    @PropertyKey(resourceBundle = BUNDLE)
    key: String,
    vararg params: Any?,
  ): @Nls String {
    return INSTANCE.getMessage(key = key, params = params)
  }

}
