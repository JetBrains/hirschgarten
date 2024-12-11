package org.jetbrains.bazel.languages.projectview.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import org.jetbrains.bazel.languages.projectview.base.ProjectViewFileType
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage

open class ProjectViewFile(viewProvider: FileViewProvider):
  PsiFileBase(viewProvider, ProjectViewLanguage),
  ProjectViewElement {

  override fun getFileType(): FileType = ProjectViewFileType
}
