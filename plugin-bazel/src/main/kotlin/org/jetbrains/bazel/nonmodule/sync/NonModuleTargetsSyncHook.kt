package org.jetbrains.bazel.nonmodule.sync

import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.task.query
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.workspacemodel.entities.toBuildTargetInfo

class NonModuleTargetsSyncHook : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHook.ProjectSyncHookEnvironment) {
    val nonModuleTargetsResult =
      query("workspace/nonModuleTargets") {
        environment.server.workspaceNonModuleTargets()
      }

    // Filter out non-module targets which cannot be run or tested, as they are just cluttering the ui
    val usefulNonModuleTargets = nonModuleTargetsResult.nonModuleTargets.filter { it.capabilities.canRun || it.capabilities.canTest }
    environment.project.targetUtils.addTargets(usefulNonModuleTargets.map { it.toBuildTargetInfo() })
  }
}
