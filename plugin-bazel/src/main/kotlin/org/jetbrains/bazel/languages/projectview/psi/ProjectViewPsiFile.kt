package org.jetbrains.bazel.languages.projectview.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import org.jetbrains.bazel.languages.projectview.base.ProjectViewFileType
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType

open class ProjectViewPsiFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, ProjectViewLanguage) {
  override fun getFileType(): FileType = ProjectViewFileType

  fun getSection(keyword: String): ProjectViewPsiSection? =
    getChildrenOfType<ProjectViewPsiSection>().find {
      it.getKeyword().text ==
        keyword
    }
}
