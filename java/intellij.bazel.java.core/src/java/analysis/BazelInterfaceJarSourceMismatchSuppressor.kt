package org.jetbrains.bazel.java.analysis

import com.intellij.codeInsight.daemon.impl.LibrarySourcesMismatchSuppressor
import com.intellij.psi.PsiClass

/**
 * The mismatch between interface jar and source jar is expected for Bazel projects, so we suppress it.
 */
internal class BazelInterfaceJarSourceMismatchSuppressor : LibrarySourcesMismatchSuppressor {
  override fun shouldSuppress(sourceClass: PsiClass): Boolean {
    val file = sourceClass.originalElement.containingFile?.virtualFile ?: return false
    return file.isBazelInterfaceJar()
  }
}
