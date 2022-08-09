package org.jetbrains.plugins.bsp.config

import com.intellij.openapi.util.IconLoader

internal object BspPluginIcons {

  val bsp = IconLoader.getIcon("/icons/bsp.svg", BspPluginIcons::class.java)
  val notLoadedTarget = IconLoader.getIcon("/icons/notLoaded.svg", BspPluginIcons::class.java)
  val loadedTarget = IconLoader.getIcon("/icons/loaded.svg", BspPluginIcons::class.java)
}
