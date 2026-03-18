package org.jetbrains.bazel.assets

import com.intellij.openapi.util.IconLoader
import org.jetbrains.annotations.ApiStatus
import javax.swing.Icon

// TODO: this should be generated automatically
@Suppress("unused") // should be able to provide us with all available icons
@ApiStatus.Internal
object BazelPluginIcons {
  private fun loadIcon(path: String): Icon = IconLoader.getIcon(path, BazelPluginIcons::class.java.classLoader)

  @JvmField
  val bazel: Icon = loadIcon("icons/bazel.svg")

  @JvmField
  val bazelConfig: Icon = loadIcon("icons/bazelConfig.svg")

  @JvmField
  val bazelDirectory: Icon = loadIcon("icons/bazelDirectory.svg")
  val bazelError: Icon = loadIcon("icons/bazelError.svg")

  @JvmField
  val bazelReload: Icon = loadIcon("icons/bazelReload.svg")

  @JvmField
  val bazelToolWindow: Icon = loadIcon("icons/toolWindowBazel.svg")

  @JvmField
  val bazelWarning: Icon = loadIcon("icons/bazelWarning.svg")
}
