package org.jetbrains.bazel.languages.bazelrc.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import org.jetbrains.bazel.languages.bazelrc.BazelrcFileType
import org.jetbrains.bazel.languages.bazelrc.BazelrcLanguage

class BazelrcFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, BazelrcLanguage) {
  override fun getFileType(): FileType = BazelrcFileType

  fun findDeclaredConfigs(): Set<String> = this.findChildrenByClass(BazelrcLine::class.java).mapNotNull(BazelrcLine::configName).toSet()
}
