@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.jetbrains.bazel.workspace.packageMarker

import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import com.intellij.platform.workspace.storage.EntityStorage
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileIndexContributor
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileKind
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileSetRegistrar
import com.intellij.workspaceModel.core.fileIndex.impl.ModuleSourceRootData
import com.intellij.workspaceModel.ide.legacyBridge.findModule
import org.jetbrains.bazel.workspacemodel.entities.PackageMarkerEntity

private fun createModuleSourceRootData(
  module: Module,
  virtualFile: VirtualFile?,
  rootTypeId: SourceRootTypeId,
  packagePrefix: String,
  forGeneratedSources: Boolean,
): ModuleSourceRootData {
  // The constructor gained a 6th parameter in newer platform builds.
  // Use reflection to handle both 5-param and 6-param versions.

  val clazz = ModuleSourceRootData::class.java
  return try {
    val ctor = clazz.constructors.maxByOrNull { it.parameterCount }!!
    if (ctor.parameterCount == 6) {
      ctor.newInstance(module, virtualFile, rootTypeId, packagePrefix, forGeneratedSources, null) as ModuleSourceRootData
    } else {
      ctor.newInstance(module, virtualFile, rootTypeId, packagePrefix, forGeneratedSources) as ModuleSourceRootData
    }
  } catch (_: Exception) {
    ModuleSourceRootData(module, virtualFile, rootTypeId, packagePrefix, forGeneratedSources)
  }
}

private class PackageMarkerEntityWorkspaceFileIndexContributor : WorkspaceFileIndexContributor<PackageMarkerEntity> {
  override val entityClass: Class<PackageMarkerEntity> = PackageMarkerEntity::class.java

  override fun registerFileSets(
    entity: PackageMarkerEntity,
    registrar: WorkspaceFileSetRegistrar,
    storage: EntityStorage,
  ) {
    val module = entity.module.findModule(storage) ?: return
    registrar.registerNonRecursiveFileSet(
      file = entity.root,
      kind = WorkspaceFileKind.CONTENT,
      entity = entity,
      customData =
        createModuleSourceRootData(
          module,
          entity.root.virtualFile,
          SourceRootTypeId("java-source"),
          entity.packagePrefix,
          false,
        ),
    )
  }
}
