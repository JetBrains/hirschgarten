package org.jetbrains.bazel.services

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.impl.ModuleFileIndexImpl
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileFilter
import org.jetbrains.bazel.config.isBazelProject

/**
 * An O(N^2) complexity was introduced in [this commit](https://github.com/JetBrains/intellij-community/commit/f8e0b7a55a598fcf3ac5cb803275de0fa7f66984).
 * For every module in the project ModuleFileIndexImpl scans every file set in the project.
 * Apparently this behavior is important for Rider.
 * This class just reverts this commit for BSP projects.
 */
class BazelModuleFileIndex(private val module: Module) : ModuleFileIndexImpl(module) {
  override fun iterateContent(processor: ContentIterator, filter: VirtualFileFilter?): Boolean {
    if (!module.project.isBazelProject) return super.iterateContent(processor, filter)

    val contentRoots = getModuleRootsToIterate()
    for (contentRoot in contentRoots) {
      if (!iterateContentUnderDirectory(contentRoot, processor, filter)) {
        return false
      }
    }
    return true
  }

  private fun getModuleRootsToIterate(): Set<VirtualFile> {
    return ReadAction.compute<Set<VirtualFile>, Throwable> {
      if (module.isDisposed) {
        return@compute emptySet()
      }

      val result = mutableSetOf<VirtualFile>()
      val moduleRootManager = ModuleRootManager.getInstance(module)
      val projectFileIndex = ProjectFileIndex.getInstance(module.project)
      for (roots in listOf(moduleRootManager.contentRoots, moduleRootManager.sourceRoots)) {
        for (root in roots) {
          if (!projectFileIndex.isInProject(root)) continue

          val parent = root.parent
          if (parent != null) {
            val parentModule = projectFileIndex.getModuleForFile(parent, false)
            if (module == parentModule) {
              // inner content - skip it
              continue
            }
          }
          result.add(root)
        }
      }
      result
    }
  }
}
