package org.jetbrains.plugins.bsp.workspace

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.fileTypes.impl.FileTypeOverrider
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.EntityStorage
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileIndexContributor
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileSetRegistrar
import org.jetbrains.plugins.bsp.config.isBspProject
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
    // Since we register the exclusion at contentRootUrl,
    // it will be overridden if we add a file as a file-based source root at a subdirectory of contentRootUrl.
    entity.contentRoots.map { contentRoot ->
      val contentRootUrl = contentRoot.url
      registrar.registerExclusionCondition(
        root = contentRootUrl,
        condition = { it.isSourceFile() },
        entity = entity,
      )
    }
  }
}

/**
 * Make sure the files we excluded aren't parsed
 */
class ExcludedFileTypeOverrider : FileTypeOverrider {
  override fun getOverriddenFileType(file: VirtualFile): FileType? {
    if (!file.isSourceFile()) return null
    val project = ProjectLocator.getInstance().guessProjectForFile(file) ?: return null
    if (!project.isBspProject) return null
    val projectFileIndex = ProjectFileIndex.getInstance(project)
    val excluded = runReadAction { projectFileIndex.isExcluded(file) }
    return if (excluded) {
      PlainTextFileType.INSTANCE
    } else {
      null
    }
  }
}
