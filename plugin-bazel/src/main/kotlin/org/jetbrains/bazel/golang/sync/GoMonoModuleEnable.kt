package org.jetbrains.bazel.golang.sync

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.sync.projectStructure.legacy.MonoModuleProjectSyncHook
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bsp.protocol.GoBuildTarget

class GoMonoModuleEnable : MonoModuleProjectSyncHook.EnableMonoModuleSyncHookExtension {
  override fun isEnabled(project: Project): Boolean {
    if (!project.isBazelProject) return false
    return BazelFeatureFlags.isGoSupportEnabled && project.targetUtils.allBuildTargets().any { it.data is GoBuildTarget }
  }
}
