package org.jetbrains.bazel.workspace.importer

import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.entities
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import com.intellij.workspaceModel.ide.toPath
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.workspace.packageMarker.concatenatePackages
import org.jetbrains.bazel.workspacemodel.entities.BazelDummyEntitySource
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntity
import org.jetbrains.bazel.workspacemodel.entities.PackageMarkerEntity
import org.jetbrains.bazel.workspacemodel.entities.PackageMarkerEntityBuilder
import org.jetbrains.bazel.workspacemodel.entities.packageMarkerEntities
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.name
import kotlin.io.path.notExists

/**
 * Adds [PackageMarkerEntity] instances for dummy modules so the workspace model can attribute
 * orphan source directories (not covered by a real module) to a package prefix.
 *
 * Shared mutable state tracks directories already covered by non-dummy modules so dummy markers
 * skip them; therefore one [PackageMarkerBuilder] should be constructed per import pass with
 * the full list of non-dummy source roots up front.
 */
@ApiStatus.Internal
class PackageMarkerBuilder(
  alreadyCoveredDirectories: Set<Path>,
  private val excludedDirectories: Set<Path>,
) {
  private val alreadyVisitedDirectories: MutableSet<Path> = alreadyCoveredDirectories.toMutableSet()

  fun write(
    sourceRoots: List<SourceRootBuilder.ResolvedSourceRoot>,
    parentModuleEntity: ModuleEntity,
    virtualFileUrlManager: VirtualFileUrlManager,
    storage: MutableEntityStorage,
  ) {
    val newEntities = sourceRoots.flatMap { iterateSubdirectories(it, virtualFileUrlManager) }
    if (newEntities.isEmpty()) {
      return
    }
    storage.modifyModuleEntity(parentModuleEntity) {
      this.packageMarkerEntities += newEntities
    }
  }

  private fun iterateSubdirectories(
    resolved: SourceRootBuilder.ResolvedSourceRoot,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): List<PackageMarkerEntityBuilder> {
    if (resolved.sourcePath.notExists()) return emptyList()
    val entities = mutableListOf<PackageMarkerEntityBuilder>()
    val visitor =
      object : SimpleFileVisitor<Path>() {
        override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
          if (attrs.isSymbolicLink) return FileVisitResult.SKIP_SUBTREE
          if (dir in excludedDirectories || dir.name == ".idea") return FileVisitResult.SKIP_SUBTREE
          if (!alreadyVisitedDirectories.add(dir)) return FileVisitResult.SKIP_SUBTREE
          val relativePath = resolved.sourcePath.relativize(dir)
          val relativePackagePrefix = relativePath.toString().replace(relativePath.fileSystem.separator, ".")
          val packagePrefix = concatenatePackages(resolved.packagePrefix, relativePackagePrefix)
          entities +=
            PackageMarkerEntity(
              root = dir.toResolvedVirtualFileUrl(virtualFileUrlManager),
              packagePrefix = packagePrefix,
              entitySource = BazelDummyEntitySource,
            )
          return FileVisitResult.CONTINUE
        }
      }
    Files.walkFileTree(resolved.sourcePath, visitor)
    return entities
  }

  companion object {
    // reads excluded directories from the `BazelProjectDirectoriesEntity` already in the storage.
    fun excludedDirectoriesFrom(storage: MutableEntityStorage): Set<Path> =
      storage.entities<BazelProjectDirectoriesEntity>()
        .firstOrNull()?.excludedRoots
        .orEmpty()
        .map { it.url.toPath() }
        .toSet()
  }
}
