package org.jetbrains.plugins.bsp.services

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.PsiPackage
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.plugins.bsp.workspacemodel.entities.GeneratedJavaSourceRootEntity
import org.jetbrains.plugins.bsp.workspacemodel.entities.PackageNameId

/**
 * Adding dummy modules for generated source files can make large parts of the build directory get indexed.
 * Therefore, we use our own "index" to resolve classes in generated source files.
 */
class GeneratedClassFinder(private val project: Project) : PsiElementFinder() {
  private val workspaceModel = WorkspaceModel.getInstance(project)

  override fun findClass(qualifiedName: String, scope: GlobalSearchScope): PsiClass? =
    findGeneratedClasses(qualifiedName, scope).firstOrNull()

  override fun findClasses(qualifiedName: String, scope: GlobalSearchScope): Array<PsiClass> =
    findGeneratedClasses(qualifiedName, scope).toList().toTypedArray()

  private fun findGeneratedClasses(qualifiedName: String, scope: GlobalSearchScope): Sequence<PsiClass> {
    val packageName = StringUtil.getPackageName(qualifiedName)
    return findGeneratedClassesInPackage(packageName, scope)
      .filter { it.qualifiedName == qualifiedName }
  }

  override fun getClasses(psiPackage: PsiPackage, scope: GlobalSearchScope): Array<PsiClass> =
    findGeneratedClassesInPackage(psiPackage.qualifiedName, scope).toList().toTypedArray()

  override fun getClasses(
    className: String?,
    psiPackage: PsiPackage,
    scope: GlobalSearchScope,
  ): Array<PsiClass> =
    findGeneratedClassesInPackage(psiPackage.qualifiedName, scope)
      .filter { className == null || it.name == className }
      .toList()
      .toTypedArray()

  private fun findGeneratedClassesInPackage(packageName: String, scope: GlobalSearchScope): Sequence<PsiClass> =
    workspaceModel.currentSnapshot
      .referrers(PackageNameId(packageName), GeneratedJavaSourceRootEntity::class.java)
      .flatMap { it.sourceRoots }
      .mapNotNull { it.virtualFile }
      .filter { scope.contains(it) }
      .mapNotNull { it.toPsiFile(project) }
      .mapNotNull { it as? PsiClassOwner }
      .flatMap { it.classes.asSequence() }
}
