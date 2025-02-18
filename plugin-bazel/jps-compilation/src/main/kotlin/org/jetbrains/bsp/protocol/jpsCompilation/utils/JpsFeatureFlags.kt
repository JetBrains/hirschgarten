package org.jetbrains.bsp.protocol.jpsCompilation.utils

import com.intellij.openapi.util.registry.Registry

private const val ENABLE_JPS_COMPILATION = "bsp.jps.compilation.enable"

object JpsFeatureFlags {
  val isJpsCompilationEnabled: Boolean
    get() = Registry.`is`(ENABLE_JPS_COMPILATION)
}
