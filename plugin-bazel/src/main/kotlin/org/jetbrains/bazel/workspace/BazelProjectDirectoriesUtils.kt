package org.jetbrains.bazel.workspace

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.impl.WorkspaceModelInternal
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.workspace.storage.CachedValue
import com.intellij.platform.workspace.storage.EntityStorage
import com.intellij.platform.workspace.storage.entities
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.BazelProjectDirectoriesEntity
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

/**
 * Returns the set of excluded directories, i.e., explicit excludes from project view and Bazel symlinks.
 * Returns `null` if the project has not been synced yet or no project model is present after clearing cache/IDEA update
 */
fun Project.excludedRoots(): Set<VirtualFile>? =
  (WorkspaceModel.getInstance(this) as WorkspaceModelInternal).entityStorage.cachedValue(excludedRoots).getOrNull()

/**
 * @see [excludedRoots]
 */
fun Project.includedRoots(): Set<VirtualFile>? =
  (WorkspaceModel.getInstance(this) as WorkspaceModelInternal).entityStorage.cachedValue(includedRoots).getOrNull()

private val excludedRoots: CachedValue<Optional<Set<VirtualFile>>> =
  CachedValue { storage ->
    val bazelProjectDirectories =
      storage.bazelProjectDirectoriesEntity()
        ?: return@CachedValue Optional.empty()
    Optional.of(bazelProjectDirectories.excludedRoots.toVirtualFileSet())
  }

private val includedRoots: CachedValue<Optional<Set<VirtualFile>>> =
  CachedValue { storage ->
    val bazelProjectDirectories =
      storage.bazelProjectDirectoriesEntity()
        ?: return@CachedValue Optional.empty()
    Optional.of(bazelProjectDirectories.includedRoots.toVirtualFileSet())
  }

fun Project.bazelProjectDirectoriesEntity(): BazelProjectDirectoriesEntity? =
  WorkspaceModel.getInstance(this).currentSnapshot.bazelProjectDirectoriesEntity()

fun EntityStorage.bazelProjectDirectoriesEntity(): BazelProjectDirectoriesEntity? = entities<BazelProjectDirectoriesEntity>().firstOrNull()

fun List<VirtualFileUrl>.toVirtualFileSet(): Set<VirtualFile> =
  asSequence()
    .mapNotNull { it.virtualFile }
    .toSet()
