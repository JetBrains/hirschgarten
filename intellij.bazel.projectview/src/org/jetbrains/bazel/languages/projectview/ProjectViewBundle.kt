package org.jetbrains.bazel.languages.projectview

import com.intellij.DynamicBundle
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey

@ApiStatus.Internal
object ProjectViewBundle {
  private const val BUNDLE = "messages.BazelProjectViewBundle"
  private val INSTANCE = DynamicBundle(ProjectViewBundle::class.java, BUNDLE)

  @JvmStatic
  fun getMessage(
    @PropertyKey(resourceBundle = BUNDLE)
    key: String,
    vararg params: Any?,
  ): @Nls String {
    return INSTANCE.getMessage(key = key, params = params)
  }
}
