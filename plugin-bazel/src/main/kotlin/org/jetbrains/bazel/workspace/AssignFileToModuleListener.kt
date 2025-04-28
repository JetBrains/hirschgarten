@file:Suppress("UnstableApiUsage")

package org.jetbrains.bazel.workspace

import com.intellij.ide.impl.isTrusted
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.vfs.VfsUtil
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
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import com.intellij.workspaceModel.ide.toPath
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.sdkcompat.AssignFileToModuleListenerCompat
import org.jetbrains.bazel.server.connection.connection
import org.jetbrains.bazel.sync.status.SyncStatusService
import org.jetbrains.bazel.target.TargetUtils
import org.jetbrains.bazel.target.moduleEntity
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.utils.SourceType
import org.jetbrains.bazel.utils.isSourceFile
import org.jetbrains.bazel.workspacemodel.entities.BspDummyEntitySource
import org.jetbrains.bsp.protocol.InverseSourcesParams
import org.jetbrains.bsp.protocol.InverseSourcesResult
import org.jetbrains.bsp.protocol.TextDocumentIdentifier
import java.nio.file.Path
import kotlin.io.path.pathString

class AssignFileToModuleListener : BulkFileListener {
  private val pendingEvents = mutableMapOf<Project, MutableList<VFileEvent>>()

  override fun after(events: MutableList<out VFileEvent>) {
    if (!ApplicationManager.getApplication().isUnitTestMode) {
      testableAfter(events)
    }
  }

  @VisibleForTesting
  fun testableAfter(events: List<VFileEvent>): Map<Project, Job?> {
    // if the list has multiple events, it means an external operation (like Git) and resync is probably required anyway
    events.singleOrNull()?.let {
      val file = it.getAffectedFile() ?: return emptyMap()
      val isSource =
        if (it is VFileDeleteEvent) {
          file.extension?.let(SourceType::fromExtension) != null
        } else {
          file.isSourceFile()
        }
      if (isSource) {
        return file.getRelatedProjects().associateWith { project -> project.processWithDelay(it) }
      }
    }
    return emptyMap()
  }

  private fun Project.processWithDelay(event: VFileEvent): Job? {
    synchronized(pendingEvents) {
      @Suppress("KotlinUnreachableCode") // it is not unreachable, but IJ wrongly marks it as such
      pendingEvents[this]?.let {
        it.add(event)
        return null
      } ?: pendingEvents.put(this, mutableListOf(event))
    }
    return BazelCoroutineService
      .getInstance(this)
      .start {
        delay(PROCESSING_DELAY)
        synchronized(pendingEvents) { pendingEvents.remove(this)?.singleOrNull() }?.process(this)
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

private fun VFileEvent.getOldFilePath(): Path? =
  when (this) {
    is VFileCreateEvent -> null // explicit branch for code clarity
    is VFileCopyEvent -> null // explicit branch for code clarity
    is VFileDeleteEvent -> this.path
    is VFileMoveEvent -> this.oldPath
    is VFilePropertyChangeEvent -> if (this.propertyName == VirtualFile.PROP_NAME) this.oldPath else null
    else -> null
  }?.toNioPathOrNull()

private fun VirtualFile.getRelatedProjects(): List<Project> =
  ProjectManager
    .getInstance()
    .openProjects
    .filter { it.isRelevant() && VfsUtil.isAncestor(it.rootDir, this, false) }

private fun Project.isRelevant(): Boolean = this.isBazelProject && this.isTrusted()

private suspend fun VFileEvent.process(project: Project) {
  val workspaceModel = WorkspaceModel.getInstance(project)
  val targetUtils = project.targetUtils

  val newFile = this.getNewFile()
  val oldFilePath = this.getOldFilePath() // the old file must be kept as a path, since this file no longer exists

  runWithProgress(project) {
    val modulesToRemoveFrom =
      oldFilePath
        ?.let {
          processFileRemoved(
            oldFilePath = it,
            project = project,
            workspaceModel = workspaceModel,
            targetUtils = targetUtils,
          )
        }?.toMutableList() ?: mutableListOf()

    val modulesToAddTo =
      newFile?.let {
        processFileCreated(
          newFile = it,
          project = project,
          workspaceModel = workspaceModel,
          modulesToRemoveFrom = modulesToRemoveFrom,
        )
      } ?: emptyList()
    updateContentRoots(newFile, oldFilePath, modulesToRemoveFrom, modulesToAddTo, workspaceModel)
  }
}

private suspend fun runWithProgress(project: Project, action: suspend () -> Unit) {
  withBackgroundProgress(project, BazelPluginBundle.message("file.change.processing.title")) {
    action()
  }
}

private suspend fun processFileCreated(
  newFile: VirtualFile,
  project: Project,
  workspaceModel: WorkspaceModel,
  modulesToRemoveFrom: MutableList<ModuleEntity>,
): List<ModuleEntity> {
  val existingModules =
    AssignFileToModuleListenerCompat
      .getModulesForFile(newFile, project)
      .filter { it.moduleEntity?.entitySource != BspDummyEntitySource }
      .mapNotNull { it.moduleEntity }

  val url = newFile.toVirtualFileUrl(workspaceModel.getVirtualFileUrlManager())
  val path = url.toPath()
  val contentRootToAdd =
    queryTargetsForFile(project, url)
      ?.let { targets ->
        val modules =
          targets
            .mapNotNull { it.toModuleEntity(workspaceModel.currentSnapshot, project) }
        project.targetUtils.addFileToTargetIdEntry(path, targets)
        return@let modules
      } ?: emptyList()
  return contentRootToAdd.filter { !modulesToRemoveFrom.remove(it) && !existingModules.contains(it) }
}

private fun processFileRemoved(
  oldFilePath: Path,
  project: Project,
  workspaceModel: WorkspaceModel,
  targetUtils: TargetUtils,
): List<ModuleEntity> {
  val modules =
    targetUtils
      .getTargetsForPath(oldFilePath)
      .mapNotNull { it.toModuleEntity(workspaceModel.currentSnapshot, project) }
  project.targetUtils.removeFileToTargetIdEntry(oldFilePath)
  return modules
}

private suspend fun queryTargetsForFile(project: Project, fileUrl: VirtualFileUrl): List<Label>? =
  if (!SyncStatusService.getInstance(project).isSyncInProgress) {
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

private suspend fun updateContentRoots(
  newFile: VirtualFile?,
  oldFilePath: Path?,
  modulesToRemoveFrom: List<ModuleEntity>,
  modulesToAddTo: List<ModuleEntity>,
  workspaceModel: WorkspaceModel,
) {
  val urlManager = workspaceModel.getVirtualFileUrlManager()
  val oldUrl = oldFilePath?.let { urlManager.fromPath(it.pathString) }
  val newUrl = newFile?.toVirtualFileUrl(urlManager)
  workspaceModel.update("File changes processing") { storage ->
    modulesToRemoveFrom.forEach { module ->
      // we check both the old and the new URL, since IJ might have updated the file in the project model, but it is not guaranteed
      storage.modifyModuleEntity(module) { contentRoots = contentRoots.filter { it.url != oldUrl && it.url != newUrl } }
    }
    modulesToAddTo.forEach {
      val contentRoot = newFile?.toNewContentRoot(module = it, virtualFileUrlManager = urlManager)
      if (contentRoot != null) {
        storage.modifyModuleEntity(it) { contentRoots = contentRoots + contentRoot }
      }
    }
  }
}

private fun VirtualFile.toNewContentRoot(module: ModuleEntity, virtualFileUrlManager: VirtualFileUrlManager): ContentRootEntity.Builder? {
  val url = this.toVirtualFileUrl(virtualFileUrlManager)

  if (module.contentRoots.any { it.url == url }) return null // we don't want to duplicate content roots

  // TODO: https://youtrack.jetbrains.com/issue/BAZEL-1917
  val sourceRootType =
    when (extension) {
      "java" -> SourceRootTypeId("java-source")
      "kt" -> SourceRootTypeId("kotlin-source")
      "py" -> SourceRootTypeId("python-source")
      else -> {
        logger.warn("Bazel recognised a file as a source, but we failed to parse its extension: .$extension")
        SourceRootTypeId("unknown-source")
      }
    }

  val sourceRoot =
    SourceRootEntity(
      url = url,
      entitySource = module.entitySource,
      rootTypeId = sourceRootType,
    )

  val contentRootEntity =
    ContentRootEntity(
      url = url,
      excludedPatterns = emptyList(),
      entitySource = module.entitySource,
    ) {
      sourceRoots += listOf(sourceRoot)
    }

  return contentRootEntity
}

private const val PROCESSING_DELAY = 250L // no noticeable by the user, but if there are many events simultaneously, we will get them all
private val logger = Logger.getInstance(AssignFileToModuleListener::class.java)
