package org.jetbrains.bazel.clion.workspace

import com.intellij.execution.configurations.GeneralCommandLine
import com.jetbrains.cidr.lang.toolchains.CidrToolEnvironment

internal class CcToolEnvironment(private val environment: Map<String, String>) : CidrToolEnvironment() {

  override fun prepare(commandLine: GeneralCommandLine, prepareFor: PrepareFor) {
    super.prepare(commandLine, prepareFor)
    commandLine.environment.putAll(environment)
  }
}

/** Caches one environment per toolchain, because many resolve configurations share a toolchain. */
internal class CcToolEnvironments {

  private val cache = mutableMapOf<Map<String, String>, CidrToolEnvironment>()

  fun of(environment: Map<String, String>): CidrToolEnvironment =
    cache.getOrPut(environment) { if (environment.isEmpty()) CidrToolEnvironment() else CcToolEnvironment(environment) }
}
