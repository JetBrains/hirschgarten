package org.jetbrains.bazel.golang.resolve

import com.intellij.DynamicBundle
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE_FQN: @NonNls String = "messages.BazelGoBundle"

@ApiStatus.Internal
object BazelGoBundle {
  private val BUNDLE = DynamicBundle(BazelGoBundle::class.java, BUNDLE_FQN)

  fun message(
    @NonNls @PropertyKey(resourceBundle = BUNDLE_FQN) key: String,
    vararg params: Any,
  ): @Nls String = BUNDLE.getMessage(key, *params)
}
