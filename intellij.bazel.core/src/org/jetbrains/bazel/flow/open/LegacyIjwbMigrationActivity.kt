package org.jetbrains.bazel.flow.open

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.jetbrains.bazel.flow.open.BazelUnlinkedProjectAware.Companion.closeAndReopenAsBazelProject
import java.nio.file.Path

internal class LegacyIjwbMigrationActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    if (!isLegacyIjwbProjectWithoutLegacyPlugin(project)) return
    val ijwbPath = Path.of(project.basePath ?: return)
    val workspaceRoot = findProjectFolderFromFile(ijwbPath) ?: return
    val workspaceFile = workspaceRoot.workspaceFile ?: return
    val service = serviceAsync<BazelApplicationCoroutineScopeService>()
    service.launch { closeAndReopenAsBazelProject(project, workspaceFile) }
  }


  private fun isLegacyIjwbProjectWithoutLegacyPlugin(project: Project): Boolean {
    val externalProjectPath = project.basePath.orEmpty()
    if (!externalProjectPath.endsWith(".ijwb")) return false
    if (BazelUnlinkedProjectAware().isLinkedProject(project, externalProjectPath)) return false
    val isGoogleBazelPluginEnabled = PluginManagerCore.getPluginSet().isPluginEnabled(PluginId.getId("com.google.idea.bazel.ijwb"))
    return !isGoogleBazelPluginEnabled
  }

}
