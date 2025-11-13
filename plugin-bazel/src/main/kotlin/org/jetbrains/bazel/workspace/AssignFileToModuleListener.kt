@file:Suppress("UnstableApiUsage")

package org.jetbrains.bazel.workspace

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ProjectFileIndex
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
import com.intellij.platform.workspace.jps.entities.ModuleDependencyItem
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.platform.workspace.storage.ImmutableEntityStorage
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.intellij.workspaceModel.ide.toPath
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.BazelDummyEntitySource
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.BazelModuleEntitySource
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
import java.nio.file.Path

class AssignFileToModuleListener : BulkFileListener {
  override fun after(events: MutableList<out VFileEvent>) {
    // if the list has multiple events, a resync is required
    val event = events.singleOrNull()
    if (event != null && !ApplicationManager.getApplication().isUnitTestMode) {
      afterSingleFileEvent(event)
    }
  }

  // the returned map is for testing purposes; Project location hash is used instead of Project since it's safer to store
  @VisibleForTesting
  fun afterSingleFileEvent(event: VFileEvent): Map<String, Job> {
    val file = getAffectedFile(event) ?: return emptyMap()
    val isSource =
      if (event is VFileDeleteEvent) {
        file.extension?.let { SourceType.fromExtension(it) } != null
      } else {
        file.isSourceFile()
      }
    return if (isSource) {
      getRelatedProjects(file).associateProjectIdWithJob { processWithDelay(it, event) }
    } else {
      emptyMap()
    }
  }

  private fun processWithDelay(project: Project, event: VFileEvent): Job? {
    val controller = Controller.getInstance(project)
    if (controller.isAnotherProcessingInProgress()) return null
    val eventIsFirstInQueue = controller.addEvent(event)

    // only the first event in the queue should trigger the delayed processing
    return if (eventIsFirstInQueue) {
      BazelCoroutineService.getInstance(project).start {
        delay(PROCESSING_DELAY)

        val workspaceModel = project.serviceAsync<WorkspaceModel>()
        val event = Controller.getInstance(project).getSingleEventOrNullAndClear()
        if (event != null) {
          controller.processWithLock {
            processFileEvent(event = event, project = project, workspaceModel = workspaceModel)
          }
        }
      }
    } else {
      null
    }
  }

  @Service(Service.Level.PROJECT)
  private class Controller {
    private var moreThanOneEvent = false
    private var eventWaiting: VFileEvent? = null

    private val processingLock = Mutex()

    /** @return `true` if this event was the first to be reported in the batch, `false` otherwise */
    fun addEvent(event: VFileEvent): Boolean =
      synchronized(this) {
        if (eventWaiting == null) {
          eventWaiting = event
          true
        } else {
          moreThanOneEvent = true
          false
        }
      }

    fun getSingleEventOrNullAndClear(): VFileEvent? {
      synchronized(this) {
        val singleEvent =
          when {
            moreThanOneEvent -> null
            else -> eventWaiting
          }
        moreThanOneEvent = false
        eventWaiting = null
        return singleEvent
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

  companion object {
    internal class ExternalFileCreateEvent(val createdFile: VirtualFile) : VFileEvent(createdFile.fileSystem) {
      override fun getFile(): VirtualFile = createdFile
      override fun getFileSystem() = createdFile.fileSystem
      override fun getPath(): String = createdFile.path
      override fun computePath(): String = createdFile.path
      override fun isFromRefresh(): Boolean = true // treat as refresh-originated
      override fun isValid(): Boolean = createdFile.isValid
      override fun equals(other: Any?): Boolean = other is ExternalFileCreateEvent && other.createdFile == createdFile
      override fun hashCode(): Int = createdFile.hashCode()
    }

    @JvmStatic
    fun enqueueExternalFile(project: Project, file: VirtualFile) {
      if (file.isDirectory || !file.isSourceFile()) return
      // Reuse the existing single-file event path instead of duplicating queue logic.
      // A fresh listener instance is stateless; queuing & concurrency are handled by Controller (per project service).
      AssignFileToModuleListener().afterSingleFileEvent(ExternalFileCreateEvent(file))
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
      is AssignFileToModuleListener.Companion.ExternalFileCreateEvent -> event.createdFile
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

private suspend fun processFileEvent(
  event: VFileEvent,
  project: Project,
  workspaceModel: WorkspaceModel,
) {
  val entityStorageDiff = MutableEntityStorage.from(workspaceModel.currentSnapshot)

  val newFile = getNewFile(event)
  val oldFilePath = getOldFilePath(event) // the old file must be kept as a path, since this file no longer exists

  withBackgroundProgress(project, event.getProgressMessage(newFile)) {
    reportSequentialProgress { reporter ->
      val contentRootsToRemove =
        oldFilePath?.let {
          val targetUtils = project.serviceAsync<TargetUtils>()
          processFileRemoved(
            oldFilePath = it,
            newFile = newFile,
            project = project,
            workspaceModel = workspaceModel,
            targetUtils = targetUtils,
          )
        }
      val mutableRemovalMap = contentRootsToRemove?.toMutableMap() ?: mutableMapOf()

      reporter.nextStep(PROGRESS_DELETE_STEP_SIZE)
      newFile?.let {
        processFileCreated(
          newFile = it,
          project = project,
          workspaceModel = workspaceModel,
          entityStorageDiff = entityStorageDiff,
          progressReporter = reporter,
          mutableRemovalMap = mutableRemovalMap,
        )
      }

      mutableRemovalMap.values.flatten().forEach { entityStorageDiff.removeEntity(it) }

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
  mutableRemovalMap: MutableMap<ModuleEntity, List<ContentRootEntity>>,
) {
  val existingModules =
    getModulesForFile(newFile, project)
      .filter { it.moduleEntity?.entitySource != BazelDummyEntitySource }
      .mapNotNull { it.moduleEntity }
  val targetUtils = project.targetUtils
  if (existingModules.isNotEmpty() && mutableRemovalMap.isEmpty()) {
    val path = newFile.toNioPathOrNull() ?: return
    val targets =
      existingModules.mapNotNull { module ->
        targetUtils.getTargetForModuleId(module.name)
      }
    targetUtils.addFileToTargetIdEntry(path, targets)
    return
  }

  val url = newFile.toVirtualFileUrl(workspaceModel.getVirtualFileUrlManager())
  val path = url.toPath()
  val targets =
    progressReporter.nextStep(
      endFraction = PROGRESS_QUERY_STEP_SIZE,
      text = BazelPluginBundle.message("file.change.processing.step.query"),
    ) { queryTargetsForFile(project, url) } ?: return
  val processedResult = processTargetsForTestlibStripping(targets)
  val modulesWithTestFlag =
    processedResult.allProcessedTargets
      .mapNotNull { it.toModuleEntity(workspaceModel.currentSnapshot, entityStorageDiff, project) }

  // Precompute a map from module names to labels for efficient lookup
  val moduleNameToLabel = processedResult.allProcessedTargets.associateBy { it.formatAsModuleName(project) }
  for ((module, isTestModule) in modulesWithTestFlag) {
    val moduleLabel = moduleNameToLabel[module.name]
    val isStripped = moduleLabel != null && processedResult.strippedLabels.contains(moduleLabel)

    // if we want a file to be both added and removed in the same module, neither of them will be done
    val moduleContainsContentRootForRemoval = mutableRemovalMap.remove(module) != null
    val alreadyAdded = existingModules.contains(module)
    if (!moduleContainsContentRootForRemoval && !alreadyAdded && !isStripped) {
      // Only add sources to original .testlib targets, not to stripped targets
      url.addToModule(entityStorageDiff, module, newFile.extension, isTestModule)
    }
  }
  // Store only stripped targets (not original .testlib) in target utils mapping
  targetUtils.addFileToTargetIdEntry(path, processedResult.targetsForMapping)
}

suspend fun getModulesForFile(newFile: VirtualFile, project: Project): Set<Module> =
  readAction { ProjectFileIndex.getInstance(project).getModulesForFile(newFile, true) }

private fun processFileRemoved(
  oldFilePath: Path,
  newFile: VirtualFile?,
  project: Project,
  workspaceModel: WorkspaceModel,
  targetUtils: TargetUtils,
): Map<ModuleEntity, List<ContentRootEntity>> {
  val oldUrl = workspaceModel.getVirtualFileUrlManager().fromPath(oldFilePath.toString())
  val newUrl = newFile?.toVirtualFileUrl(workspaceModel.getVirtualFileUrlManager())
  val modules =
    targetUtils
      .getTargetsForPath(oldFilePath)
      .mapNotNull { it.toExistingModuleEntity(workspaceModel.currentSnapshot, project) }
  targetUtils.removeFileToTargetIdEntry(oldFilePath)
  return modules
    .associateWith { module ->
      // IntelliJ might have already changed the content root's path to the new one, so we need to check both
      val newUrlContentRoots = newUrl?.let { findContentRoots(module, it) } ?: emptyList()
      findContentRoots(module, oldUrl) + newUrlContentRoots
    }.filter { it.value.isNotEmpty() }
}

// Helper function for cases where we only want to resolve existing modules, not create new ones
private fun Label.toExistingModuleEntity(storage: ImmutableEntityStorage, project: Project): ModuleEntity? {
  val moduleId = ModuleId(this.formatAsModuleName(project))
  return storage.resolve(moduleId)
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

public suspend fun askForInverseSources(project: Project, fileUrl: VirtualFileUrl): InverseSourcesResult =
  project.connection.runWithServer { bspServer ->
    bspServer
      .buildTargetInverseSources(InverseSourcesParams(TextDocumentIdentifier(fileUrl.toPath())))
  }

// Convert a Label to a ModuleEntity, creating it from partial sync if it doesn't exist
suspend fun Label.toModuleEntity(snapshot: ImmutableEntityStorage, storageUpdates: MutableEntityStorage, project: Project): Pair<ModuleEntity, Boolean>? {
  val moduleId = ModuleId(this.formatAsModuleName(project))

  // First check if module exists in the mutable storage (from previous calls in this batch)
  // note in short, the snapshot + the mutable storage = the current state
  // the storage updates here contains already commited changes that are not pushed into the final module storage
  val existingInStorage = storageUpdates.resolve(moduleId) ?: snapshot.resolve(moduleId)
  var cachedTarget = project.targetUtils.getBuildTargetForLabel(this)
  if (existingInStorage != null) {
    // For existing modules, check if it's a test module from the cached target
    val isTestModule = (cachedTarget?.kind?.ruleType == RuleType.TEST)
    return existingInStorage to isTestModule
  }

  // Try to get build target information from TargetUtils first (for synced targets)
  val dependencies = mutableListOf<ModuleDependencyItem>()

  // Determine module type based on target kind (TEST or JAVA_MODULE for non-test)
  // If target is not in cache, trigger a partial sync to fetch it via UnsyncedTargetUpdater
  if (cachedTarget == null) {
    val result = UnsyncedTargetUpdater.fetchAndCacheUnsyncedTarget(this, project, snapshot, storageUpdates) ?: return null
    cachedTarget = result.first
    dependencies.addAll(result.second)
  }
  val isTestModule = cachedTarget.kind.ruleType == RuleType.TEST
  // Use BazelModuleEntitySource for dynamically created modules
  // Note: We can't use the full JPS entity source logic from ModuleEntityUpdater here because
  // BazelProjectModelExternalSource is not accessible from this package due to module boundaries.
  // Dynamically created modules (added via file listener) should use BazelModuleEntitySource.
  val entitySource = BazelModuleEntitySource(moduleId.name)
  val moduleEntity = ModuleEntity(
    name = moduleId.name,
    dependencies = dependencies,
    entitySource = entitySource,
  )
  val addedEntity = storageUpdates.addEntity(moduleEntity)
  return addedEntity to isTestModule
}

fun VirtualFileUrl.addToModule(
  entityStorageDiff: MutableEntityStorage,
  module: ModuleEntity,
  extension: String?,
  isTestModule: Boolean = false,
) {
  if (module.contentRoots.any { it.url == this }) return // we don't want to duplicate content roots

  // TODO: https://youtrack.jetbrains.com/issue/BAZEL-1917
  val sourceRootType =
    when (extension) {
      "java" -> if (isTestModule) SourceRootTypeId("java-test") else SourceRootTypeId("java-source")
      "kt" -> SourceRootTypeId("kotlin-source") // Kotlin uses same type for test and production
      "py" -> SourceRootTypeId("python-source") // Python uses same type for test and production
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

private fun findContentRoots(module: ModuleEntity, url: VirtualFileUrl): List<ContentRootEntity> =
  module.contentRoots.filter { it.url == url }

private const val PROCESSING_DELAY = 250L // not noticeable by the user, but if there are many events simultaneously, we will get them all
private val logger = Logger.getInstance(AssignFileToModuleListener::class.java)

private const val PROGRESS_DELETE_STEP_SIZE = 20
private const val PROGRESS_QUERY_STEP_SIZE = 80
