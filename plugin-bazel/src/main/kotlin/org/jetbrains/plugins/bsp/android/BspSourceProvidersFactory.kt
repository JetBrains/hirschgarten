package org.jetbrains.plugins.bsp.android

import com.android.tools.idea.projectsystem.NamedIdeaSourceProvider
import com.android.tools.idea.projectsystem.NamedIdeaSourceProviderBuilder
import com.android.tools.idea.projectsystem.ScopeType
import com.android.tools.idea.projectsystem.SourceProviders
import com.android.tools.idea.projectsystem.SourceProvidersFactory
import com.android.tools.idea.projectsystem.SourceProvidersImpl
import com.android.tools.idea.projectsystem.TestComponentType.DeviceTest
import com.android.tools.idea.projectsystem.TestComponentType.HostTest
import com.android.tools.idea.projectsystem.emptySourceProvider
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.android.facet.createSourceProvidersForLegacyModule
import org.jetbrains.plugins.bsp.target.moduleEntity
import org.jetbrains.plugins.bsp.workspacemodel.entities.androidAddendumEntity

class BspSourceProvidersFactory : SourceProvidersFactory {
  override fun createSourceProvidersFor(facet: AndroidFacet): SourceProviders {
    val sourceProvider = createSourceProviderForModule(facet) ?: return createSourceProvidersForLegacyModule(facet)

    val mainIdeaSourceProvider = sourceProvider
    val currentSourceProviders = listOf(sourceProvider)
    val currentHostTestSourceProviders = emptyMap<HostTest, List<NamedIdeaSourceProvider>>()
    val currentDeviceTestSourceProviders = emptyMap<DeviceTest, List<NamedIdeaSourceProvider>>()
    val currentTestFixturesSourceProviders = listOf(sourceProvider)
    val allVariantAllArtifactsSourceProviders = listOf(sourceProvider)
    val currentAndSomeFrequentlyUsedInactiveSourceProviders = listOf(sourceProvider)
    val mainAndFlavorSourceProviders = listOf(sourceProvider)
    val generatedSources = emptySourceProvider(ScopeType.MAIN)
    val generatedHostTestSources = emptyMap<HostTest, NamedIdeaSourceProvider>()
    val generatedDeviceTestSources = emptyMap<DeviceTest, NamedIdeaSourceProvider>()
    val generatedTestFixturesSources = emptySourceProvider(ScopeType.TEST_FIXTURES)

    return SourceProvidersImpl(
      mainIdeaSourceProvider,
      currentSourceProviders,
      currentHostTestSourceProviders,
      currentDeviceTestSourceProviders,
      currentTestFixturesSourceProviders,
      allVariantAllArtifactsSourceProviders,
      currentAndSomeFrequentlyUsedInactiveSourceProviders,
      mainAndFlavorSourceProviders,
      generatedSources,
      generatedHostTestSources,
      generatedDeviceTestSources,
      generatedTestFixturesSources,
    )
  }

  private fun createSourceProviderForModule(facet: AndroidFacet): NamedIdeaSourceProvider? {
    val module = facet.module
    val androidAddendum = module.moduleEntity?.androidAddendumEntity ?: return null
    val manifest = androidAddendum.manifest ?: return null
    return NamedIdeaSourceProviderBuilder
      .create(module.name, manifest.url)
      .withScopeType(ScopeType.MAIN)
      .withResDirectoryUrls(androidAddendum.resourceDirectories.map { it.url })
      .withAssetsDirectoryUrls(androidAddendum.assetsDirectories.map { it.url })
      .build()
  }
}
