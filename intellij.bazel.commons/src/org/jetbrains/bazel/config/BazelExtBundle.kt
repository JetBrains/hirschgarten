package org.jetbrains.bazel.config

import com.intellij.DynamicBundle
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey

// TODO: move to BazelCommonsBundle
@ApiStatus.Internal
object BazelExtBundle {
  const val BUNDLE = "messages.BazelExtBundle"
  private val INSTANCE = DynamicBundle(BazelExtBundle::class.java, BUNDLE)

  @JvmStatic
  fun message(
    @PropertyKey(resourceBundle = BUNDLE)
    key: String,
    vararg params: Any?,
  ): @Nls String {
    return INSTANCE.getMessage(key = key, params = params)
  }

}
