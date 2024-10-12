package org.jetbrains.plugins.bsp.services

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsRoot
import com.intellij.openapi.vcs.roots.VcsRootDetector
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.bsp.config.isBspProject

/**
 * Hack to fix https://youtrack.jetbrains.com/issue/BAZEL-948
 */
class BspVcsRootDetector(private val project: Project) : VcsRootDetector {
  private val delegate: VcsRootDetector

  init {
    // Unfortunately, it's package private
    val implementationClass = Class.forName("com.intellij.openapi.vcs.roots.VcsRootDetectorImpl")
    val constructor = implementationClass.getDeclaredConstructor(Project::class.java)
    constructor.isAccessible = true
    delegate = constructor.newInstance(project) as VcsRootDetector
  }

  override fun detect(): Collection<VcsRoot> = delegate.detect().filter { it.shouldKeep() }

  override fun detect(startDir: VirtualFile?): Collection<VcsRoot> = delegate.detect(startDir).filter { it.shouldKeep() }

  override fun getOrDetect(): Collection<VcsRoot> = delegate.getOrDetect().filter { it.shouldKeep() }

  private fun VcsRoot.shouldKeep(): Boolean {
    if (!project.isBspProject) return true
    val baseDir = project.baseDir ?: return true
    // Don't keep VCS roots inside the project base dir, e.g. basel-hirschgarten
    return VfsUtilCore.isAncestor(path, baseDir, false)
  }
}
