package org.jetbrains.bazel.languages.projectview.psi

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileFactory
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.projectview.base.ProjectViewFileType
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.psi.psiUtil.isAncestor

fun ProjectViewPsiFile.addDirectoriesInclude(file: VirtualFile): Unit = addDirectoriesItem(file, true)

fun ProjectViewPsiFile.addDirectoriesExclude(file: VirtualFile): Unit = addDirectoriesItem(file, false)

fun ProjectViewPsiFile.removeDirectoriesInclude(file: VirtualFile): Unit = removeDirectoriesItem(file, true)

fun ProjectViewPsiFile.removeDirectoriesExclude(file: VirtualFile): Unit = removeDirectoriesItem(file, false)

private fun ProjectViewPsiFile.addDirectoriesItem(file: VirtualFile, include: Boolean) {
  val project = project
  val directories = directories
  val pathString = project.rootRelativePath(file) ?: return
  val item = if (include) pathString else "-$pathString"
  if (directories == null) {
    val newDirectorySection = project.createDirectoriesSection(item)
    addBefore(newDirectorySection, firstChild)
    return
  }
  val newDirectorySection = project.createDirectoriesSection(item)
  val colonPsi = newDirectorySection.getColon() ?: return
  directories.addRangeAfter(
    colonPsi.nextSibling,
    newDirectorySection.getItems().first(),
    directories.getItems().lastOrNull() ?: directories.getColon(),
  )
}

private fun ProjectViewPsiFile.removeDirectoriesItem(file: VirtualFile, include: Boolean) {
  val project = project
  val directories = directories ?: return
  val pathString = project.rootRelativePath(file) ?: return
  val itemText = if (include) pathString else "-$pathString"
  val items = directories.getItems()
  items.forEach { item ->
    if (item.text == itemText) {
      val from = item.prevSibling?.takeIf(directories::isAncestor) ?: directories.getColon()
      val to = item.nextSibling?.takeIf(directories::isAncestor) ?: directories.lastChild
      directories.deleteChildRange(from, to)
    }
  }
}

private val ProjectViewPsiFile.directories: ProjectViewPsiSection?
  get() = getSection("directories")

private fun Project.rootRelativePath(file: VirtualFile): String? = VfsUtil.getRelativePath(file, rootDir)
  ?.ifBlank { "." }

private fun Project.createDirectoriesSection(item: String) = PsiFileFactory
  .getInstance(this)
  .createFileFromText(
    "dummy.bazelproject",
    ProjectViewFileType,
    "directories:\n  $item\n\n",
  ).getChildrenOfType<ProjectViewPsiSection>()
  .first()
