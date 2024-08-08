package org.jetbrains.plugins.bsp.workspace

import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.EntityStorage
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileIndexContributor
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileSetRegistrar
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.moduleEntity
import org.jetbrains.plugins.bsp.workspacemodel.entities.BspDummyEntitySource

private val SOURCE_EXTENSIONS = listOf("java", "kt", "scala")

/**
 * If a source file is added to the dummy module but isn't added to any other module, then we should not attempt to index/analyze it.
 */
class DummyModuleExclusionWorkspaceFileIndexContributor : WorkspaceFileIndexContributor<ModuleEntity> {
  override val entityClass: Class<ModuleEntity>
    get() = ModuleEntity::class.java

  override fun registerFileSets(
    entity: ModuleEntity,
    registrar: WorkspaceFileSetRegistrar,
    storage: EntityStorage,
  ) {
    if (entity.entitySource != BspDummyEntitySource) return
    val contentRootUrl = entity.contentRoots.single().url
    registrar.registerExclusionCondition(
      root = contentRootUrl,
      condition = { it.isUnderDummyModule() },
      entity = entity,
    )
  }

  private fun VirtualFile.isUnderDummyModule(): Boolean {
    val extension = this.extension ?: return false
    // Don't exclude files like README.md just because they're under the dummy module
    if (extension.lowercase() !in SOURCE_EXTENSIONS) return false
    val project = ProjectLocator.getInstance().guessProjectForFile(this) ?: return false
    // Don't honor the exclusions here (second parameter) in order to avoid infinite recursion
    val module = ProjectFileIndex.getInstance(project).getModuleForFile(this, false) ?: return false
    return module.moduleEntity?.entitySource == BspDummyEntitySource
  }
}
