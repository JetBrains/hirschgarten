package org.jetbrains.plugins.bsp.workspace

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.EntityStorage
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileIndexContributor
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileSetRegistrar
import org.jetbrains.plugins.bsp.workspacemodel.entities.BspDummyEntitySource
import kotlin.collections.contains

private val SOURCE_EXTENSIONS = listOf("java", "kt", "scala", "py")

private fun VirtualFile.isSourceFile(): Boolean {
  val isFile =
    try {
      this.isFile
    } catch (_: UnsupportedOperationException) {
      false
    }
  return isFile && extension?.lowercase() in SOURCE_EXTENSIONS
}

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
    // Since we register the exclusion at contentRootUrl,
    // it will be overridden if we add a file as a file-based source root at a subdirectory of contentRootUrl.
    registrar.registerExclusionCondition(
      root = contentRootUrl,
      condition = { it.isSourceFile() },
      entity = entity,
    )
  }
}
