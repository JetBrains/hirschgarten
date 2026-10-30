package org.jetbrains.bazel.languages.projectview.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.isAncestor
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.projectview.BUILD_FLAGS_KEY
import org.jetbrains.bazel.languages.projectview.base.ProjectViewFileType
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionItem

private val COMPILATION_MODE_FLAG = Regex("""^(-c|--compilation_mode)([= ]\S+)?$""")

/**
 * Sets `--compilation_mode=[mode]` in the `build_flags` section. Removes every other compilation mode flag of the section.
 * Creates the section when it is missing.
 */
@ApiStatus.Internal
fun ProjectViewPsiFile.setCompilationMode(mode: String) {
  val buildFlags = getSection(BUILD_FLAGS_KEY)
  val item = "--compilation_mode=$mode"

  if (buildFlags == null) {
    val newSection = createBuildFlagsSection(item)
    val lastChild = lastChild
    if (lastChild != null && !lastChild.text.endsWith("\n")) {
      add(createNewLine() ?: return)
    }
    add(newSection)
    return
  }

  buildFlags.getItems().filter { COMPILATION_MODE_FLAG.matches(it.text.trim()) }.forEach { removeItem(buildFlags, it) }

  val newSection = createBuildFlagsSection(item)
  val colon = newSection.getColon() ?: return
  buildFlags.addRangeAfter(colon.nextSibling, newSection.getItems().first(), buildFlags.getItems().lastOrNull() ?: buildFlags.getColon())
}

/**
 * Returns the offset after the last item of the `build_flags` section, or the end of the file when the section is missing.
 */
@ApiStatus.Internal
fun ProjectViewPsiFile.buildFlagsEndOffset(): Int {
  val buildFlags = getSection(BUILD_FLAGS_KEY) ?: return textLength
  return (buildFlags.getItems().lastOrNull() ?: buildFlags.getColon() ?: buildFlags).textRange.endOffset
}

private fun removeItem(section: ProjectViewPsiSection, item: ProjectViewPsiSectionItem) {
  val from = item.prevSibling?.takeIf(section::isAncestor) ?: section.getColon()
  val to = item.nextSibling?.takeIf(section::isAncestor) ?: section.lastChild
  section.deleteChildRange(from, to)
}

private fun ProjectViewPsiFile.createBuildFlagsSection(item: String): ProjectViewPsiSection {
  return ProjectViewElementFactory(project).createSection(BUILD_FLAGS_KEY.name, listOf(item))
}

private fun ProjectViewPsiFile.createNewLine(): PsiElement? {
  return createDummyFile("\n").firstChild
}

private fun ProjectViewPsiFile.createDummyFile(text: String): PsiFile {
  return PsiFileFactory
    .getInstance(project)
    .createFileFromText("dummy.bazelproject", ProjectViewFileType, text)
}
