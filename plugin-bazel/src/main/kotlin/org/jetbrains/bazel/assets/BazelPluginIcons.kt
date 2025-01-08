package org.jetbrains.bazel.assets

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

@Suppress("unused") // should be able to provide us with all available icons
object BazelPluginIcons {
  val bazel: Icon = IconLoader.getIcon("/icons/bazel.svg", javaClass)
  val bazelConfig: Icon = IconLoader.getIcon("/icons/bazelConfig.svg", javaClass)
  val bazelDirectory: Icon = IconLoader.getIcon("/icons/bazelDirectory.svg", javaClass)
  val bazelError: Icon = IconLoader.getIcon("/icons/bazelError.svg", javaClass)
  val bazelReload: Icon = IconLoader.getIcon("/icons/bazelReload.svg", javaClass)
  val bazelToolWindow: Icon = IconLoader.getIcon("/icons/toolWindowBazel.svg", javaClass)
  val bazelWarning: Icon = IconLoader.getIcon("/icons/bazelWarning.svg", javaClass)
}
