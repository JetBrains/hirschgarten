package org.jetbrains.bazel.languages.projectview.elements

import com.intellij.psi.tree.IFileElementType
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImport
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImportItem
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionItem
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionName

object ProjectViewElementTypes {
  val FILE = IFileElementType(ProjectViewLanguage)

  val IMPORT = ProjectViewElementType("import", ProjectViewPsiImport::class.java)
  val IMPORT_ITEM = ProjectViewElementType("import_item", ProjectViewPsiImportItem::class.java)

  val SECTION = ProjectViewElementType("section", ProjectViewPsiSection::class.java)
  val SECTION_NAME = ProjectViewElementType("section_name", ProjectViewPsiSectionName::class.java)
  val SECTION_ITEM = ProjectViewElementType("section_item", ProjectViewPsiSectionItem::class.java)
}
