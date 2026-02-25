@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.jetbrains.bazel.workspace.packageMarker

import com.intellij.java.workspace.entities.javaSettings
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import com.intellij.platform.workspace.storage.EntityStorage
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileIndexContributor
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileKind
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileSetRegistrar
import com.intellij.workspaceModel.core.fileIndex.impl.ModuleSourceRootData
import com.intellij.workspaceModel.ide.legacyBridge.findModule
import org.jetbrains.bazel.workspacemodel.entities.PackageMarkerEntity

private class PackageMarkerEntityWorkspaceFileIndexContributor : WorkspaceFileIndexContributor<PackageMarkerEntity> {
  override val entityClass: Class<PackageMarkerEntity> = PackageMarkerEntity::class.java

  override fun registerFileSets(
    entity: PackageMarkerEntity,
    registrar: WorkspaceFileSetRegistrar,
    storage: EntityStorage,
  ) {
    val moduleEntity = entity.module
    val module = moduleEntity.findModule(storage) ?: return
    registrar.registerNonRecursiveFileSet(
      file = entity.root,
      kind = WorkspaceFileKind.CONTENT,
      entity = entity,
      customData =
        ModuleSourceRootData(
          module,
          entity.root.virtualFile,
          SourceRootTypeId("java-source"),
          entity.packagePrefix,
          false,
          languageLevelId = moduleEntity.javaSettings?.languageLevelId,
        ),
    )
  }
}
