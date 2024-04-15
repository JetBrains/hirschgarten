package org.jetbrains.plugins.bsp.android

import com.android.tools.idea.projectsystem.NamedIdeaSourceProvider
import com.android.tools.idea.projectsystem.NamedIdeaSourceProviderBuilder
import com.android.tools.idea.projectsystem.ScopeType
import com.android.tools.idea.projectsystem.SourceProviders
import com.android.tools.idea.projectsystem.SourceProvidersFactory
import com.android.tools.idea.projectsystem.SourceProvidersImpl
import com.android.tools.idea.projectsystem.emptySourceProvider
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.android.facet.createSourceProvidersForLegacyModule
import org.jetbrains.workspacemodel.entities.androidAddendumEntity

public class BspSourceProvidersFactory : SourceProvidersFactory {
  override fun createSourceProvidersFor(facet: AndroidFacet): SourceProviders {
    val sourceProvider = createSourceProviderForModule(facet) ?: return createSourceProvidersForLegacyModule(facet)

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

  private fun createSourceProviderForModule(facet: AndroidFacet): NamedIdeaSourceProvider? {
    val module = facet.module
    val androidAddendum = module.moduleEntity?.androidAddendumEntity ?: return null
    val manifest = androidAddendum.manifest ?: return null
    return NamedIdeaSourceProviderBuilder
      .create(module.name, manifest.url)
      .withScopeType(ScopeType.MAIN)
      .withResDirectoryUrls(androidAddendum.resourceFolders.map { it.url })
      .withAssetsDirectoryUrls(androidAddendum.assetFolders.map { it.url })
      .build()
  }
}
