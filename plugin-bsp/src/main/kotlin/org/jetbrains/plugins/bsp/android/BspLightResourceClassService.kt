package org.jetbrains.plugins.bsp.android

import com.android.tools.idea.projectsystem.LightResourceClassService
import com.android.tools.idea.projectsystem.getProjectSystem
import com.android.tools.idea.res.AndroidDependenciesCache
import com.android.tools.idea.res.AndroidLightPackage
import com.android.tools.idea.res.ModuleRClass
import com.android.tools.idea.res.ResourceRepositoryRClass
import com.intellij.facet.ProjectFacetManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiPackage
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.android.augment.AndroidLightField
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.kotlin.idea.base.util.module
import java.util.concurrent.atomic.AtomicReference

public class BspLightResourceClassService(private val project: Project) : LightResourceClassService {
  private val rClasses = AtomicReference<Map<AndroidFacet, PsiClass>>()

  public fun updateRClasses() {
    rClasses.set(createRClasses(project))
  }

  private fun createRClasses(project: Project): Map<AndroidFacet, PsiClass> =
    ProjectFacetManager.getInstance(project).getFacets(AndroidFacet.ID).associateWith { facet ->
      createRClassForAndroidFacet(facet)
    }

  private fun createRClassForAndroidFacet(androidFacet: AndroidFacet): PsiClass = ModuleRClass(
    androidFacet,
    PsiManager.getInstance(project),
    ModuleRClass.SourceSet.MAIN,
    ResourceRepositoryRClass.Transitivity.TRANSITIVE,
    AndroidLightField.FieldModifier.NON_FINAL,
  )

  override fun getLightRClasses(qualifiedName: String, scope: GlobalSearchScope): Collection<PsiClass> {
    val packageName = qualifiedName.dropLast(2)
    return getAndroidFacetsWithPackageName(packageName)
      .mapNotNull { androidFacet -> getAndroidFacetRClass(androidFacet) }
      .filter { psiClass -> isSearchInModuleContent(scope, psiClass) }
  }

  private fun isSearchInModuleContent(scope: GlobalSearchScope, psiClass: PsiClass): Boolean {
    val module = psiClass.module ?: return false
    return scope.isSearchInModuleContent(module)
  }

  override fun getLightRClassesAccessibleFromModule(
    module: Module,
    includeTestClasses: Boolean,
  ): Collection<PsiClass> {
    val androidFacet = AndroidFacet.getInstance(module) ?: return emptyList()
    val dependentAndroidFacets =
      listOf(androidFacet) + AndroidDependenciesCache.getAllAndroidDependencies(module, false)
    return dependentAndroidFacets.mapNotNull { getAndroidFacetRClass(it) }
  }

  override fun getLightRClassesDefinedByModule(module: Module, includeTestClasses: Boolean): Collection<PsiClass> {
    val androidFacet = AndroidFacet.getInstance(module) ?: return emptyList()
    val rClass = getAndroidFacetRClass(androidFacet) ?: return emptyList()
    return listOf(rClass)
  }

  override fun getLightRClassesContainingModuleResources(module: Module): Collection<PsiClass> {
    val dependents = mutableSetOf<Module>()
    ModuleUtilCore.collectModulesDependsOn(module, dependents)
    return dependents.flatMap { getLightRClassesDefinedByModule(module, false) }
  }

  override fun findRClassPackage(packageName: String): PsiPackage? {
    if (getAndroidFacetsWithPackageName(packageName).isEmpty()) return null
    return AndroidLightPackage.withName(packageName, project)
  }

  override fun getAllLightRClasses(): Collection<PsiClass> = rClasses.get().values

  private fun getAndroidFacetsWithPackageName(packageName: String): Collection<AndroidFacet> =
    project.getProjectSystem().getAndroidFacetsWithPackageName(project, packageName)

  private fun getAndroidFacetRClass(androidFacet: AndroidFacet): PsiClass? = rClasses.get()[androidFacet]

  public companion object {
    public fun getInstance(project: Project): BspLightResourceClassService =
      project.getService(BspLightResourceClassService::class.java)
  }
}
