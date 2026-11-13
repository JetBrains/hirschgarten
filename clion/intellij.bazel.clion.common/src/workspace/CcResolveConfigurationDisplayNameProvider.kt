package org.jetbrains.bazel.clion.workspace

import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import com.jetbrains.cidr.lang.workspace.OCResolveConfigurationDisplayNameProvider
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService

internal class CcResolveConfigurationDisplayNameProvider : OCResolveConfigurationDisplayNameProvider {
  override fun getDisplayName(configuration: OCResolveConfiguration, sourceFile: VirtualFile): String? {
    val identifier = configuration.getCcIdentifier() ?: return null
    val path = sourceFile.toNioPathOrNull() ?: return null
    val snapshot = configuration.project.service<WorkspaceSnapshotService>().snapshot.value

    return snapshot.fileToTarget.getTargetsByFile(path)
      .filter { it.configuration == identifier.configurationId }
      .map { it.displayName(snapshot.configurations) }
      .distinct()
      .singleOrNull()
  }
}
