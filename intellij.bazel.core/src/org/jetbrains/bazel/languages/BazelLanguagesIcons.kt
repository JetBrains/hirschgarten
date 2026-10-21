package org.jetbrains.bazel.languages

import com.intellij.openapi.util.IconLoader
import org.jetbrains.annotations.ApiStatus
import javax.swing.Icon

/** Icons of the Bazel languages. The icon files live in `intellij.bazel.resource`. */
@ApiStatus.Internal
object BazelLanguagesIcons {
  @JvmField
  val bazel: Icon = IconLoader.getIcon("icons/bazel.svg", BazelLanguagesIcons::class.java.classLoader)
}
