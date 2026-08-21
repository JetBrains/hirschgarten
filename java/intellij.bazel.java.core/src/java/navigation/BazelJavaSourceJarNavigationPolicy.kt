package org.jetbrains.bazel.java.navigation

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.workspace.jps.entities.LibraryRootTypeId
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.compiled.ClsClassImpl
import com.intellij.psi.impl.compiled.ClsCustomNavigationPolicy
import com.intellij.psi.impl.compiled.ClsFileImpl
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScopesCore
import org.jetbrains.bazel.config.isBazelProject

/**
 * Bazel may produce the source jar without the strict layout - depending on the scenario, package root might not be under the jar root
 * or the package might not match the physical location at all.
 * Java plugin has a strict assumption on the source jar layout, so navigation does not work in this case.
 * Kotlin plugin is not that strict and navigation works by default.
 */
internal class BazelJavaSourceJarNavigationPolicy : ClsCustomNavigationPolicy {

  override fun getNavigationElement(clsFile: ClsFileImpl): PsiElement? {
    val project = clsFile.project
    if (DumbService.isDumb(project)) return null
    if (!project.isBazelProject) return null
    return clsFile.findJavaSourceFile()
  }
}

private const val JAVA_SOURCE_SUFFIX = ".java"

private fun ClsFileImpl.findJavaSourceFile(): PsiJavaFile? {
  val sourceFileName = (this.classes.firstOrNull() as? ClsClassImpl)?.sourceFileName ?: return null
  if (!sourceFileName.endsWith(JAVA_SOURCE_SUFFIX)) return null
  val project = this.project
  val packageName = this.packageName
  val sourceRoots = ProjectFileIndex
    .getInstance(project)
    .findContainingLibraries(virtualFile)
    .asSequence()
    .flatMap { it.roots }
    .filter { it.type == LibraryRootTypeId.SOURCES }
    .mapNotNull { it.url.virtualFile }
    .distinct()
    .toList()
    .toTypedArray()
  if (sourceRoots.isEmpty()) return null
  val sourceRootsScope = GlobalSearchScopesCore.directoriesScope(project, true, *sourceRoots)
  return FilenameIndex
    .getVirtualFilesByName(sourceFileName, sourceRootsScope)
    .firstNotNullOfOrNull {
      if (!it.isValid) return@firstNotNullOfOrNull null
      val psi = this.manager.findFile(it) as? PsiJavaFile
      if (psi?.packageName != packageName) return@firstNotNullOfOrNull null
      psi
    }
}
