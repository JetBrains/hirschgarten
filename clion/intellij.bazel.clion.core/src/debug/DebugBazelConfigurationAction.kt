package org.jetbrains.bazel.clion.debug

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfiguration

/** Reports every Bazel configuration that the workspace snapshot holds. */
internal class DebugBazelConfigurationAction : BazelDebugAction() {

  override suspend fun exec(project: Project): Any {
    val snapshot = snapshotOrFail(project)
    if (snapshot.configurations.isEmpty()) {
      fail("no configuration found in the workspace snapshot")
    }

    return snapshot.configurations.entries
      .sortedBy { it.key.shortChecksum.orEmpty() }
      .map { (_, configuration) -> configuration.toJsonMap() }
  }
}

private fun WorkspaceConfiguration.toJsonMap(): Map<String, Any?> =
  linkedMapOf(
    "id" to id,
    "hash" to summary.hash,
    "mnemonic" to summary.mnemonic,
    "platform_name" to summary.platformName,
    "cpu" to summary.cpu,
    "is_tool" to summary.isTool,
  )
