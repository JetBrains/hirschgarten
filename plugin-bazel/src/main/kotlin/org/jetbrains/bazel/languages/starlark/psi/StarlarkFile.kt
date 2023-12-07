package org.jetbrains.bazel.languages.starlark.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage
import org.jetbrains.bazel.languages.starlark.psi.base.StarlarkElement

class StarlarkFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, StarlarkLanguage), StarlarkElement {
  override fun getFileType(): FileType = StarlarkFileType
}
