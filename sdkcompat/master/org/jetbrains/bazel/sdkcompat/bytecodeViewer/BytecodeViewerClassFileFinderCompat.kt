package org.jetbrains.bazel.sdkcompat.bytecodeViewer

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass

interface BytecodeViewerClassFileFinderCompat {
  fun findClass(element: PsiClass, containing: PsiClass?): VirtualFile?

  companion object {
    val ep = ExtensionPointName<BytecodeViewerClassFileFinderCompat>("org.jetbrains.bazel.bytecodeViewerClassFileFinderCompat")
    const val isSupported = true
  }
}
