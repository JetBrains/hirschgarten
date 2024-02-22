package org.jetbrains.plugins.bsp.android

import com.android.tools.idea.model.AndroidModel
import com.android.tools.idea.model.ClassJarProvider
import com.android.tools.idea.project.DefaultModuleSystem
import com.android.tools.idea.projectsystem.AndroidModuleSystem
import com.android.tools.idea.projectsystem.AndroidProjectSystem
import com.android.tools.idea.projectsystem.LightResourceClassService
import com.android.tools.idea.projectsystem.ProjectSystemBuildManager
import com.android.tools.idea.projectsystem.ProjectSystemSyncManager
import com.android.tools.idea.projectsystem.ScopeType
import com.android.tools.idea.projectsystem.SourceProviders
import com.android.tools.idea.projectsystem.SourceProvidersFactory
import com.android.tools.idea.projectsystem.SourceProvidersImpl
import com.android.tools.idea.projectsystem.emptySourceProvider
import com.android.tools.idea.projectsystem.getModuleSystem
import com.android.tools.idea.res.AndroidInnerClassFinder
import com.android.tools.idea.res.AndroidResourceClassPsiElementFinder
import com.intellij.facet.ProjectFacetManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElementFinder
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.android.facet.createSourceProvidersForLegacyModule
import java.nio.file.Path

public class BspAndroidProjectSystem(private val project: Project) : AndroidProjectSystem {
  private val psiElementFinders =
    listOf(AndroidInnerClassFinder.INSTANCE, AndroidResourceClassPsiElementFinder(getLightResourceClassService()))

  override fun allowsFileCreation(): Boolean = true

  override fun getAndroidFacetsWithPackageName(project: Project, packageName: String): Collection<AndroidFacet> {
    return ProjectFacetManager.getInstance(project)
      .getFacets(AndroidFacet.ID)
      .filter { it.module.getModuleSystem().getPackageName() == packageName }
  }

  override fun getBootClasspath(module: Module): Collection<String> = emptyList()

  override fun getBuildManager(): ProjectSystemBuildManager = BspProjectSystemBuildManager()

  override fun getClassJarProvider(): ClassJarProvider = BspClassJarProvider()

  override fun getDefaultApkFile(): VirtualFile? = null

  override fun getLightResourceClassService(): LightResourceClassService =
    BspLightResourceClassService.getInstance(project)

  override fun getModuleSystem(module: Module): AndroidModuleSystem = DefaultModuleSystem(module)

  override fun getPathToAapt(): Path = TODO("Not implemented")

  override fun getPsiElementFinders(): Collection<PsiElementFinder> = psiElementFinders

  override fun getSourceProvidersFactory(): SourceProvidersFactory = object : SourceProvidersFactory {
    override fun createSourceProvidersFor(facet: AndroidFacet): SourceProviders {
      val model = AndroidModel.get(facet) as? BspAndroidModel ?: return createSourceProvidersForLegacyModule(facet)
      val sourceProvider = model.sourceProvider

      return SourceProvidersImpl(
        mainIdeaSourceProvider = sourceProvider,
        currentSourceProviders = listOf(sourceProvider),
        currentUnitTestSourceProviders = listOf(sourceProvider),
        currentAndroidTestSourceProviders = listOf(sourceProvider),
        currentTestFixturesSourceProviders = listOf(sourceProvider),
        currentAndSomeFrequentlyUsedInactiveSourceProviders = listOf(sourceProvider),
        mainAndFlavorSourceProviders = listOf(sourceProvider),
        generatedSources = emptySourceProvider(ScopeType.MAIN),
        generatedUnitTestSources = emptySourceProvider(ScopeType.UNIT_TEST),
        generatedAndroidTestSources = emptySourceProvider(ScopeType.ANDROID_TEST),
        generatedTestFixturesSources = emptySourceProvider(ScopeType.TEST_FIXTURES),
      )
    }
  }

  override fun getSyncManager(): ProjectSystemSyncManager = BspProjectSystemSyncManager(project)

  override fun isNamespaceOrParentPackage(packageName: String): Boolean = false
}
