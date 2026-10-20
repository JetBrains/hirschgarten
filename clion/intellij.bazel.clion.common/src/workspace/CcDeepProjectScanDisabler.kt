package org.jetbrains.bazel.clion.workspace

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.jetbrains.cidr.lang.workspace.OCDeepProjectScanDisabler
import org.jetbrains.bazel.config.isBazelProject

internal class CcDeepProjectScanDisabler : OCDeepProjectScanDisabler {
  override fun isDeepProjectScanDisabled(project: Project): Boolean {
    return project.isBazelProject && !Registry.`is`("bazel.clion.deep.project.scan")
  }
}
