package org.jetbrains.bazel.kotlin.sync

import com.intellij.openapi.application.writeAction
import org.jetbrains.bazel.sdkcompat.publishGlobalModuleStateModificationEventCompat
import org.jetbrains.bazel.sync.ProjectPostSyncHook

/**
 * Workaround for https://youtrack.jetbrains.com/issue/KT-70632
 */
class RefreshKotlinHighlightingPostSyncHook : ProjectPostSyncHook {
  override suspend fun onPostSync(environment: ProjectPostSyncHook.ProjectPostSyncHookEnvironment) {
    writeAction {
      environment.project.publishGlobalModuleStateModificationEventCompat()
    }
  }
}
