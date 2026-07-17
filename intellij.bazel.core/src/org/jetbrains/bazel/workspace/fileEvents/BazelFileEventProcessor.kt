package org.jetbrains.bazel.workspace.fileEvents

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.build.events.impl.SkippedResultImpl
import com.intellij.build.events.impl.SuccessResultImpl
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.rethrowControlFlowException
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.platform.workspace.storage.ImmutableEntityStorage
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileIndex
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileSetWithCustomData
import com.intellij.workspaceModel.core.fileIndex.impl.JvmPackageRootDataInternal
import com.intellij.workspaceModel.core.fileIndex.impl.ModuleRelatedRootData
import com.intellij.workspaceModel.ide.isEqualOrParentOf
import com.intellij.workspaceModel.ide.legacyBridge.findModuleEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.utils.StarlarkSrcsListEval
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.progress.ShowConsole
import org.jetbrains.bazel.progress.syncConsole
import org.jetbrains.bazel.projectAware.BazelProjectAware
import org.jetbrains.bazel.run.task.BazelBuildTaskListener
import org.jetbrains.bazel.server.connection
import org.jetbrains.bazel.sync.status.SyncStatusService
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService
import org.jetbrains.bazel.ui.status.BazelFileStatusRefresher
import org.jetbrains.bazel.workspace.fileEvents.SimplifiedFileEvent.Create
import org.jetbrains.bazel.workspace.fileEvents.SimplifiedFileEvent.CreateDirectory
import org.jetbrains.bazel.workspace.packageMarker.concatenatePackages
import org.jetbrains.bazel.workspacemodel.entities.BazelDummyEntitySource
import org.jetbrains.bazel.workspacemodel.entities.PackageMarkerEntity
import org.jetbrains.bazel.workspacemodel.entities.PackageMarkerEntityBuilder
import org.jetbrains.bazel.workspacemodel.entities.bazelModuleExtension
import org.jetbrains.bazel.workspacemodel.entities.packageMarkerEntities
import org.jetbrains.bsp.protocol.InverseSourcesParams
import org.jetbrains.bsp.protocol.TaskGroupId
import org.jetbrains.bsp.protocol.TaskId
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.name
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

@ApiStatus.Internal
interface BazelFileEventProcessor {
  /**
   * Return true if events were queued and processed
   */
  suspend fun enqueue(events: List<VFileEvent>): Deferred<Boolean>

  /**
   * Check if queue is empty and no events are being processed
   */
  fun isIdle(): Boolean

  companion object {
    @JvmStatic
    fun getInstance(project: Project): BazelFileEventProcessor = project.service()
  }
}

@ApiStatus.Internal
open class DefaultBazelFileEventProcessor(private val project: Project): BazelFileEventProcessor {
  private val targetUtils = project.targetUtils
  private val eventsQueue = Channel<EventsBatch>(Channel.UNLIMITED)

  private val eventsRequestCounter = AtomicInteger(0)
  private val eventsProcessCounter = AtomicInteger(0)

  private class EventsBatch(
    val events: List<SimplifiedFileEvent>,
    val result: CompletableDeferred<Boolean>,
  )

  init {
    //coroutineScope.launch {
    BazelCoroutineService.getInstance(project).start {
      while(true) {
        val batches = ArrayList<EventsBatch>()
        batches.add(eventsQueue.receive())
        // load all available events after small pause
        do {
          delay(PROCESSING_DELAY)
          val moreEvents = eventsQueue.tryReceive().getOrNull()
          if (moreEvents != null) {
            batches.add(moreEvents)
          }
        } while (moreEvents != null)
        processEventsBatch(batches)
      }
    }
  }

  override suspend fun enqueue(events: List<VFileEvent>): Deferred<Boolean> {
    // if a project has no targets, there is no point in processing (also, it could interrupt the initial sync)
    targetUtils.awaitLoaded()
    if (!targetUtils.allTargets().any())
      return CompletableDeferred(false)

    val simplifiedEvents = events
      .mapNotNull { SimplifiedFileEvent.from(it) }
      .filter { it.shouldBeProcessed(project) }
      .filterByProject()

    if (simplifiedEvents.isEmpty())
      return CompletableDeferred(false)

    val result = CompletableDeferred<Boolean>()
    eventsRequestCounter.addAndGet(simplifiedEvents.size)
    eventsQueue.send(EventsBatch(simplifiedEvents, result))
    return result
  }

  override fun isIdle(): Boolean {
    return eventsRequestCounter.get() == eventsProcessCounter.get()
  }

  private val allowBazelQuery: Boolean get() = BazelFeatureFlags.queryBazelOnFileEvents
  private val allowPsiEvaluation: Boolean get() = BazelFeatureFlags.evaluatePsiOnFileEvents

  private suspend fun processEventsBatch(batches: List<EventsBatch>) {
    val jobManager = FileEventJobManager.getInstance(project)
    val taskGroupId = jobManager.syncTaskGroupId ?: TaskGroupId("file-event-" + Random.nextBytes(8).toHexString())
    val taskId = taskGroupId.task("file-event-processing")

    val events = batches.flatMap { it.events }
    val success = AtomicBoolean(false)

    val processingJob = jobManager.runFileEventsProcessing {
      BazelCoroutineService.getInstance(project).startAsync(true) {
        try {
          val progressTitle = getProgressTitle(events)

          val workspaceModel = project.serviceAsync<WorkspaceModel>()
          val entityStorageDiff = MutableEntityStorage.from(workspaceModel.currentSnapshot)

          BazelFileEventProgressReporter.runWithProgressBar(progressTitle, project) { bazelReporter ->
            val context = ProcessingContext(
              urlManager = workspaceModel.getVirtualFileUrlManager(),
              workspaceModel = workspaceModel,
              workspaceSnapshot = workspaceModel.currentSnapshot,
              entityStorageDiff = entityStorageDiff,
              taskId = taskId,
              progressReporter = bazelReporter,
            )

            processEventsBatchImpl(
              events = events,
              context = context
            )
          }

          success.set(true)
        }
        catch (ex: Throwable) {
          if (ex !is CancellationException)
            logger.error(ex)
          throw ex
        }
      }
    }

    try {
      if (processingJob == null) {
        // Cannot spawn job, most likely sync is in progress
        return
      }

      // no need to start sync console task if no Bazel processing is allowed
      if (allowBazelQuery) {
        startSyncConsoleTask(project, processingJob, taskId)
      }
      try {
        processingJob.join()
      }
      finally {
        BazelTaskEventsService.getInstance(project).removeListener(taskId.taskGroupId)
      }
    }
    finally {
      eventsProcessCounter.addAndGet(events.size)
      batches.forEach {
        logger.runCatching { it.result.complete(success.get()) }
      }
      BazelFileStatusRefresher.getInstance(project).refreshAllFilesPresentation()
    }
  }

  private fun startSyncConsoleTask(project: Project, processingJob: Job, taskId: TaskId) {
    val syncConsole = project.syncConsole
    syncConsole.startTask(
      taskId = taskId,
      title = BazelPluginBundle.message("file.change.processing.title.multiple"),
      message = BazelPluginBundle.message("file.change.processing.message.start"),
      showConsole = ShowConsole.ON_FAIL,
      cancelAction = { processingJob.cancel() },
    )
    val taskListener = BazelBuildTaskListener(syncConsole)
    BazelTaskEventsService.getInstance(project).saveListener(taskId.taskGroupId, taskListener)
    processingJob.invokeOnCompletion { exception ->
      val (message, result) =
        when (exception) {
          null -> BazelPluginBundle.message("file.change.processing.message.finish") to SuccessResultImpl()
          is CancellationException -> BazelPluginBundle.message("file.change.processing.message.cancelled") to SkippedResultImpl()
          else -> BazelPluginBundle.message("file.change.processing.message.failed") to FailureResultImpl(exception)
        }
      project.syncConsole.finishTask(taskId, message, result)
    }
  }

  private suspend fun processEventsBatchImpl(events: List<SimplifiedFileEvent>, context: ProcessingContext) {
    doProcessFileEvents(
      events.flatMap { event ->
        if (event is CreateDirectory) {
          collectCreatedFiles(event.newVirtualFile)
        } else if (event.newVirtualFile?.isDirectory == true) {
          emptyList() // ignore other directory events
        } else {
          listOf(event)
        }
      },
      context,
    )

    doProcessDirectoryEvents(
      events.filterIsInstance<CreateDirectory>(),
      context
    )

    // Finalize and apply changes
    context.progressReporter.startFinalisingStep()
    context.workspaceModel.update("File event processing (Bazel)") {
      it.applyChangesFrom(context.entityStorageDiff)
    }
  }

  private fun collectCreatedFiles(root: VirtualFile?): List<Create> {
    if (root == null || !root.isValid) return emptyList()

    val filesInDirectory = ArrayList<Create>()
    try {
      ProjectFileIndex.getInstance(project).iterateContentUnderDirectory(root) { file ->
        if (!file.isValid) return@iterateContentUnderDirectory true
        if (!file.isDirectory) {
          val createFileEvent = Create(file.path, file)
          if (createFileEvent.shouldBeProcessed(project))
            filesInDirectory.add(createFileEvent)
        }
        true
      }
    }
    catch (ex: IllegalArgumentException) {
      logger.debug("Stopped collecting created files for stale directory ${root.path}", ex)
    }
    return filesInDirectory
  }

  private suspend fun doProcessFileEvents(events: List<SimplifiedFileEvent>, context: ProcessingContext) {
    if (events.isEmpty())
      return

    context.progressReporter.startPreparationStep()

    val targetsByPath: Map<Path, List<Label>> =
      events
        .mapNotNull { event -> event.toPathAndVFile()?.takeIf { !it.vFile.isDirectory } }
        .distinctBy { it.path }
        .let { files ->
          // avoid running a Bazel query when not required (BAZEL-2458)
          if (files.isNotEmpty()) {
            context.progressReporter.startQueryStep {
              invertedSourcesQuery(context.taskId, files)
            }
          } else {
            context.progressReporter.skipQueryStep()
            emptyMap()
          }
        }

    val removedPaths = HashSet<Path>()
    val addedPaths = HashSet<Path>()

    for (event in events) {
      val removedFile = event.fileRemoved
      val addedFile = event.fileAdded

      val removedFileUrl = removedFile?.toVirtualFileUrl(context.urlManager)
      val addedFileUrl = addedFile?.toVirtualFileUrl(context.urlManager)

      val oldTargets: Set<Label> = removedFile?.let { targetUtils.getTargetsForPath(it) }?.toSet() ?: emptySet()
      val newTargets: Set<Label> = addedFile?.let { targetsByPath[it] }?.toSet() ?: emptySet()

      if (removedFile != null && oldTargets.isNotEmpty() && removedPaths.add(removedFile)) {
        targetUtils.removeFileToTargetIdEntry(removedFile)
        (oldTargets - newTargets).forEach { toRemove ->
          val module = toRemove.toModuleEntity(context.workspaceSnapshot, project)
          if (module != null) {
            val contentRoots = module.contentRoots.filter { it.url == removedFileUrl || it.url == addedFileUrl }
            contentRoots.forEach { context.entityStorageDiff.removeEntity(it) }
          }
        }
      }

      if (addedFile != null && newTargets.isNotEmpty() && addedPaths.add(addedFile) && addedFileUrl != null) {
        targetUtils.addFileToTargetIdEntry(addedFile, newTargets)
        (newTargets - oldTargets).forEach { toAdd ->
          val module = toAdd.toModuleEntity(context.workspaceSnapshot, project)
          if (module != null) {
            addFileToModule(addedFileUrl, context.entityStorageDiff, module)
          }
        }
      }
    }
  }

  private suspend fun doProcessDirectoryEvents(events: List<CreateDirectory>, context: ProcessingContext) {
    for (event in events) {
      // Update package marker
      val entity = updatePackageMarkerEntity(event, context)
      if (entity != null) {
        context.entityStorageDiff.modifyModuleEntity(entity.first) {
          this.packageMarkerEntities += entity.second
        }
      }
    }
  }

  protected open suspend fun invertedSourcesQuery(
    taskId: TaskId,
    files: Collection<PathAndVFile>
  ): Map<Path, List<Label>> {
    return if (allowBazelQuery) {
      val queryResult = queryTargetsForFile(project, files.map { it.path }, taskId)
      queryResult ?: emptyMap()
    } else if (allowPsiEvaluation) {
      // Heuristically evaluate targets by looking into PSI
      val emptyEvaluation = ArrayList<Path>()
      val eval = StarlarkSrcsListEval(project)

      files.mapNotNull { file ->
        val targets = readAction { eval.findTargetsForSourceFile(file.vFile) }
        if (targets.isEmpty()) {
          // Cannot determine the targets for file - show "Resync" button.
          emptyEvaluation.add(file.path)
          return@mapNotNull null
        }
        file.path to targets
      }.toMap().also {
        if (emptyEvaluation.isNotEmpty()) {
          logger.warn("Cannot evaluate targets for ${emptyEvaluation.size} new files. Show \"Sync Bazel changes\" button. ${emptyEvaluation.take(3).joinToString()}")
          BazelProjectAware.notify(project)
        }
      }
    } else {
      BazelProjectAware.notify(project)
      emptyMap()
    }
  }

  private suspend fun updatePackageMarkerEntity(
    event: CreateDirectory,
    context: ProcessingContext,
  ): Pair<ModuleEntity, PackageMarkerEntityBuilder>? {
    val workspaceModelIndex = WorkspaceFileIndex.getInstance(project)
    val dir = event.newVirtualFile ?: return null
    if (!dir.isValid) return null
    val moduleRoot = generateSequence(dir.parent) { it.parent }.firstNotNullOfOrNull { file ->
      findModuleSourceRoot(workspaceModelIndex, file)
    } ?: return null
    val moduleEntity = moduleRoot.data.module.findModuleEntity(context.workspaceSnapshot) ?: return null
    val basePackagePrefix = (moduleRoot.data as? JvmPackageRootDataInternal)?.packagePrefix ?: return null
    val relativePackagePrefix = VfsUtilCore.getRelativePath(dir, moduleRoot.root, '.') ?: return null
    val packagePrefix = concatenatePackages(basePackagePrefix, relativePackagePrefix)
    return moduleEntity to PackageMarkerEntity(
      root = dir.toVirtualFileUrl(context.urlManager),
      packagePrefix = packagePrefix,
      entitySource = BazelDummyEntitySource,
    )
  }

  private suspend fun findModuleSourceRoot(
    workspaceModelIndex: WorkspaceFileIndex,
    file: VirtualFile,
  ): WorkspaceFileSetWithCustomData<ModuleRelatedRootData>? =
    readAction {
      workspaceModelIndex.findFileSetWithCustomData(
        file = file,
        honorExclusion = true,
        includeContentSets = true,
        includeContentNonIndexableSets = true,
        includeExternalSets = false,
        includeExternalSourceSets = false,
        includeExternalNonIndexableSets = false,
        includeCustomKindSets = false,
        customDataClass = ModuleRelatedRootData::class.java,
      )
    }

  protected data class PathAndVFile(val path: Path, val vFile: VirtualFile)

  private fun SimplifiedFileEvent.toPathAndVFile(): PathAndVFile? {
    return if (fileAdded != null && newVirtualFile != null)
      PathAndVFile(fileAdded, newVirtualFile)
    else
      null
  }

  // return true if was added, false if already present in content roots
  private fun addFileToModule(
    url: VirtualFileUrl,
    entityStorageDiff: MutableEntityStorage,
    module: ModuleEntity
  ): Boolean {
    // we don't want to duplicate source content roots
    if (module.contentRoots.any { contentRoot ->
        contentRoot.sourceRoots.any { sourceRoot ->
          sourceRoot.url.isEqualOrParentOf(url)
        }
      })
      return false

    val sourceRootTypeId = module.bazelModuleExtension?.rootTypeId?.default
                           ?: return false

    entityStorageDiff.modifyModuleEntity(module) {
      if (contentRoots.any { contentRoot ->
          contentRoot.sourceRoots.any { sourceRoot ->
            sourceRoot.url.isEqualOrParentOf(url)
          }
        })
        return@modifyModuleEntity

      contentRoots += ContentRootEntity(
        url = url,
        excludedPatterns = emptyList(),
        entitySource = module.entitySource,
      ) {
        sourceRoots += listOf(
          SourceRootEntity(
            url = url,
            entitySource = module.entitySource,
            rootTypeId = sourceRootTypeId,
          ),
        )
      }
    }
    return true
  }

  private suspend fun List<SimplifiedFileEvent>.filterByProject(): List<SimplifiedFileEvent> {
    if (this.isEmpty() || !project.isBazelProject) return emptyList()
    val rootDirPath =
      try {
        project.rootDir.toNioPath()
      } catch (_: IllegalStateException) { // Bazel rootDir not set
        return emptyList()
      } catch (_: UnsupportedOperationException) { // unable to create a Path instance
        return emptyList()
      }
    val fileIndex = ProjectRootManager.getInstance(project).fileIndex
    val fileSystem = LocalFileSystem.getInstance()
    return readAction {
      filter { it.doesAffectFolder(rootDirPath) && !it.affectsExcludedFiles(fileIndex, fileSystem) }
    }
  }
}

@NlsSafe
private fun getProgressTitle(fileChanges: List<SimplifiedFileEvent>): String =
  fileChanges
    .singleOrNull()
    ?.fileAdded
    ?.let { BazelPluginBundle.message("file.change.processing.title.single", it.name) }
  ?: BazelPluginBundle.message("file.change.processing.title.multiple")

private fun Label.toModuleEntity(storage: ImmutableEntityStorage, project: Project): ModuleEntity? =
  storage.resolve(ModuleId(this.formatAsModuleName(project)))

private suspend fun queryTargetsForFile(project: Project, filePaths: List<Path>, taskId: TaskId): Map<Path, List<Label>>? {
  if (project.serviceAsync<SyncStatusService>().isSyncInProgress)
    return null

  return try {
    project
      .connection
      .runWithServer(taskId) { it.buildTargetInverseSources(InverseSourcesParams(taskId, filePaths)) }
      .targets
  }
  catch (ex: Exception) {
    rethrowControlFlowException(ex)
    logger.debug(ex)
    null
  }
}

// the .toUri() conversion is necessary to contain file:// schema, which is present in VirtualFile.toVirtualFileUrl() results
private fun Path.toVirtualFileUrl(manager: VirtualFileUrlManager): VirtualFileUrl = manager.getOrCreateFromUrl(this.toUri().toString())

private val PROCESSING_DELAY = 250.milliseconds // not noticeable by the user, but if there are many events simultaneously, we will get them all

private val logger = Logger.getInstance(BazelFileEventProcessor::class.java)

private data class ProcessingContext(
  val urlManager: VirtualFileUrlManager,
  val workspaceModel: WorkspaceModel,
  val workspaceSnapshot: ImmutableEntityStorage,
  val entityStorageDiff: MutableEntityStorage,
  val taskId: TaskId,
  val progressReporter: BazelFileEventProgressReporter,
)
