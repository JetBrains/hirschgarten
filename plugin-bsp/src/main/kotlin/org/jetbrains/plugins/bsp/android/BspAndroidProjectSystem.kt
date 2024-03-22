package org.jetbrains.plugins.bsp.android

import com.android.tools.idea.model.ClassJarProvider
import com.android.tools.idea.projectsystem.AndroidModuleSystem
import com.android.tools.idea.projectsystem.AndroidProjectSystem
import com.android.tools.idea.projectsystem.LightResourceClassService
import com.android.tools.idea.projectsystem.ProjectSystemBuildManager
import com.android.tools.idea.projectsystem.ProjectSystemSyncManager
import com.android.tools.idea.projectsystem.SourceProvidersFactory
import com.android.tools.idea.projectsystem.getModuleSystem
import com.android.tools.idea.res.AndroidInnerClassFinder
import com.android.tools.idea.res.AndroidResourceClassPsiElementFinder
import com.android.tools.idea.res.ProjectLightResourceClassService
import com.intellij.facet.ProjectFacetManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElementFinder
import org.jetbrains.android.facet.AndroidFacet
import java.nio.file.Path

public class BspAndroidProjectSystem(private val project: Project) : AndroidProjectSystem {
  private val psiElementFinders =
    listOf(AndroidInnerClassFinder.INSTANCE, AndroidResourceClassPsiElementFinder(getLightResourceClassService()))

  private val syncManager = BspProjectSystemSyncManager(project)

  override fun allowsFileCreation(): Boolean = true

  override fun getAndroidFacetsWithPackageName(project: Project, packageName: String): Collection<AndroidFacet> {
    return ProjectFacetManager.getInstance(project)
      .getFacets(AndroidFacet.ID)
      .filter { it.module.getModuleSystem().getPackageName() == packageName }
  }

  override fun getBootClasspath(module: Module): Collection<String> = emptyList()

  override fun getBuildManager(): ProjectSystemBuildManager = BspProjectSystemBuildManager(project)

  override fun getClassJarProvider(): ClassJarProvider = BspClassJarProvider()

  override fun getDefaultApkFile(): VirtualFile? = null

  override fun getLightResourceClassService(): LightResourceClassService =
    ProjectLightResourceClassService.getInstance(project)

  override fun getModuleSystem(module: Module): AndroidModuleSystem = BspAndroidModuleSystem(module)

  override fun getPathToAapt(): Path = TODO("Not implemented")

  override fun getPsiElementFinders(): Collection<PsiElementFinder> = psiElementFinders

  override fun getSourceProvidersFactory(): SourceProvidersFactory = BspSourceProvidersFactory()

  override fun getSyncManager(): ProjectSystemSyncManager = syncManager

  override fun isNamespaceOrParentPackage(packageName: String): Boolean = false
}
