package org.jetbrains.bazel.workspace.fileEvents

import com.android.adblib.utils.launchCancellable
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.build.events.impl.SkippedResultImpl
import com.intellij.build.events.impl.SuccessResultImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListenerBackgroundable
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.platform.workspace.storage.ImmutableEntityStorage
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import com.intellij.workspaceModel.ide.toPath
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
import org.jetbrains.bsp.protocol.InverseSourcesParams
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.extension
import kotlin.io.path.name

@Suppress("UnstableApiUsage")
class BazelFileEventListener : BulkFileListenerBackgroundable {
  override fun after(events: MutableList<out VFileEvent>) {
    // unit tests trigger file event listeners normally, making them hard to test unless that is disabled
    if (!ApplicationManager.getApplication().isUnitTestMode) {
      process(events)
    }
  }

  // the returned map is for testing purposes; Project location hash is used instead of Project since it's safer to store
  @VisibleForTesting
  fun process(events: List<VFileEvent>): Map<String, Deferred<Boolean>> {
    val projects =
      ProjectManager
        .getInstance()
        .openProjects // ProjectLocator::getProjectsForFile won't work, since it only recognizes files already added to content roots
        .filter { it.isBazelProject && it.hasAnyTargets() }
    if (projects.isEmpty())
      return emptyMap()

    return projects.associateWith { project ->
      BazelCoroutineService.getInstance(project).startAsync {
        val simplifiedEvents = events.mapNotNull { SimplifiedFileEvent.from(it) }
        processEventsForProject(project, simplifiedEvents)
      }
    }.mapKeys { it.key.locationHash }
  }

  /** @return `true` if processing has been performed in this function execution, `false` if it was omitted for any reason */
  private suspend fun processEventsForProject(project: Project, events: List<SimplifiedFileEvent>): Boolean {
    val applicableEvents = events.filterByProject(project).takeIf { it.isNotEmpty() } ?: return false
    val queueController = FileEventQueueController.getInstance(project)
    val shouldStartProcessing = queueController.addEvents(applicableEvents)
    if (!shouldStartProcessing)
      return false

    val originId = "file-event-" + UUID.randomUUID().toString()
    val processingJob = BazelCoroutineService.getInstance(project).startAsync(true) {
      delay(PROCESSING_DELAY)
      do {
        val processed = queueController.withNextBatch { batch ->
          try {
            processEventQueue(project, batch, originId)
          } catch (ex: Throwable) {
            if (ex !is CancellationException)
              logger.error(ex)
            throw ex
          }
        }
      } while (processed)
    }

    startSyncConsoleTask(project, processingJob, originId)
    processingJob.join()
    return true
  }

  private fun startSyncConsoleTask(project: Project, processingJob: Job, originId: String) {
    project.syncConsole.startTask(
      taskId = originId,
      title = BazelPluginBundle.message("file.change.processing.title.multiple"),
      message = BazelPluginBundle.message("file.change.processing.message.start"),
      showConsole = TaskConsole.ShowConsole.ON_FAIL,
      cancelAction = { processingJob.cancel() },
    )
    processingJob.invokeOnCompletion { exception ->
      val (message, result) = if (exception == null) {
        BazelPluginBundle.message("file.change.processing.message.finish") to SuccessResultImpl()
      } else if (exception is CancellationException) {
        BazelPluginBundle.message("file.change.processing.message.cancelled") to SkippedResultImpl()
      } else {
        BazelPluginBundle.message("file.change.processing.message.failed") to FailureResultImpl(exception)
      }
      project.syncConsole.finishTask(originId, message, result)
    }
  }

  private suspend fun processEventQueue(project: Project, events: List<SimplifiedFileEvent>, originId: String) {
    val progressTitle = getProgressTitle(events)
    BazelFileEventProgressReporter.runWithProgressBar(progressTitle, project) { bazelReporter ->
      applyAllChanges(
        allFileEvents = events,
        context = prepareProcessingContext(project, originId, bazelReporter)
      )
    }
  }

  private suspend fun applyAllChanges(allFileEvents: List<SimplifiedFileEvent>, context: ProcessingContext) {
    context.progressReporter.startPreparationStep()
    removeOldFilesFromBothModels(allFileEvents, context)
    replaceFilesInPluginModel(allFileEvents, context.targetUtils)

    val mutableRemovalMap = findNewFilesInWorkspaceModel(allFileEvents, context)
    addNewFilesToBothModels(allFileEvents, mutableRemovalMap, context)
    applyRemovalMap(mutableRemovalMap, context)

    context.progressReporter.startFinalisingStep()
    context.workspaceModel.update("File event processing (Bazel)") {
      it.applyChangesFrom(context.entityStorageDiff)
    }
  }

  private fun removeOldFilesFromBothModels(
    allFileEvents: List<SimplifiedFileEvent>,
    context: ProcessingContext,
  ) {
    val applicableEvents = allFileEvents.filter {
      it is SimplifiedFileEvent.Delete ||
      it is SimplifiedFileEvent.Move ||
      (it as? SimplifiedFileEvent.Rename)?.extensionChanged == true
    }
    val pathsToRemove = applicableEvents.mapNotNull { it.fileRemoved }
    val targetUtils = context.targetUtils

    for (path in pathsToRemove) {
      val fileUrl = path.toVirtualFileUrl(context.urlManager)
      val modules =
        targetUtils
          .getTargetsForPath(path)
          .mapNotNull { it.toModuleEntity(context.workspaceSnapshot, context.project) }
      for (module in modules) {
        val contentRoot = module.contentRoots.find { it.url == fileUrl }
        contentRoot?.let { context.entityStorageDiff.removeEntity(it) }
      }
      targetUtils.removeFileToTargetIdEntry(path)
    }
  }

  /** In all targets containing the old file, replace it with the new one */
  private fun replaceFilesInPluginModel(
    allFileEvents: List<SimplifiedFileEvent>,
    targetUtils: TargetUtils,
  ) {
    val applicableEvents = allFileEvents.filter { it is SimplifiedFileEvent.Rename && !it.extensionChanged }
    for (event in applicableEvents) {
      if (event.fileRemoved == null || event.fileAdded == null) {
        continue
      }
      val targets = targetUtils.getTargetsForPath(event.fileRemoved)
      targetUtils.removeFileToTargetIdEntry(event.fileRemoved)
      targetUtils.addFileToTargetIdEntry(event.fileAdded, targets)
    }
  }

  private suspend fun findNewFilesInWorkspaceModel(
    allFileEvents: List<SimplifiedFileEvent>,
    context: ProcessingContext,
  ): Map<Path, MutableSet<ModuleEntity>> {
    val applicableEvents =
      allFileEvents.filter { it is SimplifiedFileEvent.Move || (it as? SimplifiedFileEvent.Rename)?.extensionChanged == true }
    return applicableEvents
      .mapNotNull { (it.fileAdded ?: return@mapNotNull null) to (it.newVirtualFile ?: return@mapNotNull null) }
      .toMap()
      .mapValues { findModulesForFile(it.value, context.fileIndex).toMutableSet() }
  }

  private suspend fun addNewFilesToBothModels(
    allFileEvents: List<SimplifiedFileEvent>,
    moduleToRemoveFilesFrom: Map<Path, MutableSet<ModuleEntity>>,
    context: ProcessingContext,
  ) {
    val applicableEvents = allFileEvents.filter {
      it is SimplifiedFileEvent.Create ||
      it is SimplifiedFileEvent.Move ||
      it is SimplifiedFileEvent.Copy ||
      (it as? SimplifiedFileEvent.Rename)?.extensionChanged == true
    }
    val modulesAlreadyContainingFiles =
      applicableEvents
        .mapNotNull { event ->
          event.newVirtualFile?.let { (event.fileAdded ?: return@mapNotNull null) to findModulesForFile(it, context.fileIndex) }
        }.toMap()
    val addedFilePaths = applicableEvents.mapNotNull { it.fileAdded }
    val bazelQueryIsRequired =
      addedFilePaths.any {
        moduleToRemoveFilesFrom[it]?.isNotEmpty() == true || modulesAlreadyContainingFiles[it].isNullOrEmpty()
      }
    // avoid running a Bazel query when not required (BAZEL-2458)
    if (bazelQueryIsRequired) {
      val targetsByPath =
        context.progressReporter.startQueryStep { queryTargetsForFile(context.project, addedFilePaths, context.originId) } ?: return
      addFileToTargets(targetsByPath, moduleToRemoveFilesFrom, modulesAlreadyContainingFiles, context)
    } else {
      context.progressReporter.skipQueryStep()
      for (filePath in addedFilePaths) {
        val modules = modulesAlreadyContainingFiles[filePath] ?: continue
        addToPluginModelByModules(filePath, modules, context.targetUtils)
      }
    }
  }

  private fun addFileToTargets(
    targetsByPath: Map<Path, List<Label>>,
    moduleToRemoveFilesFrom: Map<Path, MutableSet<ModuleEntity>>,
    modulesAlreadyContainingFiles: Map<Path, Set<ModuleEntity>>,
    context: ProcessingContext,
  ) {
    for ((filePath, targets) in targetsByPath.entries) {
      val fileUrl = filePath.toVirtualFileUrl(context.urlManager)
      val moduleRemovalsForFile = moduleToRemoveFilesFrom[filePath]
      val modulesAlreadyContainingFile =
        modulesAlreadyContainingFiles.getOrDefault(filePath, emptySet())
      val modulesToAddTo = targets.mapNotNull { it.toModuleEntity(context.workspaceSnapshot, context.project) }
      for (module in modulesToAddTo) {
        // if we want a file to be both added and removed in the same module, neither of them will be done
        val moduleContainsContentRootForRemoval = moduleRemovalsForFile?.remove(module) == true
        val fileAlreadyInModule = modulesAlreadyContainingFile.contains(module)
        if (!moduleContainsContentRootForRemoval && !fileAlreadyInModule) {
          fileUrl.addToModule(context.entityStorageDiff, module, filePath.extension)
        }
      }
      context.targetUtils.addFileToTargetIdEntry(filePath, targets)
    }
  }

  private fun applyRemovalMap(removalMap: Map<Path, MutableSet<ModuleEntity>>, context: ProcessingContext) {
    val filePathsByModule = mutableMapOf<ModuleEntity, MutableSet<Path>>()
    for ((filePath, modules) in removalMap) {
      modules.forEach { module -> filePathsByModule.getOrPut(module) { mutableSetOf() }.add(filePath) }
    }
    for ((module, filePaths) in filePathsByModule) {
      val contentRootsToRemove = module.contentRoots.filter { it.url.toPath() in filePaths }
      contentRootsToRemove.forEach { context.entityStorageDiff.removeEntity(it) }
    }
  }
}

// if a project has no targets, there is no point in processing (also, it could interrupt the initial sync)
private fun Project.hasAnyTargets(): Boolean = this.targetUtils.allTargets().any()

private fun List<SimplifiedFileEvent>.filterByProject(project: Project): List<SimplifiedFileEvent> {
  if (this.isEmpty() || !project.isBazelProject) return emptyList()
  val rootDirPath =
    try {
      project.rootDir.toNioPath()
    } catch (_: IllegalStateException) { // Bazel rootDir not set
      return emptyList()
    } catch (_: UnsupportedOperationException) { // unable to create a Path instance
      return emptyList()
    }
  return filter { it.doesAffectFolder(rootDirPath) }
}

@NlsSafe
private fun getProgressTitle(fileChanges: List<SimplifiedFileEvent>): String =
  fileChanges
    .singleOrNull()
    ?.fileAdded
    ?.let { BazelPluginBundle.message("file.change.processing.title.single", it.name) }
    ?: BazelPluginBundle.message("file.change.processing.title.multiple")

private suspend fun prepareProcessingContext(
  project: Project,
  originId: String,
  bazelReporter: BazelFileEventProgressReporter,
): ProcessingContext {
  val workspaceModel = project.serviceAsync<WorkspaceModel>()
  val entityStorageDiff = MutableEntityStorage.from(workspaceModel.currentSnapshot)
  return ProcessingContext(
    project = project,
    urlManager = workspaceModel.getVirtualFileUrlManager(),
    workspaceModel = workspaceModel,
    workspaceSnapshot = workspaceModel.currentSnapshot,
    entityStorageDiff = entityStorageDiff,
    targetUtils = project.serviceAsync<TargetUtils>(),
    originId = originId,
    fileIndex = ProjectRootManager.getInstance(project).fileIndex,
    progressReporter = bazelReporter,
  )
}

private fun Label.toModuleEntity(storage: ImmutableEntityStorage, project: Project): ModuleEntity? =
  storage.resolve(ModuleId(this.formatAsModuleName(project)))

@Suppress("UnstableApiUsage")
private suspend fun findModulesForFile(newFile: VirtualFile, fileIndex: ProjectFileIndex): Set<ModuleEntity> {
  val modules = readAction { fileIndex.getModulesForFile(newFile, true) }
  return modules
    .mapNotNull { it.moduleEntity }
    .toSet()
}

private suspend fun queryTargetsForFile(project: Project, filePaths: List<Path>, originId: String): Map<Path, List<Label>>? =
  if (!project.serviceAsync<SyncStatusService>().isSyncInProgress) {
    try {
      project
        .connection
        .runWithServer { it.buildTargetInverseSources(InverseSourcesParams(originId, filePaths)) }
        .targets
    } catch (ex: Exception) {
      logger.debug(ex)
      null
    }
  } else {
    null
  }

private fun addToPluginModelByModules(filePath: Path, modules: Set<ModuleEntity>, targetUtils: TargetUtils) {
  val targets = modules.mapNotNull { targetUtils.getTargetForModuleId(it.name) }
  if (targets.isNotEmpty()) {
    targetUtils.addFileToTargetIdEntry(filePath, targets)
  }
}

// the .toUri() conversion is necessary to contain file:// schema, which is present in VirtualFile.toVirtualFileUrl() results
private fun Path.toVirtualFileUrl(manager: VirtualFileUrlManager): VirtualFileUrl = manager.getOrCreateFromUrl(this.toUri().toString())

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

private const val PROCESSING_DELAY = 250L // not noticeable by the user, but if there are many events simultaneously, we will get them all

private val logger = Logger.getInstance(BazelFileEventListener::class.java)

private data class ProcessingContext(
  val project: Project,
  val urlManager: VirtualFileUrlManager,
  val workspaceModel: WorkspaceModel,
  val workspaceSnapshot: ImmutableEntityStorage,
  val entityStorageDiff: MutableEntityStorage,
  val targetUtils: TargetUtils,
  val originId: String,
  val fileIndex: ProjectFileIndex,
  val progressReporter: BazelFileEventProgressReporter,
)
