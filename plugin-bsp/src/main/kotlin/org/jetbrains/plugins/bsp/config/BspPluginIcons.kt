package org.jetbrains.plugins.bsp.config

import com.intellij.openapi.util.IconLoader

internal object BspPluginIcons {
  val bsp = IconLoader.getIcon("/icons/bsp.svg", BspPluginIcons::class.java)
  val notLoadedTarget = IconLoader.getIcon("/icons/notLoaded.svg", BspPluginIcons::class.java)
  val loadedTarget = IconLoader.getIcon("/icons/loaded.svg", BspPluginIcons::class.java)
  val reload = IconLoader.getIcon("/icons/reload.svg", BspPluginIcons::class.java)
  val restart = IconLoader.getIcon("/icons/restart.svg", BspPluginIcons::class.java)
  val disconnect = IconLoader.getIcon("/icons/disconnect.svg", BspPluginIcons::class.java)
  val bazel = IconLoader.getIcon("/icons/bazel.svg", BspPluginIcons::class.java)
}
