package com.intellij.bazel.java.profiler

import com.intellij.execution.configurations.RunProfile
import com.intellij.profiler.ultimate.JavaProfilerStarterExtension
import org.jetbrains.bazel.jvm.run.JvmRunHandler
import org.jetbrains.bazel.jvm.run.JvmTestHandler
import org.jetbrains.bazel.run.config.BazelRunConfiguration

internal class BazelJavaProfilerStarterExtension : JavaProfilerStarterExtension() {
  override fun canRun(profile: RunProfile): Boolean {
    val configuration = profile as? BazelRunConfiguration ?: return false
    return (configuration.handler is JvmRunHandler || configuration.handler is JvmTestHandler) && configuration.targets.size == 1
  }
}
