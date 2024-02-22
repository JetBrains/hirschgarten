package org.jetbrains.plugins.bsp.services

import com.intellij.java.workspace.entities.javaSourceRoots
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.SingleFileSourcesTracker
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.WorkspaceModelChangeListener
import com.intellij.platform.backend.workspace.WorkspaceModelTopics
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.storage.VersionedStorageChange
import com.intellij.util.text.nullize

/**
 * Provides single file source information to the IntelliJ platform.
 *
 * For more information about single file source, please refer to [SingleFileSourcesTracker].
 */
internal class SingleFileSourcesTrackerImpl(private val project: Project) : SingleFileSourcesTracker {
  private val singleFileSourceData = SingleFileSourceData()
  private val moduleManager = ModuleManager.getInstance(project)
  private val workspaceModel = WorkspaceModel.getInstance(project)
  private val virtualFileUrlManager = workspaceModel.getVirtualFileUrlManager()
  private val supportedFileExtensions = listOf("java", "kt", "kts")

  private val lock = Any()

  init {
    populateSingleFileSourceData()
    subscribeToWorkspaceModelChange()
  }

  private fun populateSingleFileSourceData() {
    workspaceModel
      .currentSnapshot
      .entities(SourceRootEntity::class.java)
      .forEach { handleSingleFileSourceEntity(it, true) }
  }

  private fun subscribeToWorkspaceModelChange() {
    val connection = project.messageBus.connect()
    connection.subscribe(WorkspaceModelTopics.CHANGED, object : WorkspaceModelChangeListener {
      override fun changed(event: VersionedStorageChange) {
        val sourceRootChanges = event.getChanges(SourceRootEntity::class.java)
        sourceRootChanges.forEach {
          handleSingleFileSourceEntity(it.oldEntity, false)
          handleSingleFileSourceEntity(it.newEntity, true)
        }
      }
    })
  }

  private fun handleSingleFileSourceEntity(entity: SourceRootEntity?, isNewEntity: Boolean) {
    if (entity == null) return
    val module = extractModuleFromEntity(entity) ?: return
    val singleFileSourceCandidate = extractVirtualFileFromEntity(entity)
    singleFileSourceCandidate?.let { updateSingleFileSourceData(it, module, isNewEntity) }
  }

  private fun updateSingleFileSourceData(sourceFile: VirtualFile, module: Module, isNewEntity: Boolean) {
    if (sourceFile.isFileSupported()) synchronized(lock) {
      updateSingleFileSourceDataAccordingly(sourceFile, module, isNewEntity)
    }
  }

  private fun updateSingleFileSourceDataAccordingly(sourceFile: VirtualFile, module: Module, isNewEntity: Boolean) {
    val sourceDirectory = getSourceDirectory(sourceFile)
    if (isNewEntity) {
      addSourceDirectory(module, sourceDirectory)
    } else {
      removeSourceDirectory(module, sourceDirectory)
    }
  }

  private fun VirtualFile.isFileSupported(): Boolean =
    this.isFile && supportedFileExtensions.contains(this.extension)

  private fun extractModuleFromEntity(entity: SourceRootEntity): Module? =
    moduleManager.findModuleByName(entity.contentRoot.module.name)

  private fun extractVirtualFileFromEntity(entity: SourceRootEntity): VirtualFile? = entity.url.virtualFile

  private fun addSourceDirectory(module: Module, sourceDirectory: VirtualFile) {
    singleFileSourceData.addSourceDirectory(sourceDirectory, module)
  }

  private fun getSourceDirectory(file: VirtualFile): VirtualFile = file.parent

  private fun removeSourceDirectory(module: Module, sourceDirectory: VirtualFile) {
    singleFileSourceData.removeSourceDirectory(sourceDirectory, module)
  }

  override fun isSourceDirectoryInModule(dir: VirtualFile, module: Module): Boolean =
    singleFileSourceData.isSourceDirectoryInModule(dir, module)

  override fun isSingleFileSource(file: VirtualFile): Boolean = findSourceRootEntity(file) != null

  private fun findSourceRootEntity(file: VirtualFile): SourceRootEntity? {
    if (!file.isFileSupported()) return null
    val entityCandidates = workspaceModel
      .currentSnapshot
      .getVirtualFileUrlIndex()
      .findEntitiesByUrl(file.toVirtualFileUrl(virtualFileUrlManager))

    return entityCandidates
      .map { it.first }
      .filterIsInstance<SourceRootEntity>()
      .firstOrNull()
  }

  override fun getSourceDirectoryIfExists(file: VirtualFile): VirtualFile? =
    if (isSingleFileSource(file)) getSourceDirectory(file) else null

  override fun getPackageNameForSingleFileSource(file: VirtualFile): String? =
    findSourceRootEntity(file)?.javaSourceRoots?.firstOrNull()?.packagePrefix?.nullize()
}

/**
 * Stores data that tracks JVM single file source directories
 */
private class SingleFileSourceData {
  /**
   * Tracks the relationship between module (name) and JVM single files' parents' urls
   */
  private val sourceDirectoriesByModule = HashMap<String, HashMap<String, Int>>()

  fun addSourceDirectory(sourceDirectory: VirtualFile, module: Module) {
    val moduleName = module.name
    sourceDirectoriesByModule.putIfAbsent(moduleName, HashMap())
    sourceDirectoriesByModule[moduleName]?.compute(sourceDirectory.url) { _, currentOccurrence ->
      currentOccurrence?.plus(1) ?: 1
    }
  }

  fun removeSourceDirectory(sourceDirectory: VirtualFile, module: Module) {
    val moduleName = module.name
    val sourceDirectories = sourceDirectoriesByModule[moduleName]
    sourceDirectories?.let { doRemoveSourceDirectory(it, sourceDirectory, sourceDirectories, moduleName) }
  }

  private fun doRemoveSourceDirectory(
    dirs: HashMap<String, Int>,
    sourceDirectory: VirtualFile,
    sourceDirectories: HashMap<String, Int>,
    moduleName: @NlsSafe String,
  ) {
    dirs.compute(sourceDirectory.url) { _, currentOccurrence ->
      currentOccurrence?.minus(1)?.let { if (it <= 0) null else it }
    }
    if (sourceDirectories.isEmpty()) sourceDirectoriesByModule.remove(moduleName)
  }

  fun isSourceDirectoryInModule(dir: VirtualFile, module: Module): Boolean {
    val sourceDirectories = sourceDirectoriesByModule[module.name]
    return dir.isDirectory && sourceDirectories?.contains(dir.url) == true
  }
}
