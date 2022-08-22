package org.jetbrains.bsp.bazel.languages.starlark.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import org.jetbrains.bsp.bazel.languages.starlark.StarlarkLanguage
import org.jetbrains.bsp.bazel.languages.starlark.StarlarkLanguageFileType

class StarlarkFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, StarlarkLanguage) {
    override fun getFileType(): FileType =
        StarlarkLanguageFileType
}
