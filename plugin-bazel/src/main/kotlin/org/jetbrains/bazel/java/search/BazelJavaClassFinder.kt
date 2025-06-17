package org.jetbrains.bazel.java.search

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiPackage
import com.intellij.psi.impl.ResolveScopeManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.UseScopeEnlarger
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.BazelJavaSourceRootEntity
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.PackageNameId

/**
 * Adding dummy modules for generated source files can make large parts of the build directory get indexed.
 * Therefore, we use our own "index" to resolve classes in generated source files.
 */
internal class BazelJavaClassFinder(private val project: Project) : PsiElementFinder() {
  private val workspaceModel = WorkspaceModel.getInstance(project)

  override fun findClass(qualifiedName: String, scope: GlobalSearchScope): PsiClass? =
    findGeneratedClasses(qualifiedName, scope).firstOrNull()

  override fun findClasses(qualifiedName: String, scope: GlobalSearchScope): Array<PsiClass> =
    findGeneratedClasses(qualifiedName, scope).toList().toTypedArray()

  private fun findGeneratedClasses(qualifiedName: String, scope: GlobalSearchScope): Sequence<PsiClass> {
    if (BazelFeatureFlags.fbsrSupportedInPlatform) return emptySequence()
    return findGeneratedClassesInPackage(
      packageName = StringUtil.getPackageName(qualifiedName),
      scope,
      classNameHint = StringUtil.getShortName(qualifiedName),
    ).filter { it.qualifiedName == qualifiedName }
  }

  override fun getClasses(psiPackage: PsiPackage, scope: GlobalSearchScope): Array<PsiClass> =
    findGeneratedClassesInPackage(psiPackage.qualifiedName, scope).toList().toTypedArray()

  override fun getClasses(
    className: String?,
    psiPackage: PsiPackage,
    scope: GlobalSearchScope,
  ): Array<PsiClass> {
    if (BazelFeatureFlags.fbsrSupportedInPlatform) return emptyArray()
    return findGeneratedClassesInPackage(psiPackage.qualifiedName, scope, classNameHint = className)
      .filter { className == null || it.name == className }
      .toList()
      .toTypedArray()
  }

  private fun findGeneratedClassesInPackage(
    packageName: String,
    scope: GlobalSearchScope,
    classNameHint: String? = null,
  ): Sequence<PsiClass> =
    workspaceModel.currentSnapshot
      .referrers(PackageNameId(packageName), BazelJavaSourceRootEntity::class.java)
      .flatMap { it.sourceRoots }
      .mapNotNull { it.virtualFile }
      // Check the files that match the class name first
      .partition { classNameHint != null && it.nameWithoutExtension == classNameHint }
      .let { it.first.asSequence() + it.second.asSequence() }
      .filter { scope.contains(it) }
      .mapNotNull { PsiManager.getInstance(project).findFile(it) }
      .mapNotNull { it as? PsiClassOwner }
      .flatMap { it.classes.asSequence() }
}

/**
 * PsiClassImplUtil.getClassUseScope returns [com.intellij.psi.search.PackageScope] for package-private Java classes.
 * However, PackageScope is broken if we use BazelJavaClassFinder instead of dummy modules because of FBSR,
 * so we need to override the scope manually.
 */
internal class BazelJavaUseScopeEnlarger : UseScopeEnlarger() {
  override fun getAdditionalUseScope(psiClass: PsiElement): SearchScope? {
    if (BazelFeatureFlags.fbsrSupportedInPlatform) return null
    if (psiClass !is PsiClass) return null

    val project = psiClass.project
    if (!project.isBazelProject) return null

    val packageName = (psiClass.containingFile as? PsiJavaFile)?.packageName ?: return null

    if (WorkspaceModel
        .getInstance(
          project,
        ).currentSnapshot
        .referrers(PackageNameId(packageName), BazelJavaSourceRootEntity::class.java)
        .none()
    ) {
      return null
    }

    if (!isPackagePrivate(psiClass)) return null
    return ResolveScopeManager.getElementUseScope(psiClass)
  }

  private fun isPackagePrivate(psiClass: PsiClass): Boolean =
    !psiClass.hasModifierProperty(PsiModifier.PUBLIC) &&
      !psiClass.hasModifierProperty(PsiModifier.PROTECTED) &&
      !psiClass.hasModifierProperty(PsiModifier.PRIVATE)
}
