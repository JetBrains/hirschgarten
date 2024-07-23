package org.jetbrains.bazel.languages.bazel

import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.kotlin.utils.findIsInstanceAnd

data class BazelPackage(val buildFile: StarlarkFile) {
  companion object {
    fun ofFile(file: PsiFile): BazelPackage? = when (file) {
      is StarlarkFile -> ofStarlarkFile(file)
      else -> ofDirectory(file.containingDirectory)
    }

    private fun ofStarlarkFile(starlarkFile: StarlarkFile): BazelPackage? =
      if (starlarkFile.isBuildFile()) BazelPackage(starlarkFile) else ofDirectory(starlarkFile.containingDirectory)

    private tailrec fun ofDirectory(directory: PsiDirectory?): BazelPackage? = directory?.let { dir ->
      dir.files.toList().findIsInstanceAnd<StarlarkFile> { it.isBuildFile() }?.let { BazelPackage(it) }
        ?: return ofDirectory(dir.parentDirectory)
    }
  }
}
