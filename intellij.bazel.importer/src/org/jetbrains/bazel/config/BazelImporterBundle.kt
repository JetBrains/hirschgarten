package org.jetbrains.bazel.config

import com.intellij.DynamicBundle
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey

@ApiStatus.Internal
object BazelImporterBundle {
  const val BUNDLE = "messages.BazelImporterBundle"
  private val INSTANCE = DynamicBundle(BazelImporterBundle::class.java, BUNDLE)

  @JvmStatic
  fun message(
    @PropertyKey(resourceBundle = BUNDLE)
    key: String,
    vararg params: Any?,
  ): @Nls String {
    return INSTANCE.getMessage(key = key, params = params)
  }

}
