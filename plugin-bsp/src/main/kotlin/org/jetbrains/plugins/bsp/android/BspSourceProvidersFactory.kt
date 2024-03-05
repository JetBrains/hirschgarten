package org.jetbrains.plugins.bsp.android

import com.android.tools.idea.projectsystem.NamedIdeaSourceProvider
import com.android.tools.idea.projectsystem.NamedIdeaSourceProviderBuilder
import com.android.tools.idea.projectsystem.ScopeType
import com.android.tools.idea.projectsystem.SourceProviders
import com.android.tools.idea.projectsystem.SourceProvidersFactory
import com.android.tools.idea.projectsystem.SourceProvidersImpl
import com.android.tools.idea.projectsystem.emptySourceProvider
import com.intellij.openapi.module.Module
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.android.facet.createSourceProvidersForLegacyModule
import org.jetbrains.magicmetamodel.impl.workspacemodel.AndroidAddendum
import org.jetbrains.magicmetamodel.impl.workspacemodel.JavaModule
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import java.net.URI

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
    val androidAddendum = module.androidAddendum ?: return null
    val manifest = androidAddendum.manifest ?: return null
    return NamedIdeaSourceProviderBuilder
      .create(module.name, manifest.toString())
      .withScopeType(ScopeType.MAIN)
      .withResDirectoryUrls(androidAddendum.resourceFolders.map(URI::toString))
      .build()
  }
}

public val Module.androidAddendum: AndroidAddendum?
  get() {
    val magicMetaModel = MagicMetaModelService.getInstance(this.project).value
    val moduleDetails = magicMetaModel.getDetailsForTargetId(this.name)
    if (moduleDetails !is JavaModule) return null
    return moduleDetails.androidAddendum
  }
