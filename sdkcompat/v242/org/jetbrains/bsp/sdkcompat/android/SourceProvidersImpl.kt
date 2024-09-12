package org.jetbrains.bsp.sdkcompat.android

import com.android.tools.idea.projectsystem.NamedIdeaSourceProvider
import com.android.tools.idea.projectsystem.ScopeType
import com.android.tools.idea.projectsystem.SourceProvidersImpl
import com.android.tools.idea.projectsystem.emptySourceProvider

// v243: Dropped some parameters and added some new ones
fun sourceProviderImpl(sourceProvider: NamedIdeaSourceProvider): SourceProvidersImpl =
  SourceProvidersImpl(
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
