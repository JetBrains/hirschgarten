package org.jetbrains.bazel.golang.workspace

import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleSourceDependency
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.golang.sync.extractGoBuildTarget
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.utils.filterPathsThatDontContainEachOther
import org.jetbrains.bazel.workspacemodel.entities.BazelModuleEntitySource
import java.nio.file.Path

internal const val GO_WORKSPACE_MODULE_NAME = ".workspace"

/**
 * Go plugin expects all sources to be under a content root.
 * Otherwise, e.g., Go SDK references aren't resolved, and source files aren't indexed.
 * By the way, we *don't* need to add source roots, those are specific to Java/Python and are ignored by the Go plugin.
 *
 * The fact that there is only one module does not, however, mean that Go doesn't support a proper dependency tree.
 * Resolve is handled via BazelGoImportResolver, unlike with Java plugin where it's done via module dependencies.
 */
internal class GoWorkspaceModuleProjectSyncHook : ProjectSyncHook {
  override fun isEnabled(project: Project): Boolean = BazelFeatureFlags.isGoSupportEnabled

  override suspend fun onSync(environment: ProjectSyncHook.ProjectSyncHookEnvironment) {
    val project = environment.project

    val workspacePath = project.rootDir.toNioPath()

    val goSourcesParentDirectories = environment.workspace.targets
      .asSequence()
      .filter { it.isWorkspace }
      .mapNotNull { extractGoBuildTarget(it) }
      .flatMap { it.sources }
      .filter { it.startsWith(workspacePath) }  // External files are already handled in GoExternalLibraryManager
      .map { source -> source.parent }
      .toSet()
      .filterPathsThatDontContainEachOther()
    if (goSourcesParentDirectories.isEmpty()) return
    
    val virtualFileUrlManager = project.serviceAsync<WorkspaceModel>().getVirtualFileUrlManager()
    val moduleEntitySource = BazelModuleEntitySource(GO_WORKSPACE_MODULE_NAME)

    val contentRoots = goSourcesParentDirectories.map { directory: Path ->
      ContentRootEntity(
        url = directory.toVirtualFileUrl(virtualFileUrlManager),
        entitySource = moduleEntitySource,
        excludedPatterns = emptyList(),
      )
    }

    val moduleEntity =
      ModuleEntity(
        name = GO_WORKSPACE_MODULE_NAME,
        dependencies = listOf(ModuleSourceDependency),
        entitySource = moduleEntitySource,
      ) {
        this.contentRoots = contentRoots
      }
    environment.diff.addEntity(moduleEntity)
  }
}

@ApiStatus.Internal
object GoWorkspaceModuleUtil {
  fun findModule(project: Project): Module? = ModuleManager.getInstance(project).findModuleByName(GO_WORKSPACE_MODULE_NAME)
}
