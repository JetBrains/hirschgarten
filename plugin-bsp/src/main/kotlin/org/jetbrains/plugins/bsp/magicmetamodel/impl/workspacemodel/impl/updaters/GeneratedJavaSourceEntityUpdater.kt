package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import org.jetbrains.plugins.bsp.workspacemodel.entities.BspProjectEntitySource
import org.jetbrains.plugins.bsp.workspacemodel.entities.GeneratedJavaSourceRootEntity
import org.jetbrains.plugins.bsp.workspacemodel.entities.JavaSourceRoot
import org.jetbrains.plugins.bsp.workspacemodel.entities.PackageNameId
import java.nio.file.Path

internal class GeneratedJavaSourceEntityUpdater(private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig) :
  WorkspaceModelEntityWithoutParentModuleUpdater<JavaSourceRoot, GeneratedJavaSourceRootEntity> {
  override fun addEntity(entityToAdd: JavaSourceRoot): GeneratedJavaSourceRootEntity = addEntities(listOf(entityToAdd)).single()

  override fun addEntities(entitiesToAdd: List<JavaSourceRoot>): List<GeneratedJavaSourceRootEntity> {
    val packages = mutableMapOf<String, MutableList<Path>>()
    for (entity in entitiesToAdd) {
      if (!entity.generated) continue
      if (entity.packagePrefix.isEmpty()) continue
      val packagePaths = packages.getOrPut(entity.packagePrefix) { mutableListOf() }
      packagePaths.add(entity.sourcePath)
    }

    return packages
      .map { (packageName, sourceRoots) ->
        val sourceRootUrls =
          sourceRoots.map {
            it.toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager)
          }
        GeneratedJavaSourceRootEntity(
          packageNameId = PackageNameId(packageName),
          sourceRoots = sourceRootUrls,
          entitySource = BspProjectEntitySource,
        )
      }.map {
        workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.addEntity(it)
      }
  }
}
