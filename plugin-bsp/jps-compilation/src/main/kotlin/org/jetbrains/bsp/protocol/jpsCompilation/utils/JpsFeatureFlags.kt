package org.jetbrains.bsp.protocol.jpsCompilation.utils

import com.intellij.openapi.util.registry.Registry

private const val DEFAULT_COMPILE_PROJECT_WITH_JPS = "bsp.jps.compilation.default"
private const val ENABLE_JPS_COMPILATION = "bsp.jps.compilation.enable"

public object JpsFeatureFlags {
  public val isJpsCompilationAsDefaultEnabled: Boolean
    get() = Registry.`is`(DEFAULT_COMPILE_PROJECT_WITH_JPS)

  public val isJpsCompilationEnabled: Boolean
    get() = Registry.`is`(ENABLE_JPS_COMPILATION)
}
