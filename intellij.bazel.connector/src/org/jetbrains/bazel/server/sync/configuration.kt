package org.jetbrains.bazel.server.sync

import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.server.bep.BepOutput
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId

internal fun patchConfigurationId(
  bepOutput: BepOutput,
  configurationId: WorkspaceConfigurationId,
): WorkspaceConfigurationId {
  if (configurationId == WorkspaceConfigurationId.EMPTY) {
    return WorkspaceConfigurationId.EMPTY
  }
  if (!bepOutput.buildToolVersion.isConfigurationSupportEnabled) {
    return WorkspaceConfigurationId.EMPTY
  }
  val configurationChecksum = configurationId.configurationChecksum ?: return configurationId
  val foundConfiguration = bepOutput.findConfigurationByChecksum(configurationChecksum)
                           ?: return WorkspaceConfigurationId.EMPTY
  return foundConfiguration.id
}

internal val BazelRelease.isConfigurationSupportEnabled: Boolean
  get() {
    // this one is dictated by whenever bazel support `short_id` on `BuildConfigurationApi`
    val (major, minor) = this
    return when {
      major >= 9 -> true
      major == 8 && minor >= 5 -> true
      major == 7 && minor >= 7 -> true
      else -> false
    }
  }
