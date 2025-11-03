package org.jetbrains.bazel.languages.projectview.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import org.jetbrains.bazel.languages.projectview.base.ProjectViewFileType
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImportBase
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType

open class ProjectViewPsiFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, ProjectViewLanguage) {
  override fun getFileType(): FileType = ProjectViewFileType

  fun getSection(keyword: String): ProjectViewPsiSection? =
    getChildrenOfType<ProjectViewPsiSection>().find {
      it.getKeyword().text ==
        keyword
    }

  fun getSectionsOrImports(): List<PsiElement> =
    children
      .filter {
        it is ProjectViewPsiSection || it is ProjectViewPsiImportBase
      }.toList()
}

fun Project.getProjectViewPsiFileOrNull(): ProjectViewPsiFile? {
  val file = bazelProjectSettings
    .projectViewPath
    ?: return null
  return PsiManager
    .getInstance(this)
    .findFile(file) as? ProjectViewPsiFile
}
