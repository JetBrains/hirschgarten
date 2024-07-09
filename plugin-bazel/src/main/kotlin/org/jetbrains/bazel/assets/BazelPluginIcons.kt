package org.jetbrains.bazel.assets

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

internal object BazelPluginIcons {
  val bazel: Icon = IconLoader.getIcon("/icons/bazel.svg", javaClass)
  val bazelReload: Icon = IconLoader.getIcon("/icons/bazelReload.svg", javaClass)
}