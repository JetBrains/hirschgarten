package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.workspacemodel.entities.BazelJavaSourceRootEntity
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectEntitySource
import org.jetbrains.bazel.workspacemodel.entities.JavaSourceRoot
import org.jetbrains.bazel.workspacemodel.entities.PackageNameId
import java.nio.file.Path
import kotlin.io.path.extension

internal class BazelJavaSourceRootEntityUpdater(private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig) :
  WorkspaceModelEntityWithoutParentModuleUpdater<JavaSourceRoot, BazelJavaSourceRootEntity> {
  override suspend fun addEntity(entityToAdd: JavaSourceRoot): BazelJavaSourceRootEntity = addEntities(listOf(entityToAdd)).single()

  override suspend fun addEntities(entitiesToAdd: List<JavaSourceRoot>): List<BazelJavaSourceRootEntity> {
    val packages = mutableMapOf<String, MutableList<Path>>()
    for (entity in entitiesToAdd) {
      if (!shouldAddBazelJavaSourceRootEntity(entity)) continue
      val packagePaths = packages.getOrPut(entity.packagePrefix) { mutableListOf() }
      packagePaths.add(entity.sourcePath)
    }

    return packages
      .map { (packageName, sourceRoots) ->
        val sourceRootUrls =
          sourceRoots.map {
            it.toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager)
          }
        BazelJavaSourceRootEntity(
          packageNameId = PackageNameId(packageName),
          sourceRoots = sourceRootUrls,
          entitySource = BazelProjectEntitySource,
        )
      }.map {
        workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.addEntity(it)
      }
  }

  companion object {
    fun shouldAddBazelJavaSourceRootEntity(root: JavaSourceRoot): Boolean {
      if (BazelFeatureFlags.fbsrSupportedInPlatform) return false
      if (root.sourcePath.extension != "java") return false
      if (BazelFeatureFlags.enableBazelJavaClassFinder) return true
      return root.generated
    }
  }
}
