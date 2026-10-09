package org.jetbrains.bazel.languages.projectview.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.childrenOfType
import org.jetbrains.bazel.languages.projectview.base.ProjectViewFileType
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection

private const val DUMMY_FILENAME = "dummy.bazelproject"

internal class ProjectViewElementFactory(private val project: Project) {

  fun createSection(name: String, items: List<String> = emptyList()): ProjectViewPsiSection {
    val itemLines = items.joinToString(separator = "") { "  $it\n" }
    return createFile("$name:\n$itemLines")
      .childrenOfType<ProjectViewPsiSection>()
      .first()
  }

  fun createSingleLineSection(name: String, item: String): ProjectViewPsiSection = createFile("$name: $item\n")
    .childrenOfType<ProjectViewPsiSection>()
    .first()

  private fun createFile(text: String): PsiFile =
    PsiFileFactory
      .getInstance(project)
      .createFileFromText(DUMMY_FILENAME, ProjectViewFileType, text)
}
