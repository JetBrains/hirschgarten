package org.jetbrains.bazel.languages.projectview.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.isAncestor
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.projectview.BUILD_FLAGS_KEY
import org.jetbrains.bazel.languages.projectview.base.ProjectViewFileType
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImportBase
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionItem
import org.jetbrains.bazel.languages.projectview.psi.sections.sectionName

private val COMPILATION_MODE_FLAG = Regex("""^(-c|--compilation_mode)([= ]\S+)?$""")

/**
 * Sets `--compilation_mode=[mode]` after all `build_flags` sections and imports of the file.
 * Removes every other compilation mode flag from the `build_flags` sections of the file.
 * Appends the flag to the last `build_flags` section when no import follows it. Otherwise,
 * adds a new section at the end of the file.
 */
@ApiStatus.Internal
fun ProjectViewPsiFile.setCompilationMode(mode: String) {
  for (section in childrenOfType<ProjectViewPsiSection>().filter(::isBuildFlagsSection)) {
    section.getItems().filter { COMPILATION_MODE_FLAG.matches(it.text.trim()) }.forEach { removeItem(section, it) }
  }

  val item = "--compilation_mode=$mode"
  val buildFlags = lastBuildFlagsContributor() as? ProjectViewPsiSection

  if (buildFlags == null) {
    val newSection = createBuildFlagsSection(item)
    val lastChild = lastChild
    if (lastChild != null && !lastChild.text.endsWith("\n")) {
      add(createNewLine() ?: return)
    }
    add(newSection)
    return
  }

  val newSection = createBuildFlagsSection(item)
  val colon = newSection.getColon() ?: return
  buildFlags.addRangeAfter(colon.nextSibling, newSection.getItems().first(), buildFlags.getItems().lastOrNull() ?: buildFlags.getColon())
}

/**
 * Returns the offset where [setCompilationMode] writes the flag.
 * This is the end of the last `build_flags` section when no import follows it. Otherwise, it is the end of the file.
 */
@ApiStatus.Internal
fun ProjectViewPsiFile.buildFlagsEndOffset(): Int {
  val buildFlags = lastBuildFlagsContributor() as? ProjectViewPsiSection ?: return textLength
  return (buildFlags.getItems().lastOrNull() ?: buildFlags.getColon() ?: buildFlags).textRange.endOffset
}

/** Returns the last `build_flags` section or import of the file. An import can supply `build_flags` too. */
private fun ProjectViewPsiFile.lastBuildFlagsContributor(): PsiElement? {
  return children.lastOrNull { it is ProjectViewPsiImportBase || it is ProjectViewPsiSection && isBuildFlagsSection(it) }
}

private fun isBuildFlagsSection(section: ProjectViewPsiSection): Boolean {
  return section.sectionName() == BUILD_FLAGS_KEY.name
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
