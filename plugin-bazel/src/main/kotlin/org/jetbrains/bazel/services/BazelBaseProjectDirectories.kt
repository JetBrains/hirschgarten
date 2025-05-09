package org.jetbrains.bazel.services

import com.intellij.ide.project.impl.BaseProjectDirectoriesImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.workspace.storage.ImmutableEntityStorage
import com.intellij.platform.workspace.storage.VersionedStorageChange
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntity

class BazelBaseProjectDirectories(project: Project, scope: CoroutineScope) : BaseProjectDirectoriesImpl(project, scope) {
  override fun collectRoots(snapshot: ImmutableEntityStorage): Sequence<VirtualFile> =
    super.collectRoots(snapshot) +
      snapshot.entities(BazelProjectDirectoriesEntity::class.java).mapNotNull { it.projectRoot.virtualFile }

  override fun processChange(
    change: VersionedStorageChange,
    oldRoots: HashSet<VirtualFile>,
    newRoots: HashSet<VirtualFile>,
  ) {
    super.processChange(change, oldRoots, newRoots)
    change.getChanges(BazelProjectDirectoriesEntity::class.java).forEach {
      it.oldEntity?.projectRoot?.virtualFile?.let { virtualFile ->
        oldRoots.add(virtualFile)
      }
      it.newEntity?.projectRoot?.virtualFile?.let { virtualFile ->
        newRoots.add(virtualFile)
      }
    }
  }
}
