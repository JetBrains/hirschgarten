package org.jetbrains.plugins.bsp.utils

import com.intellij.util.EnvironmentUtil

public fun ProcessBuilder.withRealEnvs(): ProcessBuilder {
  val env = environment()
  env.clear()
  env.putAll(EnvironmentUtil.getEnvironmentMap())

  return this
}
