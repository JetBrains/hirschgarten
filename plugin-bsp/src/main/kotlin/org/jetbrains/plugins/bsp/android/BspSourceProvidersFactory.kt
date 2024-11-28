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
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.util.moduleEntity
import org.jetbrains.plugins.bsp.workspacemodel.entities.androidAddendumEntity

class BspSourceProvidersFactory : SourceProvidersFactory {
  override fun createSourceProvidersFor(facet: AndroidFacet): SourceProviders {
    val sourceProvider = createSourceProviderForModule(facet) ?: return createSourceProvidersForLegacyModule(facet)

    throw NoSuchMethodError("This should fail Qodana Android run")

    return SourceProvidersImpl(
      mainIdeaSourceProvider = sourceProvider,
      currentSourceProviders = listOf(sourceProvider),
      currentTestFixturesSourceProviders = listOf(sourceProvider),
      currentAndSomeFrequentlyUsedInactiveSourceProviders = listOf(sourceProvider),
      mainAndFlavorSourceProviders = listOf(sourceProvider),
      generatedSources = emptySourceProvider(ScopeType.MAIN),
      currentHostTestSourceProviders = emptyMap(),
      currentDeviceTestSourceProviders = emptyMap(),
      generatedHostTestSources = emptyMap(),
      generatedDeviceTestSources = emptyMap(),
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
      .withResDirectoryUrls(androidAddendum.resourceDirectories.map { it.url })
      .withAssetsDirectoryUrls(androidAddendum.assetsDirectories.map { it.url })
      .build()
  }
}
