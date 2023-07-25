package org.jetbrains.plugins.bsp.config

import com.intellij.openapi.util.registry.Registry

private const val PYTHON_SUPPORT = "bsp.python.support"

public object BspFeatureFlags {

  public val isPythonSupportEnabled: Boolean
    get() = Registry.`is`(PYTHON_SUPPORT)
}
