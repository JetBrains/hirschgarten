package org.jetbrains.plugins.bsp.utils

import com.intellij.util.EnvironmentUtil

// TODO probably we should move it to another package / module
public fun ProcessBuilder.withRealEnvs(): ProcessBuilder {
  val env = environment()
  env.clear()
  env.putAll(EnvironmentUtil.getEnvironmentMap())

  return this
}
