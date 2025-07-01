package org.jetbrains.bazel.sync.projectStructure.legacy

import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.module.ModuleTypeManager
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ExcludeUrlEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleSourceDependency
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.BazelModuleEntitySource
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.projectStructure.workspaceModel.workspaceModelDiff
import org.jetbrains.bazel.sync.task.query

class WorkspaceModuleProjectSyncHook : ProjectSyncHook {
  /**
   * this sync hook is enabled by default, but the real check whether to run this is in the [onSync] function.
   * the reason why this workaround exists is for some logic depending on [ProjectSyncHook.ProjectSyncHookEnvironment] to work.
   */
  override fun isEnabled(project: Project): Boolean = true

  override suspend fun onSync(environment: ProjectSyncHook.ProjectSyncHookEnvironment) {
    val project = environment.project
    if (!EnableWorkspaceModuleSyncHookExtension.EP.extensionList.any { it.isEnabled(environment) }) return
    val moduleEntitySource = BazelModuleEntitySource(WORKSPACE_MODULE_NAME)
    val directories = query("workspace/directories") { environment.server.workspaceDirectories() }
    val virtualFileUrlManager = project.serviceAsync<WorkspaceModel>().getVirtualFileUrlManager()
    val includedDirectories = directories.includedDirectories.map { it.uri }.map { virtualFileUrlManager.getOrCreateFromUrl(it) }
    val excludedDirectories = directories.excludedDirectories.map { it.uri }.map { virtualFileUrlManager.getOrCreateFromUrl(it) }
    val sourceRootEntities =
      includedDirectories.map { includedDirectory ->
        SourceRootEntity(
          url = includedDirectory,
          rootTypeId = SourceRootTypeId("generic-source"),
          entitySource = moduleEntitySource,
        )
      }
    val contentRootEntity =
      ContentRootEntity(
        url = project.rootDir.toVirtualFileUrl(virtualFileUrlManager),
        entitySource = moduleEntitySource,
        excludedPatterns = emptyList(),
      ) {
        this.sourceRoots = sourceRootEntities
        this.excludedUrls = excludedDirectories.map { ExcludeUrlEntity(it, moduleEntitySource) }
      }
    val moduleEntity =
      ModuleEntity(
        name = WORKSPACE_MODULE_NAME,
        dependencies = listOf(ModuleSourceDependency),
        entitySource = moduleEntitySource,
      ) {
        this.type = ModuleTypeId(ModuleTypeManager.getInstance().defaultModuleType.id)
        this.contentRoots = listOf(contentRootEntity)
      }

    environment.diff.workspaceModelDiff.mutableEntityStorage
      .addEntity(moduleEntity)
  }

  interface EnableWorkspaceModuleSyncHookExtension {
    suspend fun isEnabled(environment: ProjectSyncHook.ProjectSyncHookEnvironment): Boolean

    companion object {
      internal val EP =
        ExtensionPointName.create<EnableWorkspaceModuleSyncHookExtension>(
          "org.jetbrains.bazel.workspaceModuleSyncHook.enable",
        )
    }
  }
}
