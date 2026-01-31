package org.jetbrains.bazel.sync_new

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import org.jetbrains.bazel.sync_new.settings.BazelSyncSettings

@Service(Service.Level.PROJECT)
class SyncFlagsService private constructor(private val project: Project) {
  companion object {
    val isFeatureEnabled: Boolean
      get() = Registry.`is`("bazel.new.sync.enabled")
  }

  private val settings = BazelSyncSettings.getInstance(project)

  // TODO: move to settings UI
  val useOptimizedInverseSourceQuery: Boolean = true
  val useSkyQueryForInverseSourceQueries: Boolean = false
  val useFastSource2Label: Boolean = true
  val useFileChangeBasedInvalidation: Boolean = true
  val disallowLegacyFullTargetGraphMaterialization: Boolean = true

  val useTargetHasher: Boolean = settings.useTargetHasher
  val isEnabled: Boolean = isFeatureEnabled && settings.enableIncrementalSync
}

internal val Project.isNewSyncEnabled: Boolean
  get() = service<SyncFlagsService>().isEnabled
