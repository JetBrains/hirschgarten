@file:Suppress("UnstableApiUsage")

package org.jetbrains.plugins.bsp.workspace

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.InverseSourcesParams
import ch.epfl.scala.bsp4j.InverseSourcesResult
import ch.epfl.scala.bsp4j.TextDocumentIdentifier
import com.intellij.ide.impl.isTrusted
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.project.ProjectManager
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import org.jetbrains.kotlin.config.KOTLIN_SOURCE_ROOT_TYPE_ID
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.coroutines.BspCoroutineService
import org.jetbrains.plugins.bsp.impl.projectAware.BspSyncStatusService
import org.jetbrains.plugins.bsp.impl.server.connection.connection
import org.jetbrains.plugins.bsp.magicmetamodel.TargetNameReformatProvider
import org.jetbrains.plugins.bsp.magicmetamodel.findNameProvider
import org.jetbrains.plugins.bsp.target.TemporaryTargetUtils
import org.jetbrains.plugins.bsp.target.temporaryTargetUtils
import org.jetbrains.plugins.bsp.utils.isSourceFile

class AssignFileToModuleListener : BulkFileListener {
  private val pendingEvents = mutableMapOf<Project, MutableList<VFileEvent>>()

  override fun after(events: MutableList<out VFileEvent>) {
    // if the list has multiple events, it means an external operation (like Git) and resync is probably required anyway
    events.singleOrNull()?.let {
      val file = it.getAffectedFile()
      if (file?.isSourceFile() == true) {
        file.getRelatedProjects().forEach { project -> project.processWithDelay(it) }
      }
    }
  }

  private fun Project.processWithDelay(event: VFileEvent) {
    synchronized(pendingEvents) {
      pendingEvents[this]?.let {
        it.add(event)
        return
      } ?: pendingEvents.put(this, mutableListOf(event))
    }
    BspCoroutineService.getInstance(this).start {
      delay(PROCESSING_DELAY)
      synchronized(pendingEvents) { pendingEvents.remove(this)?.singleOrNull() }?.process(this)
    }
  }
}

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

private fun VirtualFile.getRelatedProjects(): List<Project> {
  val projectManager = ProjectManager.getInstance()
  val projectLocator = ProjectLocator.getInstance()
  return if (this.isValid) {
    projectLocator.getProjectsForFile(this).filterNotNull().filter { it.doWeCareAboutIt() }
  } else {
    projectManager.openProjects.filter { it.doWeCareAboutIt() } // the project locator would return an empty list
  }
}

private fun Project.doWeCareAboutIt(): Boolean = this.isBspProject && this.isTrusted()

private fun VFileEvent.process(project: Project) {
  val workspaceModel = WorkspaceModel.getInstance(project)
  val storage = workspaceModel.currentSnapshot
  val moduleNameProvider = project.findNameProvider() ?: return
  val file = this.getAffectedFile() ?: return
  val targetUtils = project.temporaryTargetUtils
  runInBackgroundWithProgress(project, file.name) {
    when (this) {
      is VFileCreateEvent ->
        processFileCreated(file, project, workspaceModel, targetUtils, storage, moduleNameProvider)
      is VFileCopyEvent ->
        processFileCreated(file, project, workspaceModel, targetUtils, storage, moduleNameProvider)
      is VFileDeleteEvent ->
        processFileRemoved(file, project, workspaceModel, targetUtils, storage, moduleNameProvider)
      is VFileMoveEvent ->
        processFileMoved(file, project, workspaceModel, targetUtils, storage, moduleNameProvider)
      is VFilePropertyChangeEvent ->
        processFileMoved(file, project, workspaceModel, targetUtils, storage, moduleNameProvider)
    }
  }
}

private fun runInBackgroundWithProgress(
  project: Project,
  fileName: String,
  action: suspend () -> Unit,
) {
  BspCoroutineService.getInstance(project).start {
    withBackgroundProgress(project, BspPluginBundle.message("file.change.processing.title", fileName)) {
      action()
    }
  }
}

private suspend fun processFileCreated(
  file: VirtualFile,
  project: Project,
  workspaceModel: WorkspaceModel,
  targetUtils: TemporaryTargetUtils,
  storage: ImmutableEntityStorage,
  moduleNameProvider: TargetNameReformatProvider,
) {
  val url = file.toVirtualFileUrl(workspaceModel.getVirtualFileUrlManager())
  getTargetsForFile(project, url)
    ?.mapNotNull { it.toModuleEntity(storage, moduleNameProvider, targetUtils) }
    ?.forEach { url.addToModule(workspaceModel, it, file.extension) }
}

private suspend fun processFileRemoved(
  file: VirtualFile,
  project: Project,
  workspaceModel: WorkspaceModel,
  targetUtils: TemporaryTargetUtils,
  storage: ImmutableEntityStorage,
  moduleNameProvider: TargetNameReformatProvider,
) {
  val url = file.toVirtualFileUrl(workspaceModel.getVirtualFileUrlManager())
  targetUtils
    .getTargetsForFile(file, project)
    .mapNotNull { it.toModuleEntity(storage, moduleNameProvider, targetUtils) }
    .forEach {
      url.removeFromModule(workspaceModel, it)
    }
}

private suspend fun processFileMoved(
  file: VirtualFile,
  project: Project,
  workspaceModel: WorkspaceModel,
  targetUtils: TemporaryTargetUtils,
  storage: ImmutableEntityStorage,
  moduleNameProvider: TargetNameReformatProvider,
) {
  val url = file.toVirtualFileUrl(workspaceModel.getVirtualFileUrlManager())
  val inverseSourcesResult = getTargetsForFile(project, url) ?: return
  val applicableModules =
    inverseSourcesResult
      .mapNotNull { it.toModuleEntity(storage, moduleNameProvider, targetUtils) }
      .toSet()

  applicableModules.forEach { url.addToModule(workspaceModel, it, file.extension) }
}

private suspend fun getTargetsForFile(project: Project, fileUrl: VirtualFileUrl): List<BuildTargetIdentifier>? =
  if (!BspSyncStatusService.getInstance(project).isSyncInProgress) {
    try {
      askForInverseSources(project, fileUrl)
        ?.targets
        ?.toList()
    } catch (ex: Exception) {
      logger.debug(ex)
      null
    }
  } else {
    null
  }

private suspend fun askForInverseSources(project: Project, fileUrl: VirtualFileUrl): InverseSourcesResult? =
  project.connection.runWithServer { bspServer, bazelBuildServerCapabilities ->
    if (bazelBuildServerCapabilities.inverseSourcesProvider) {
      bspServer
        .buildTargetInverseSources(InverseSourcesParams(TextDocumentIdentifier(fileUrl.url)))
        .await()
    } else {
      null
    }
  }

private fun BuildTargetIdentifier.toModuleEntity(
  storage: ImmutableEntityStorage,
  moduleNameProvider: TargetNameReformatProvider,
  targetUtils: TemporaryTargetUtils,
): ModuleEntity? {
  val targetInfo = targetUtils.getBuildTargetInfoForId(this) ?: return null
  val moduleName = moduleNameProvider(targetInfo)
  val moduleId = ModuleId(moduleName)
  return storage.resolve(moduleId)
}

private suspend fun VirtualFileUrl.addToModule(
  workspaceModel: WorkspaceModel,
  module: ModuleEntity,
  extension: String?,
) {
  if (module.contentRoots.any { it.url == this }) return // we don't want to duplicate content roots

  val sourceRootType =
    when (extension) {
      "java" -> JAVA_SOURCE_ROOT_ENTITY_TYPE_ID
      "kt" -> SourceRootTypeId(KOTLIN_SOURCE_ROOT_TYPE_ID)
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
  writeAction {
    workspaceModel.updateProjectModel("File changes processing") {
      it.modifyModuleEntity(module) {
        contentRoots = updater(contentRoots)
      }
    }
  }
}

private const val PROCESSING_DELAY = 250L // no noticeable by the user, but if there are many events simultaneously, we will get them all
private val logger = Logger.getInstance(AssignFileToModuleListener::class.java)
