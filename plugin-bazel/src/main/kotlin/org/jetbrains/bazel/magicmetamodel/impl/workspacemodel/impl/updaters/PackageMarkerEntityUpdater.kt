package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.workspaceModel.ide.toPath
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.BazelDummyEntitySource
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.JavaModule
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.JavaSourceRoot
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.ModifiablePackageMarkerEntity
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.Module
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.PackageMarkerEntity
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.packageMarkerEntities
import org.jetbrains.bazel.workspace.bazelProjectDirectoriesEntity
import org.jetbrains.bazel.workspace.packageMarker.concatenatePackages
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.name

class PackageMarkerEntityUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
  moduleEntities: List<Module>,
) : WorkspaceModelEntityWithParentModuleUpdater<JavaSourceRoot, PackageMarkerEntity> {
  private val alreadyVisitedDirectories: MutableSet<Path> =
    moduleEntities
      .asSequence()
      .filterIsInstance<JavaModule>()
      .filter { !it.genericModuleInfo.isDummy }
      .flatMap { it.sourceRoots }
      .map { it.sourcePath }
      .toMutableSet()

  private val excludedDirectories =
    workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder
      .bazelProjectDirectoriesEntity()
      ?.excludedRoots
      .orEmpty()
      .map { it.toPath() }
      .toSet()

  override suspend fun addEntities(entitiesToAdd: List<JavaSourceRoot>, parentModuleEntity: ModuleEntity): List<PackageMarkerEntity> {
    val entities =
      entitiesToAdd.flatMap { entityToAdd ->
        iterateSubdirectories(entityToAdd)
      }
    workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.modifyModuleEntity(parentModuleEntity) {
      this.packageMarkerEntities += entities
    }
    return parentModuleEntity.packageMarkerEntities
  }

  private fun iterateSubdirectories(entityToAdd: JavaSourceRoot): List<ModifiablePackageMarkerEntity> {
    val entities = mutableListOf<ModifiablePackageMarkerEntity>()
    val visitor =
      object : SimpleFileVisitor<Path>() {
        override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
          if (attrs.isSymbolicLink) return FileVisitResult.SKIP_SUBTREE
          if (dir in excludedDirectories || dir.name == ".idea") return FileVisitResult.SKIP_SUBTREE
          if (!alreadyVisitedDirectories.add(dir)) return FileVisitResult.SKIP_SUBTREE
          val relativePath = entityToAdd.sourcePath.relativize(dir)
          val relativePackagePrefix = relativePath.toString().replace(relativePath.fileSystem.separator, ".")
          val packagePrefix = concatenatePackages(entityToAdd.packagePrefix, relativePackagePrefix)
          entities +=
            PackageMarkerEntity(
              root = dir.toResolvedVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager),
              packagePrefix = packagePrefix,
              entitySource = BazelDummyEntitySource,
            )
          return FileVisitResult.CONTINUE
        }
      }
    Files.walkFileTree(entityToAdd.sourcePath, visitor)
    return entities
  }

  override suspend fun addEntity(entityToAdd: JavaSourceRoot, parentModuleEntity: ModuleEntity): PackageMarkerEntity =
    throw UnsupportedOperationException()
}
