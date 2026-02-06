package org.jetbrains.bazel.workspace.packageMarker

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileIndex
import com.intellij.workspaceModel.core.fileIndex.impl.JvmPackageRootDataInternal
import com.intellij.workspaceModel.core.fileIndex.impl.ModuleRelatedRootData
import com.intellij.workspaceModel.ide.legacyBridge.findModuleEntity
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import com.intellij.openapi.components.serviceIfCreated
import org.jetbrains.bazel.target.TargetUtils
import org.jetbrains.bazel.workspacemodel.entities.BazelDummyEntitySource
import org.jetbrains.bazel.workspacemodel.entities.PackageMarkerEntity
import org.jetbrains.bazel.workspacemodel.entities.packageMarkerEntities

private class PackageMarkerEntityListener : BulkFileListener {
  override fun after(events: List<VFileEvent>) {
    if (!BazelFeatureFlags.fbsrSupportedInPlatform) return
    for (event in events) {
      processEvent(event)
    }
  }

  private fun processEvent(event: VFileEvent) {
    when (event) {
      is VFileCreateEvent -> {
        if (!event.isDirectory) return
        val relatedProjects = getRelatedProjects(event.parent)
        for (project in relatedProjects) {
          processEvent(event, project)
        }
      }
      // TODO: handle move
    }
  }

  private fun processEvent(event: VFileCreateEvent, project: Project) {
    val workspaceModelIndex = WorkspaceFileIndex.getInstance(project)
    if (event.file?.let { file -> findModuleSourceRootData(workspaceModelIndex, file) } != null) {
      // Already belongs to a module, nothing to do
      return
    }
    val moduleRootData = findModuleSourceRootData(workspaceModelIndex, event.parent) as? JvmPackageRootDataInternal ?: return
    val workspaceModel = project.workspaceModel
    // Update things synchronously as soon as the virtual file is created to be sure that refactorings don't break
    @Suppress("UsagesOfObsoleteApi")
    workspaceModel.updateProjectModel("Add PackageMarkerEntity") { storage ->
      val moduleEntity = (moduleRootData as ModuleRelatedRootData).module.findModuleEntity(storage) ?: return@updateProjectModel
      storage.modifyModuleEntity(moduleEntity) {
        this.packageMarkerEntities +=
          PackageMarkerEntity(
            root = workspaceModel.getVirtualFileUrlManager().getOrCreateFromUrl(event.parent.url + '/' + event.childName),
            packagePrefix = concatenatePackages(moduleRootData.packagePrefix, event.childName),
            entitySource = BazelDummyEntitySource,
          )
      }
    }
  }

  private fun findModuleSourceRootData(workspaceModelIndex: WorkspaceFileIndex, file: VirtualFile): ModuleRelatedRootData? =
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
    )?.data
}

fun getRelatedProjects(file: VirtualFile): List<Project> =
  ProjectManager // ProjectLocator::getProjectsForFile only recognizes files already added to content roots
    .getInstance()
    .openProjects
    .filter { projectIsBazelAndContainsFile(it, file) }

private fun projectIsBazelAndContainsFile(project: Project, file: VirtualFile): Boolean {
  val rootDir =
    try {
      project.rootDir
    } catch (_: IllegalStateException) {
      return false
    }
  return project.isBazelProject && VfsUtil.isAncestor(rootDir, file, false) && project.hasAnyTargets()
}

private fun Project.hasAnyTargets(): Boolean =
  this.serviceIfCreated<TargetUtils>()?.allTargets()?.any() == true
