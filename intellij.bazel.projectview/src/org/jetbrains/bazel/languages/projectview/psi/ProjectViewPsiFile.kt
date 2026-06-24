package org.jetbrains.bazel.languages.projectview.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil.getChildrenOfType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.projectview.base.ProjectViewFileType
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImportBase
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection

@ApiStatus.Internal
open class ProjectViewPsiFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, ProjectViewLanguage) {
  override fun getFileType(): FileType = ProjectViewFileType

  internal fun getSection(keyword: String): ProjectViewPsiSection? =
    getChildrenOfType(this, ProjectViewPsiSection::class.java)?.find {
      it.getKeyword().text ==
        keyword
    }

  fun getSectionsOrImports(): List<PsiElement> =
    children
      .filter {
        it is ProjectViewPsiSection || it is ProjectViewPsiImportBase
      }.toList()
}
