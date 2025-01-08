package org.jetbrains.bazel.languages.bazelrc.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import org.jetbrains.bazel.languages.bazelrc.BazelrcFileType
import org.jetbrains.bazel.languages.bazelrc.BazelrcLanguage

class BazelrcFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, BazelrcLanguage) {
  override fun getFileType(): FileType = BazelrcFileType

  val imports: Array<BazelrcImport>
    get() = this.findChildrenByClass(BazelrcImport::class.java)

  val lines: Array<BazelrcLine>
    get() = this.findChildrenByClass(BazelrcLine::class.java)
}
