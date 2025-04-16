package org.jetbrains.bazel.kotlin.sync

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync.ProjectPostSyncHook
import org.jetbrains.kotlin.analysis.api.platform.analysisMessageBus
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationTopics

/**
 * Workaround for https://youtrack.jetbrains.com/issue/KT-70632
 */
class RefreshKotlinHighlightingPostSyncHook : ProjectPostSyncHook {
  override suspend fun onPostSync(environment: ProjectPostSyncHook.ProjectPostSyncHookEnvironment) {
    writeAction {
      try {
        environment.project.analysisMessageBus
          .syncPublisher(KotlinModificationTopics.GLOBAL_MODULE_STATE_MODIFICATION)
          .onModification()
      } catch (_: NoClassDefFoundError) {
        // TODO: the above method was removed in 252 master.
        //  Replace with `project.publishGlobalModuleStateModificationEvent()` once it's available in the next 252 EAP
        val utilsKt = Class.forName("org.jetbrains.kotlin.analysis.api.platform.modification.UtilsKt")
        val method = utilsKt.getDeclaredMethod("publishGlobalModuleStateModificationEvent", Project::class.java)
        method.invoke(utilsKt, environment.project)
      }
    }
  }
}
