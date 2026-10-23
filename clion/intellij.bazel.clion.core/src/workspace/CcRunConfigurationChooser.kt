package org.jetbrains.bazel.clion.workspace

import com.intellij.execution.RunManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import com.jetbrains.cidr.lang.workspace.OCResolveConfigurationChooser
import com.jetbrains.cidr.lang.workspace.OCResolveConfigurationSlice
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.repomapping.toCanonicalLabel
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey

internal class CcRunConfigurationChooser : OCResolveConfigurationChooser {
  override fun selectPreselectedResolveConfiguration(
    project: Project,
    slice: OCResolveConfigurationSlice,
    file: VirtualFile?,
  ): OCResolveConfiguration? {
    if (!project.isBazelProject) return null

    val labels = selectedLabels(project)
    if (labels.isEmpty()) return null

    val graph = project.service<WorkspaceSnapshotService>().snapshot.value.targetGraph
    return slice.resolve(project).firstOrNull { resolveConfiguration ->
      val configurationId = resolveConfiguration.getCcIdentifier()?.configurationId ?: return@firstOrNull false
      labels.any { graph.findTargetByKey(WorkspaceTargetKey(it, configurationId)) != null }
    }
  }

  private fun selectedLabels(project: Project): List<Label> {
    val configuration = RunManager.getInstance(project).selectedConfiguration?.configuration as? BazelRunConfiguration ?: return emptyList()
    return configuration.targets.mapNotNull { it.toCanonicalLabel(project) }.distinct()
  }
}
