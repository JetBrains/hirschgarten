package org.jetbrains.bazel.clion.workspace

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.jetbrains.cidr.lang.workspace.OCFallbackConfigDisabler
import org.jetbrains.bazel.config.isBazelProject

internal class CcFallbackConfigDisabler : OCFallbackConfigDisabler {
  override fun isFallbackConfigDisabled(project: Project): Boolean {
    return project.isBazelProject && !Registry.`is`("bazel.clion.fallback.config")
  }
}
