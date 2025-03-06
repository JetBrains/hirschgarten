package org.jetbrains.bazel.languages.bazelquery.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import org.jetbrains.bazel.languages.bazelquery.BazelqueryFileType
import org.jetbrains.bazel.languages.bazelquery.BazelqueryFlagsFileType
import org.jetbrains.bazel.languages.bazelquery.BazelqueryFlagsLanguage
import org.jetbrains.bazel.languages.bazelquery.BazelqueryLanguage

class BazelqueryFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, BazelqueryLanguage) {
    override fun getFileType(): FileType = BazelqueryFileType

    val prompts: Array<BazelqueryPrompt>
        get() = this.findChildrenByClass(BazelqueryPrompt::class.java)
}

class BazelqueryFlagsFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, BazelqueryFlagsLanguage) {
  override fun getFileType(): FileType = BazelqueryFlagsFileType

}
