package org.jetbrains.bazel.workspace.fileEvents

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.build.events.impl.SkippedResultImpl
import com.intellij.build.events.impl.SuccessResultImpl
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.Logger
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
import kotlinx.coroutines.Job
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
import org.jetbrains.bazel.ui.status.refreshAllFilesPresentation
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
import kotlin.io.path.name
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

@ApiStatus.Internal
interface BazelFileEventProcessor {
  /**
   * Return true if events were processed
   */
  suspend fun process(events: List<VFileEvent>): Boolean
}

@ApiStatus.Internal
open class DefaultBazelFileEventProcessor(private val project: Project): BazelFileEventProcessor {
  private val targetUtils = project.targetUtils

  override suspend fun process(events: List<VFileEvent>): Boolean {
    // if a project has no targets, there is no point in processing (also, it could interrupt the initial sync)
    targetUtils.awaitLoaded()
    if (!targetUtils.allTargets().any())
      return false

    val simplifiedEvents = events.mapNotNull { SimplifiedFileEvent.from(it) }.filter { it.shouldBeProcessed(project) }
    return processEvents(simplifiedEvents)
  }

  private val allowBazelQuery: Boolean get() = BazelFeatureFlags.queryBazelOnFileEvents
  private val allowPsiEvaluation: Boolean get() = BazelFeatureFlags.evaluatePsiOnFileEvents

  /** @return `true` if processing has been performed in this function execution, `false` if it was omitted for any reason */
  private suspend fun processEvents(events: List<SimplifiedFileEvent>): Boolean {
    val applicableEvents = events.filterByProject(project).takeIf { it.isNotEmpty() } ?: return false
    val queueController = FileEventQueueController.getInstance(project)
    val shouldStartProcessing = queueController.addEvents(applicableEvents)
    if (!shouldStartProcessing)
      return false

    val jobManager = FileEventJobManager.getInstance(project)
    val taskGroupId = jobManager.syncTaskGroupId ?: TaskGroupId("file-event-" + Random.nextBytes(8).toHexString())
    val taskId = taskGroupId.task("file-event-processing")
    val processingJob = jobManager.runFileEventsProcessing {
      BazelCoroutineService.getInstance(project).startAsync(true) {
        delay(PROCESSING_DELAY)
        try {
          do {
            val processed = queueController.withNextBatch { batch ->
              try {
                val progressTitle = getProgressTitle(batch)

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

                  doProcessEvents(
                    events = batch,
                    context = context
                  )
                }
              }
              catch (ex: Throwable) {
                if (ex !is CancellationException)
                  logger.error(ex)
                throw ex
              }
            }
          } while (processed)
        } finally {
          refreshAllFilesPresentation(project)
        }
      }
    }

    if (processingJob == null) {
      queueController.clearAllEvents()
      return false
    }

    // no need to start sync console task if no Bazel processing is allowed
    if (allowBazelQuery) {
      startSyncConsoleTask(project, processingJob, taskId)
    }
    try {
      processingJob.join()
    } finally {
      BazelTaskEventsService.getInstance(project).removeListener(taskId.taskGroupId)
    }
    return true
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

  private suspend fun doProcessEvents(events: List<SimplifiedFileEvent>, context: ProcessingContext) {
    doProcessFileEvents(
      events.flatMap { event ->
        if (event is CreateDirectory) {
          collectCreatedFiles(event.newVirtualFile)
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
      if (root.isValid) throw ex
      logger.debug("Skipping stale created directory event for ${root.path}", ex)
      return emptyList()
    }
    return filesInDirectory
  }

  private suspend fun doProcessFileEvents(events: List<SimplifiedFileEvent>, context: ProcessingContext) {
    if (events.isEmpty())
      return

    context.progressReporter.startPreparationStep()

    val eventToOldTargets: Map<SimplifiedFileEvent, List<Label>> =
      events.associateWithIfNotNull { event ->
        event.fileRemoved?.let { fileRemoved -> targetUtils.getTargetsForPath(fileRemoved) }?.takeIf { it.isNotEmpty() }
      }

    val eventsToNewTargets: Map<SimplifiedFileEvent, List<Label>> =
      events
        .mapNotNull { event -> event.toPathAndVFile()?.takeIf { !it.vFile.isDirectory } }
        .let { files ->
          // avoid running a Bazel query when not required (BAZEL-2458)
          val targetsByPath: Map<Path, List<Label>> = if (files.isNotEmpty()) {
            context.progressReporter.startQueryStep {
              invertedSourcesQuery(context.taskId, files)
            }
          } else {
            context.progressReporter.skipQueryStep()
            emptyMap()
          }
          events.associateWithIfNotNull { event -> targetsByPath[event.fileAdded]?.takeIf { it.isNotEmpty() } }
        }

    for (event in events) {
      val removedFile = event.fileRemoved
      val addedFile = event.fileAdded

      val oldTargets: Set<Label> = eventToOldTargets[event]?.toSet() ?: emptySet()
      val newTargets: Set<Label> = eventsToNewTargets[event]?.toSet() ?: emptySet()

      // Update target utils
      if (removedFile != null) {
        targetUtils.removeFileToTargetIdEntry(removedFile)
      }
      if (addedFile != null && newTargets.isNotEmpty()) {
        targetUtils.addFileToTargetIdEntry(addedFile, newTargets)
      }

      // Update WSM
      val removedFileUrl = removedFile?.toVirtualFileUrl(context.urlManager)
      val addedFileUrl = addedFile?.toVirtualFileUrl(context.urlManager)

      (oldTargets - newTargets).forEach { toRemove ->
        val module = toRemove.toModuleEntity(context.workspaceSnapshot, project)
        if (module != null) {
          val contentRoots = module.contentRoots.filter { it.url == removedFileUrl || it.url == addedFileUrl }
          contentRoots.forEach { context.entityStorageDiff.removeEntity(it) }
        }
      }
      if (addedFileUrl != null) {
        (newTargets - oldTargets).forEach { toAdd ->
          val module = toAdd.toModuleEntity(context.workspaceSnapshot, project)
          if (module != null) {
            addFileToModule(project, addedFileUrl, context.entityStorageDiff, module)
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
}

private suspend fun List<SimplifiedFileEvent>.filterByProject(project: Project): List<SimplifiedFileEvent> {
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
      .runWithServer { it.buildTargetInverseSources(InverseSourcesParams(taskId, filePaths)) }
      .targets
  }
  catch (ex: Exception) {
    logger.debug(ex)
    null
  }
}

// the .toUri() conversion is necessary to contain file:// schema, which is present in VirtualFile.toVirtualFileUrl() results
private fun Path.toVirtualFileUrl(manager: VirtualFileUrlManager): VirtualFileUrl = manager.getOrCreateFromUrl(this.toUri().toString())

// return true if was added, false if already present in content roots
private fun addFileToModule(
  project: Project,
  url: VirtualFileUrl,
  entityStorageDiff: MutableEntityStorage,
  module: ModuleEntity
): Boolean {
  // we don't want to duplicate source content roots
  val existingContentRoot = module.contentRoots.find { contentRoot ->
    contentRoot.sourceRoots.any { sourceRoot ->
      sourceRoot.url.isEqualOrParentOf(url)
    }
  }
  if (existingContentRoot != null)
    return false

  val sourceRootTypeId = module.bazelModuleExtension?.rootTypeId?.default
                         ?: return false
  val sourceRoot =
    SourceRootEntity(
      url = url,
      entitySource = module.entitySource,
      rootTypeId = sourceRootTypeId,
    )

  val contentRootEntity =
    ContentRootEntity(
      url = url,
      excludedPatterns = emptyList(),
      entitySource = module.entitySource,
    ) {
      sourceRoots += listOf(sourceRoot)
    }

  entityStorageDiff.modifyModuleEntity(module) { contentRoots += contentRootEntity }
  return true
}

private inline fun <K, V> Collection<K>.associateWithIfNotNull(valueSelector: (K) -> V?): Map<K, V> {
  val result = HashMap<K, V>(this.size)
  for (k in this) {
    val v = valueSelector(k) ?: continue
    result[k] = v
  }
  return result
}

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
