package org.jetbrains.bsp.protocol.jpsCompilation.utils

import com.intellij.openapi.util.registry.Registry

private const val COMPILE_PROJECT_WITH_JPS = "bsp.jps.project.compilation"
private const val ENABLE_JPS_COMPILATION = "bsp.jps.enable"

public object JpsFeatureFlags {
  public val isCompileProjectWithJpsEnabled: Boolean
    get() = Registry.`is`(COMPILE_PROJECT_WITH_JPS)

  public val isJpsCompilationEnabled: Boolean
    get() = Registry.`is`(ENABLE_JPS_COMPILATION)
}
