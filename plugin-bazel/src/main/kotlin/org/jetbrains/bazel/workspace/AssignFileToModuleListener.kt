@file:Suppress("UnstableApiUsage")

package org.jetbrains.bazel.workspace

import com.intellij.ide.impl.isTrusted
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.platform.workspace.storage.ImmutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.intellij.workspaceModel.ide.legacyBridge.impl.java.JAVA_SOURCE_ROOT_ENTITY_TYPE_ID
import com.intellij.workspaceModel.ide.toPath
import kotlinx.coroutines.delay
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.server.connection.connection
import org.jetbrains.bazel.sync.status.SyncStatusService
import org.jetbrains.bazel.target.TargetUtils
import org.jetbrains.bazel.target.moduleEntity
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.utils.SourceType
import org.jetbrains.bazel.utils.isSourceFile
import org.jetbrains.bazel.workspacemodel.entities.BazelDummyEntitySource
import org.jetbrains.bsp.protocol.InverseSourcesParams
import org.jetbrains.bsp.protocol.InverseSourcesResult
import org.jetbrains.bsp.protocol.TextDocumentIdentifier

internal class AssignFileToModuleListener : BulkFileListener {
  private val pendingEvents = mutableMapOf<Project, MutableList<VFileEvent>>()

  override fun after(events: MutableList<out VFileEvent>) {
    // if the list has multiple events, it means an external operation (like Git) and resync is probably required anyway
    val event = events.singleOrNull() ?: return
    val file = event.getAffectedFile() ?: return
    val isSource =
      if (event is VFileDeleteEvent) {
        file.extension?.let { SourceType.fromExtension(it) } != null
      } else {
        file.isSourceFile()
      }
    if (isSource) {
      getRelatedProjects(file).forEach { project -> processWithDelay(project, event) }
    }
  }

  private fun processWithDelay(project: Project, event: VFileEvent) {
    synchronized(pendingEvents) {
      pendingEvents.computeIfAbsent(project) { mutableListOf() }.add(event)
    }

    BazelCoroutineService.getInstance(project).start {
      delay(PROCESSING_DELAY)

      val workspaceModel = project.serviceAsync<WorkspaceModel>()
      val event = synchronized(pendingEvents) { pendingEvents.remove(project)?.singleOrNull() }
      if (event != null) {
        processFileEvent(event = event, project = project, workspaceModel = workspaceModel)
      }
    }
  }
}

private fun VFileEvent.getNewFile(): VirtualFile? = if (this !is VFileDeleteEvent) getAffectedFile() else null

private fun VFileEvent.getAffectedFile(): VirtualFile? {
  val file =
    when (this) {
      is VFileCreateEvent -> this.file
      is VFileDeleteEvent -> this.file
      is VFileMoveEvent -> this.file
      is VFileCopyEvent -> this.findCreatedFile()
      is VFilePropertyChangeEvent -> if (this.propertyName == VirtualFile.PROP_NAME) this.file else null
      else -> null
    }
  return if (file?.isDirectory == false) file else null
}

private fun VFileEvent.getOldFilePath(): String? =
  when (this) {
    is VFileCreateEvent -> null // explicit branch for code clarity
    is VFileCopyEvent -> null // explicit branch for code clarity
    is VFileDeleteEvent -> this.path
    is VFileMoveEvent -> this.oldPath
    is VFilePropertyChangeEvent -> if (this.propertyName == VirtualFile.PROP_NAME) this.oldPath else null
    else -> null
  }

private fun getRelatedProjects(file: VirtualFile): List<Project> {
  val projectManager = ProjectManager.getInstance()
  val projectLocator = ProjectLocator.getInstance()
  return if (file.isValid) {
    projectLocator.getProjectsForFile(file).filterNotNull().filter { it.doWeCareAboutIt() }
  } else {
    projectManager.openProjects.filter { it.doWeCareAboutIt() } // the project locator would return an empty list
  }
}

private fun Project.doWeCareAboutIt(): Boolean = this.isBazelProject && this.isTrusted()

private suspend fun processFileEvent(
  event: VFileEvent,
  project: Project,
  workspaceModel: WorkspaceModel,
) {
  val storage = workspaceModel.currentSnapshot

  val newFile = event.getNewFile()
  val oldFilePath = event.getOldFilePath()

  withBackgroundProgress(project, BazelPluginBundle.message("file.change.processing.title")) {
    oldFilePath?.let {
      val targetUtils = project.serviceAsync<TargetUtils>()
      processFileRemoved(
        oldFilePath = it,
        newFile = newFile,
        project = project,
        workspaceModel = workspaceModel,
        targetUtils = targetUtils,
        storage = storage,
      )
    }
    newFile?.let {
      processFileCreated(
        newFile = it,
        project = project,
        workspaceModel = workspaceModel,
        storage = storage,
      )
    }
  }
}

private suspend fun processFileCreated(
  newFile: VirtualFile,
  project: Project,
  workspaceModel: WorkspaceModel,
  storage: ImmutableEntityStorage,
) {
  // ProjectFileIndex::getModulesForFile is not compatible with IJ 2024, but it's not important enough to bother with SDK-compat
  // TODO: replace once 243 is dropped
//  val existingModules =
//    readAction { ProjectFileIndex.getInstance(project).getModulesForFile(newFile, true) }
//      .filter { it.moduleEntity?.entitySource != BspDummyEntitySource }
//      .mapNotNull { it.moduleEntity }

  val existingModule =
    readAction { ProjectFileIndex.getInstance(project).getModuleForFile(newFile) }.let {
      if (it?.moduleEntity?.entitySource != BazelDummyEntitySource) it else null
    }

  val url = newFile.toVirtualFileUrl(workspaceModel.getVirtualFileUrlManager())
  val path = url.toPath()
  queryTargetsForFile(project, url)
    ?.let { targets ->
      val modules =
        targets
          .mapNotNull { it.toModuleEntity(storage, project) }
//          .filter { !existingModules.contains(it) } // 251
          .filter { it != existingModule } // 243
      modules.forEach { url.addToModule(workspaceModel, it, newFile.extension) }
      project.targetUtils.addFileToTargetIdEntry(path, targets)
    }
}

private suspend fun processFileRemoved(
  oldFilePath: String,
  newFile: VirtualFile?,
  project: Project,
  workspaceModel: WorkspaceModel,
  targetUtils: TargetUtils,
  storage: ImmutableEntityStorage,
) {
  val oldUrl = workspaceModel.getVirtualFileUrlManager().fromPath(oldFilePath)
  val oldUri = oldFilePath.toNioPathOrNull()!!
  val newUrl = newFile?.toVirtualFileUrl(workspaceModel.getVirtualFileUrlManager())
  val modules =
    targetUtils
      .getTargetsForPath(oldUri)
      .mapNotNull { it.toModuleEntity(storage, project) }
  modules.forEach {
    oldUrl.removeFromModule(workspaceModel, it)
    newUrl?.removeFromModule(workspaceModel, it) // IntelliJ might have already changed the content root's path to the new one
  }
  targetUtils.removeFileToTargetIdEntry(oldUri)
}

private suspend fun queryTargetsForFile(project: Project, fileUrl: VirtualFileUrl): List<Label>? =
  if (!project.serviceAsync<SyncStatusService>().isSyncInProgress) {
    try {
      askForInverseSources(project, fileUrl)
        .targets
        .toList()
    } catch (ex: Exception) {
      logger.debug(ex)
      null
    }
  } else {
    null
  }

private suspend fun askForInverseSources(project: Project, fileUrl: VirtualFileUrl): InverseSourcesResult =
  project.connection.runWithServer { bspServer ->
    bspServer
      .buildTargetInverseSources(InverseSourcesParams(TextDocumentIdentifier(fileUrl.toPath())))
  }

private fun Label.toModuleEntity(storage: ImmutableEntityStorage, project: Project): ModuleEntity? {
  val moduleId = ModuleId(this.formatAsModuleName(project))
  return storage.resolve(moduleId)
}

private suspend fun VirtualFileUrl.addToModule(
  workspaceModel: WorkspaceModel,
  module: ModuleEntity,
  extension: String?,
) {
  if (module.contentRoots.any { it.url == this }) return // we don't want to duplicate content roots

  // TODO: https://youtrack.jetbrains.com/issue/BAZEL-1917
  val sourceRootType =
    when (extension) {
      "java" -> JAVA_SOURCE_ROOT_ENTITY_TYPE_ID
      "kt" -> SourceRootTypeId("kotlin-source")
      "py" -> SourceRootTypeId("python-source")
      else -> {
        logger.warn("Bazel recognised a file as a source, but we failed to parse its extension: .$extension")
        SourceRootTypeId("unknown-source")
      }
    }

  val sourceRoot =
    SourceRootEntity(
      url = this,
      entitySource = module.entitySource,
      rootTypeId = sourceRootType,
    )

  val contentRootEntity =
    ContentRootEntity(
      url = this,
      excludedPatterns = emptyList(),
      entitySource = module.entitySource,
    ) {
      sourceRoots += listOf(sourceRoot)
    }

  updateContentRoots(workspaceModel, module) { it + contentRootEntity }
}

private suspend fun VirtualFileUrl.removeFromModule(workspaceModel: WorkspaceModel, module: ModuleEntity) {
  updateContentRoots(workspaceModel, module) { it.filter { contentRoot -> contentRoot.url != this } }
}

private suspend fun updateContentRoots(
  workspaceModel: WorkspaceModel,
  module: ModuleEntity,
  updater: (List<ContentRootEntity.Builder>) -> List<ContentRootEntity.Builder>,
) {
  workspaceModel.update("File changes processing") {
    it.modifyModuleEntity(module) {
      contentRoots = updater(contentRoots)
    }
  }
}

private const val PROCESSING_DELAY = 250L // no noticeable by the user, but if there are many events simultaneously, we will get them all
private val logger = Logger.getInstance(AssignFileToModuleListener::class.java)
