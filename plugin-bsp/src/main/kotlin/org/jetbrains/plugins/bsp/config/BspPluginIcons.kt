package org.jetbrains.plugins.bsp.config

import com.intellij.openapi.util.IconLoader

internal object BspPluginIcons {
  val bsp = IconLoader.getIcon("/icons/bsp.svg", BspPluginIcons::class.java)

  val reload = IconLoader.getIcon("/icons/reload.svg", BspPluginIcons::class.java)
  val disconnect = IconLoader.getIcon("/icons/disconnect.svg", BspPluginIcons::class.java)
}
