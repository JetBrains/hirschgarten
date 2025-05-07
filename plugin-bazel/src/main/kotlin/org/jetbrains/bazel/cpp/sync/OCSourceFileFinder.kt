package org.jetbrains.bazel.cpp.sync

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.lang.preprocessor.OCImportGraph

// See com.google.idea.blaze.cpp.oclang.OCSourceFileFinder
object OCSourceFileFinder {
  fun getSourceFileForHeaderFile(project: Project, headerFile: VirtualFile): VirtualFile? {
    val roots =
      OCImportGraph.getInstance(project).getAllHeaderRoots(headerFile)

    val headerNameWithoutExtension = headerFile.nameWithoutExtension
    for (root in roots) {
      if (root.nameWithoutExtension == headerNameWithoutExtension) {
        return root
      }
    }
    return null
  }
}
