@file:Suppress("UnstableApiUsage")

package org.jetbrains.bazel.workspace

import com.intellij.ide.impl.isTrusted
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
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
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.intellij.platform.util.progress.reportSequentialProgress
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.platform.workspace.storage.ImmutableEntityStorage
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.intellij.workspaceModel.ide.legacyBridge.impl.java.JAVA_SOURCE_ROOT_ENTITY_TYPE_ID
import com.intellij.workspaceModel.ide.toPath
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    val controller = Controller.getInstance(project)
    if (controller.isAnotherProcessingInProgress()) return
    val eventIsFirstInQueue = controller.addEvent(event)

    // only the first event in the queue should trigger the delayed processing
    if (eventIsFirstInQueue) {
      BazelCoroutineService.getInstance(project).start {
        delay(PROCESSING_DELAY)

        val workspaceModel = project.serviceAsync<WorkspaceModel>()
        val event = Controller.getInstance(project).popAllEvents().singleOrNull()
        if (event != null) {
          controller.processWithLock {
            processFileEvent(event = event, project = project, workspaceModel = workspaceModel)
          }
        }
      }
    }
  }

  @Service(Service.Level.PROJECT)
  private class Controller {
    // synchronised lists do not guarantee safety of operations like size checking and clearing - we need explicit synchronisation here
    private val pendingEvents = mutableListOf<VFileEvent>()
    private val processingLock = Mutex()

    /** @return `true` if this event was the first to be added, `false` otherwise */
    fun addEvent(event: VFileEvent): Boolean =
      synchronized(pendingEvents) {
        val isEmpty = pendingEvents.isEmpty()
        pendingEvents.add(event)
        isEmpty
      }

    fun popAllEvents(): List<VFileEvent> =
      synchronized(pendingEvents) {
        val events = pendingEvents.toList()
        pendingEvents.clear()
        events
      }

    suspend fun processWithLock(action: suspend () -> Unit) {
      try {
        processingLock.withLock(this) {
          action()
        }
      } catch (_: IllegalStateException) {
        // it means that Mutex::withLock was called with Mutex already locked - in that case, we just want not to start the processing
      }
    }

    fun isAnotherProcessingInProgress(): Boolean = processingLock.isLocked

    companion object {
      @JvmStatic
      fun getInstance(project: Project): Controller = project.service()
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
  val entityStorageDiff = MutableEntityStorage.from(workspaceModel.currentSnapshot)

  val newFile = event.getNewFile()
  val oldFilePath = event.getOldFilePath()

  withBackgroundProgress(project, event.getProgressMessage(newFile)) {
    reportSequentialProgress { reporter ->
      oldFilePath?.let {
        val targetUtils = project.serviceAsync<TargetUtils>()
        processFileRemoved(
          oldFilePath = it,
          newFile = newFile,
          project = project,
          workspaceModel = workspaceModel,
          targetUtils = targetUtils,
          entityStorageDiff = entityStorageDiff,
        )
      }
      reporter.nextStep(PROGRESS_DELETE_STEP_SIZE)
      newFile?.let {
        processFileCreated(
          newFile = it,
          project = project,
          workspaceModel = workspaceModel,
          entityStorageDiff = entityStorageDiff,
          progressReporter = reporter,
        )
      }

      reporter.nextStep(endFraction = 100, text = BazelPluginBundle.message("file.change.processing.step.commit")) {
        workspaceModel.update("File changes processing (Bazel)") {
          it.applyChangesFrom(entityStorageDiff)
        }
      }
    }
  }
}

private fun VFileEvent.getProgressMessage(newFile: VirtualFile?): String =
  when (this) {
    is VFileCreateEvent -> BazelPluginBundle.message("file.change.processing.title.create", newFile?.name ?: "")
    is VFileDeleteEvent -> BazelPluginBundle.message("file.change.processing.title.delete")
    else -> BazelPluginBundle.message("file.change.processing.title.change", newFile?.name ?: "")
  }

private suspend fun processFileCreated(
  newFile: VirtualFile,
  project: Project,
  workspaceModel: WorkspaceModel,
  entityStorageDiff: MutableEntityStorage,
  progressReporter: SequentialProgressReporter,
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
  val targets =
    progressReporter.nextStep(
      endFraction = PROGRESS_QUERY_STEP_SIZE,
      text = BazelPluginBundle.message("file.change.processing.step.query"),
    ) { queryTargetsForFile(project, url) } ?: return

  val modules =
    targets
      .mapNotNull { it.toModuleEntity(workspaceModel.currentSnapshot, project) }
//        .filter { !existingModules.contains(it) } // 251
      .filter { it != existingModule } // 243
  modules.forEach { url.addToModule(entityStorageDiff, it, newFile.extension) }
  project.targetUtils.addFileToTargetIdEntry(path, targets)
}

private fun processFileRemoved(
  oldFilePath: String,
  newFile: VirtualFile?,
  project: Project,
  workspaceModel: WorkspaceModel,
  targetUtils: TargetUtils,
  entityStorageDiff: MutableEntityStorage,
) {
  val oldUrl = workspaceModel.getVirtualFileUrlManager().fromPath(oldFilePath)
  val oldUri = oldFilePath.toNioPathOrNull()!!
  val newUrl = newFile?.toVirtualFileUrl(workspaceModel.getVirtualFileUrlManager())
  val modules =
    targetUtils
      .getTargetsForPath(oldUri)
      .mapNotNull { it.toModuleEntity(workspaceModel.currentSnapshot, project) }
  modules.forEach {
    oldUrl.removeFromModule(entityStorageDiff, it)
    newUrl?.removeFromModule(entityStorageDiff, it) // IntelliJ might have already changed the content root's path to the new one
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

private fun VirtualFileUrl.addToModule(
  entityStorageDiff: MutableEntityStorage,
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

  entityStorageDiff.modifyModuleEntity(module) { contentRoots += contentRootEntity }
}

private fun VirtualFileUrl.removeFromModule(entityStorageDiff: MutableEntityStorage, module: ModuleEntity) {
  entityStorageDiff.modifyModuleEntity(module) { contentRoots = contentRoots.filter { it.url != this@removeFromModule } }
}

private const val PROCESSING_DELAY = 250L // not noticeable by the user, but if there are many events simultaneously, we will get them all
private val logger = Logger.getInstance(AssignFileToModuleListener::class.java)

private const val PROGRESS_DELETE_STEP_SIZE = 20
private const val PROGRESS_QUERY_STEP_SIZE = 80
