@file:Suppress("UnstableApiUsage")

package org.jetbrains.bazel.workspace

import com.intellij.build.events.impl.SkippedResultImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.NlsSafe
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
import com.intellij.openapi.vfs.toNioPathOrNull
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
import com.intellij.workspaceModel.ide.toPath
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.server.connection.connection
import org.jetbrains.bazel.sync.status.SyncStatusService
import org.jetbrains.bazel.target.TargetUtils
import org.jetbrains.bazel.target.moduleEntity
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.ui.console.TaskConsole
import org.jetbrains.bazel.ui.console.syncConsole
import org.jetbrains.bazel.utils.SourceType
import org.jetbrains.bazel.utils.isSourceFile
import org.jetbrains.bazel.workspacemodel.entities.BazelDummyEntitySource
import org.jetbrains.bsp.protocol.InverseSourcesParams
import java.nio.file.Path
import java.util.UUID

class AssignFileToModuleListener : BulkFileListener {
  override fun after(events: MutableList<out VFileEvent>) {
    if (!ApplicationManager.getApplication().isUnitTestMode) {
      process(events)
    }
  }

  // the returned map is for testing purposes; Project location hash is used instead of Project since it's safer to store
  @VisibleForTesting
  fun process(events: List<VFileEvent>): Map<String, Job> {
    // Getting the affected file is quite an expensive operation (source: `VFileEvent` documentation).
    // To improve performance, the first event from the list is used to decide whether to process or not.
    // IntelliJ usually reports file events one by one, so this logic will rarely change anything.
    val firstEvent = events.firstOrNull() ?: return emptyMap()
    val file = getAffectedFile(firstEvent) ?: return emptyMap()
    val isSource =
      if (firstEvent is VFileDeleteEvent) {
        file.extension?.let { SourceType.fromExtension(it) } != null
      } else {
        file.isSourceFile()
      }
    return if (isSource) {
      getRelatedProjects(file).associateProjectIdWithJob { processWithDelay(it, events) }
    } else {
      emptyMap()
    }
  }

  private fun processWithDelay(project: Project, events: List<VFileEvent>): Job? {
    if (events.isEmpty()) return null

    val controller = Controller.getInstance(project)
    if (controller.isAnotherProcessingInProgress()) return null
    val eventIsFirstInQueue = controller.addEvents(events)

    // only the first event in the queue should trigger the delayed processing
    return if (eventIsFirstInQueue) {
      BazelCoroutineService.getInstance(project).start {
        delay(PROCESSING_DELAY)

        val workspaceModel = project.serviceAsync<WorkspaceModel>()
        val events = Controller.getInstance(project).getEventsAndClear()
        val originId = "file-event-" + UUID.randomUUID().toString()
        if (events.isNotEmpty()) {
          controller.processWithLock {
            try {
              processFileEvents(events = events, project = project, workspaceModel = workspaceModel, originId = originId)
            } catch (e: CancellationException) {
              project.syncConsole.finishTask(originId, BazelPluginBundle.message("file.change.processing.message.cancelled"), SkippedResultImpl())
              throw e
            }
          }
        }
      }
    } else {
      null
    }
  }

  @Service(Service.Level.PROJECT)
  private class Controller {
    private val eventQueue = mutableListOf<VFileEvent>()

    private val processingLock = Mutex()

    /** @return `true` if these events were the first to be reported in the batch, `false` otherwise */
    fun addEvents(events: List<VFileEvent>): Boolean {
      synchronized(this) {
        if (eventQueue.size > EVENT_QUEUE_LIMIT) return false
        val eventIsFirstInQueue = eventQueue.isEmpty()
        eventQueue += events
        return eventIsFirstInQueue
      }
    }

    fun getEventsAndClear(): List<VFileEvent> {
      synchronized(this) {
        val events = eventQueue.takeIf { it.size <= EVENT_QUEUE_LIMIT }?.toList() ?: emptyList()
        eventQueue.clear()
        return events
      }
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

private fun getNewFile(event: VFileEvent): VirtualFile? = if (event !is VFileDeleteEvent) getAffectedFile(event) else null

private fun getAffectedFile(event: VFileEvent): VirtualFile? {
  val file =
    when (event) {
      is VFileCreateEvent -> event.file
      is VFileDeleteEvent -> event.file
      is VFileMoveEvent -> event.file
      is VFileCopyEvent -> event.findCreatedFile()
      is VFilePropertyChangeEvent -> if (event.propertyName == VirtualFile.PROP_NAME) event.file else null
      else -> null
    }
  return if (file?.isDirectory == false) file else null
}

private fun getOldFilePath(event: VFileEvent): Path? =
  when (event) {
    is VFileCreateEvent -> null // explicit branch for code clarity
    is VFileCopyEvent -> null // explicit branch for code clarity
    is VFileDeleteEvent -> event.path
    is VFileMoveEvent -> event.oldPath
    is VFilePropertyChangeEvent -> if (event.propertyName == VirtualFile.PROP_NAME) event.oldPath else null
    else -> null
  }?.toNioPathOrNull()

fun getRelatedProjects(file: VirtualFile): List<Project> =
  ProjectManager // ProjectLocator::getProjectsForFile won't work, since it only recognises files already added to content roots
    .getInstance()
    .openProjects
    .filter {
      projectIsBazelAndContainsFile(it, file) &&
        it.hasAnyTargets() // if a project has no targets, there is no point in processing (also, it could interrupt the initial sync)
    }

private fun projectIsBazelAndContainsFile(project: Project, file: VirtualFile): Boolean {
  val rootDir =
    try {
      project.rootDir
    } catch (_: IllegalStateException) {
      return false
    }
  return project.isBazelProject && VfsUtil.isAncestor(rootDir, file, false)
}

private fun Project.hasAnyTargets(): Boolean = this.targetUtils.allTargets().any()

private fun List<Project>.associateProjectIdWithJob(action: (Project) -> Job?): Map<String, Job> =
  mapNotNull {
    val projectHash = it.locationHash
    val job = action(it)
    if (job != null) {
      projectHash to job
    } else {
      null
    }
  }.toMap()

private suspend fun processFileEvents(
  events: List<VFileEvent>,
  project: Project,
  workspaceModel: WorkspaceModel,
  originId: String,
) {
  val entityStorageDiff = MutableEntityStorage.from(workspaceModel.currentSnapshot)
  val fileChanges = events.map { it.getOldAndNewFile() }

  val progressTitle = getProgressTitle(fileChanges)
  val job = currentCoroutineContext()[Job]
  project.syncConsole.startTask(
    taskId = originId,
    title = progressTitle,
    message = BazelPluginBundle.message("file.change.processing.message.start"),
    showConsole = TaskConsole.ShowConsole.ON_FAIL,
    cancelAction = createCancelAction(job),
  )

  withBackgroundProgress(project, progressTitle) {
    reportSequentialProgress { reporter ->
      val targetUtils = project.serviceAsync<TargetUtils>()
      val contentRootsToRemove =
        processFileRemoved(
          fileChanges = fileChanges,
          project = project,
          workspaceModel = workspaceModel,
          targetUtils = targetUtils,
        )
      val mutableRemovalMap = contentRootsToRemove.toMutableMap()

      reporter.nextStep(PROGRESS_DELETE_STEP_SIZE)
      processFileCreated(
        fileChanges = fileChanges,
        project = project,
        workspaceModel = workspaceModel,
        entityStorageDiff = entityStorageDiff,
        progressReporter = reporter,
        mutableRemovalMap = mutableRemovalMap,
        originId = originId,
      )

      mutableRemovalMap.values.flatten().forEach { entityStorageDiff.removeEntity(it) }

      reporter.nextStep(endFraction = 100, text = BazelPluginBundle.message("file.change.processing.step.commit")) {
        workspaceModel.update("File changes processing (Bazel)") {
          it.applyChangesFrom(entityStorageDiff)
        }
      }
    }
  }


  project.syncConsole.finishTask(originId, BazelPluginBundle.message("file.change.processing.message.finish"))
}

@NlsSafe
private fun getProgressTitle(fileChanges: List<OldAndNewFile>): String =
  fileChanges
    .singleOrNull()
    ?.newFile
    ?.let { BazelPluginBundle.message("file.change.processing.title.single", it.name) }
    ?: BazelPluginBundle.message("file.change.processing.title.multiple")

private fun createCancelAction(
  job: Job?,
): () -> Unit =
  if (job != null) {
    { job.cancel(CancellationException("File change processing was cancelled")) }
  } else {
    {}
  }

private suspend fun processFileCreated(
  fileChanges: List<OldAndNewFile>,
  project: Project,
  workspaceModel: WorkspaceModel,
  entityStorageDiff: MutableEntityStorage,
  progressReporter: SequentialProgressReporter,
  mutableRemovalMap: MutableMap<ModuleEntity, List<ContentRootEntity>>,
  originId: String,
) {
  val newFiles = fileChanges.mapNotNull { it.newFile }
  if (newFiles.isEmpty()) return

  val existingModules = newFiles.associateWith { getModulesForFile(it, project) }
  val targetUtils = project.targetUtils

  @Suppress("DialogTitleCapitalization") // "Querying Bazel" has title capitalization, it's a false positive
  val newTargetsByPath =
    progressReporter.nextStep(
      endFraction = PROGRESS_QUERY_STEP_SIZE,
      text = BazelPluginBundle.message("file.change.processing.step.query"),
    ) {
      queryTargetsForFile(
        project = project,
        filePaths = newFiles.mapNotNull { it.toNioPathOrNull() },
        originId = originId,
      )
    } ?: emptyMap()

  for (newFile in newFiles) {
    val url = newFile.toVirtualFileUrl(workspaceModel.getVirtualFileUrlManager())
    val path = url.toPath()
    val targets = newTargetsByPath[path] ?: continue

    val modules =
      targets.mapNotNull { it.toModuleEntity(workspaceModel.currentSnapshot, project) }
    val modulesFileIsAlreadyIn = existingModules[newFile] ?: emptySet()
    for (module in modules) {
      // if we want a file to be both added and removed in the same module, neither of them will be done
      val moduleContainsContentRootForRemoval = mutableRemovalMap.remove(module) != null
      val fileAlreadyInModule = modulesFileIsAlreadyIn.contains(module)
      if (!moduleContainsContentRootForRemoval && !fileAlreadyInModule) {
        url.addToModule(entityStorageDiff, module, newFile.extension)
      }
    }
    targetUtils.addFileToTargetIdEntry(path, targets)
  }

}

suspend fun getModulesForFile(newFile: VirtualFile, project: Project): Set<ModuleEntity> {
  val modules = readAction { ProjectFileIndex.getInstance(project).getModulesForFile(newFile, true) }
  return modules
    .filter { it.moduleEntity?.entitySource != BazelDummyEntitySource }
    .mapNotNull { it.moduleEntity }
    .toSet()
}

private fun processFileRemoved(
  fileChanges: List<OldAndNewFile>,
  project: Project,
  workspaceModel: WorkspaceModel,
  targetUtils: TargetUtils,
): Map<ModuleEntity, List<ContentRootEntity>> {
  val oldFilePaths = fileChanges.mapNotNull { it.oldFile }
  val oldFileUrls =
    oldFilePaths.map { workspaceModel.getVirtualFileUrlManager().getOrCreateFromUrl(it.toString()) }
  val newFileUrls = // IntelliJ might have already changed the content root's path to the new one, so we need to check both
    fileChanges.mapNotNull { it.newFile?.toVirtualFileUrl(workspaceModel.getVirtualFileUrlManager()) }
  val allUrls = (oldFileUrls + newFileUrls).toSet()
  val modules =
    oldFilePaths
      .flatMap { targetUtils.getTargetsForPath(it) }
      .toSet()
      .mapNotNull { it.toModuleEntity(workspaceModel.currentSnapshot, project) }
  oldFilePaths.forEach { targetUtils.removeFileToTargetIdEntry(it) }
  return modules
    .associateWith { module -> findContentRoots(module, allUrls) }
    .filter { it.value.isNotEmpty() }
}

private suspend fun queryTargetsForFile(project: Project, filePaths: List<Path>, originId: String): Map<Path, List<Label>>? =
  if (!project.serviceAsync<SyncStatusService>().isSyncInProgress) {
    try {
      askForInverseSources(project, filePaths, originId)
    } catch (ex: Exception) {
      logger.debug(ex)
      null
    }
  } else {
    null
  }

private suspend fun askForInverseSources(project: Project, filePaths: List<Path>, originId: String): Map<Path, List<Label>> {
  val result =
    project.connection.runWithServer { bspServer ->
      bspServer
        .buildTargetInverseSources(InverseSourcesParams(originId, filePaths))
    }
  return result.targets
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

private fun findContentRoots(module: ModuleEntity, urls: Set<VirtualFileUrl>): List<ContentRootEntity> =
  module.contentRoots.filter { urls.contains(it.url) }

private const val PROCESSING_DELAY = 250L // not noticeable by the user, but if there are many events simultaneously, we will get them all
private val logger = Logger.getInstance(AssignFileToModuleListener::class.java)

private const val PROGRESS_DELETE_STEP_SIZE = 20
private const val PROGRESS_QUERY_STEP_SIZE = 80
private const val EVENT_QUEUE_LIMIT = 200

private data class OldAndNewFile(
  val oldFile: Path?, // the old file must be kept as a path, since this file no longer exists
  val newFile: VirtualFile?,
)

private fun VFileEvent.getOldAndNewFile(): OldAndNewFile =
  OldAndNewFile(
    oldFile = getOldFilePath(this),
    newFile = getNewFile(this),
  )
