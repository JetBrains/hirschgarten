package org.jetbrains.bazel.languages.bazelversion.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import org.jetbrains.bazel.languages.bazelversion.BazelVersionFileType
import org.jetbrains.bazel.languages.bazelversion.BazelVersionLanguage

class BazelVersionFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, BazelVersionLanguage) {
  override fun getFileType(): FileType = BazelVersionFileType

  val bazelVersion: BazelVersionLiteral?
    get() = this.text.toBazelVersionLiteral()
}
