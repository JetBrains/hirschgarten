package org.jetbrains.bazel.languages.starlark.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage

class StarlarkFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, StarlarkLanguage) {
  override fun getFileType(): FileType =
    StarlarkFileType
}
