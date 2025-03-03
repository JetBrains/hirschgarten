package org.jetbrains.bazel.run.coverage

import com.intellij.coverage.BaseCoverageSuite
import com.intellij.coverage.CoverageEngine
import com.intellij.coverage.CoverageFileProvider
import com.intellij.coverage.CoverageRunner
import com.intellij.coverage.CoverageSuitesBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.rt.coverage.data.LineData
import com.intellij.rt.coverage.data.ProjectData
import org.jetbrains.bazel.config.rootDir

class BazelCoverageSuite : BaseCoverageSuite {
  var rootDirectory: VirtualFile? = null
    private set

  constructor() : super()

  constructor(
    name: String,
    project: Project,
    runner: CoverageRunner,
    fileProvider: CoverageFileProvider,
    timestamp: Long,
  ) : super(name, project, runner, fileProvider, timestamp)

  override fun getCoverageEngine(): CoverageEngine = BazelCoverageEngine.getInstance()

  override fun setCoverageData(projectData: ProjectData?) {
    super.setCoverageData(projectData)
    if (projectData == null) {
      rootDirectory = null
      return
    }
    val files =
      projectData.classes.values
        .filter { classData ->
          @Suppress("UNCHECKED_CAST")
          (classData.lines as Array<LineData>).any { it.hits > 0 }
        }.map { classData ->
          BazelCoverageRunner.getFileByCoverageDataName(classData.name)
        }
    rootDirectory =
      VfsUtil.getCommonAncestor(files)?.let {
        if (it.isDirectory) it else it.parent
      }
  }

  companion object {
    fun getRoots(bundle: CoverageSuitesBundle): List<VirtualFile> {
      val roots =
        bundle.suites
          .filterIsInstance<BazelCoverageSuite>()
          .mapNotNull { it.rootDirectory }
          .toSet()
      if (roots.isEmpty()) return emptyList()

      val projectRoot = bundle.project.rootDir
      if (projectRoot in roots) return listOf(projectRoot)

      val rootsThatDontContainEachOther = roots.filterNot { root -> VfsUtilCore.isUnder(root.parent, roots) }
      return rootsThatDontContainEachOther
    }
  }
}
