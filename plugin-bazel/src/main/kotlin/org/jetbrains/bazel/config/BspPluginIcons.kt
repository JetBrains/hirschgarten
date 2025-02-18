package org.jetbrains.bazel.config

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader

object BspPluginIcons {
  val bsp = IconLoader.getIcon("/icons/bsp.svg", BspPluginIcons::class.java)

  val reload = AllIcons.Actions.Refresh
  val disconnect = AllIcons.Actions.Suspend
}
