package org.jetbrains.plugins.bsp.config

import com.intellij.openapi.util.registry.Registry

private const val PYTHON_SUPPORT = "bsp.python.support"
private const val BUILD_PROJECT_ON_SYNC = "bsp.build.project.on.sync"

public object BspFeatureFlags {
  public val isPythonSupportEnabled: Boolean
    get() = Registry.`is`(PYTHON_SUPPORT)

  public val isBuildProjectOnSyncEnabled: Boolean
    get() = Registry.`is`(BUILD_PROJECT_ON_SYNC)
}
