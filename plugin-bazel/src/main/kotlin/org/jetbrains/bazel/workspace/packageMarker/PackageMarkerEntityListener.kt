package org.jetbrains.bazel.workspace.packageMarker

import com.intellij.openapi.project.Project
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
import org.jetbrains.bazel.sdkcompat.findFileSetWithCustomDataCompat
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.BazelDummyEntitySource
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.PackageMarkerEntity
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.packageMarkerEntities
import org.jetbrains.bazel.workspace.getRelatedProjects

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
    workspaceModelIndex
      .findFileSetWithCustomDataCompat(
        file = file,
        honorExclusion = true,
        includeContentSets = true,
        includeExternalSets = false,
        includeExternalSourceSets = false,
        includeCustomKindSets = false,
        customDataClass = ModuleRelatedRootData::class.java,
      )?.data
}
