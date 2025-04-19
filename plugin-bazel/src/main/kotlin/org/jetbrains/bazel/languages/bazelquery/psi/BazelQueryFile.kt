package org.jetbrains.bazel.languages.bazelquery.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import org.jetbrains.bazel.languages.bazelquery.BazelQueryFileType
import org.jetbrains.bazel.languages.bazelquery.BazelQueryFlagsFileType
import org.jetbrains.bazel.languages.bazelquery.BazelQueryFlagsLanguage
import org.jetbrains.bazel.languages.bazelquery.BazelQueryLanguage

class BazelQueryFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, BazelQueryLanguage) {
  override fun getFileType(): FileType = BazelQueryFileType
}

class BazelQueryFlagsFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, BazelQueryFlagsLanguage) {
  override fun getFileType(): FileType = BazelQueryFlagsFileType
}
