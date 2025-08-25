package org.jetbrains.bazel.sdkcompat.bytecodeViewer

import com.intellij.byteCodeViewer.BytecodeViewerClassFileFinder
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass

class BazelCompatBytecodeViewerClassFileFinder : BytecodeViewerClassFileFinder {
  override fun findClass(
    element: PsiClass,
    containing: PsiClass?,
  ): VirtualFile? {
    for (compat in BytecodeViewerClassFileFinderCompat.ep.extensionList) {
      val vFile = compat.findClass(element, containing)
      if (vFile != null) {
        return vFile
      }
    }
    return null
  }
}
