// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.bazel.searchEverywhere

import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributor
import com.intellij.ide.actions.searcheverywhere.WeightedSearchEverywhereContributor
import com.intellij.openapi.project.Project
import com.intellij.platform.searchEverywhere.SeItemsProvider
import com.intellij.platform.searchEverywhere.providers.SeAsyncContributorWrapper
import com.intellij.platform.searchEverywhere.providers.SeWrappedLegacyContributorItemsProviderFactory
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.BazelPluginConstants.SE_LABEL_PROVIDER_ID

@ApiStatus.Internal
class SeLabelProviderFactory : SeWrappedLegacyContributorItemsProviderFactory {
  override val id: String
    get() = SE_LABEL_PROVIDER_ID

  override suspend fun getItemsProvider(project: Project?, legacyContributor: SearchEverywhereContributor<Any>): SeItemsProvider? {
    if (project == null || legacyContributor !is WeightedSearchEverywhereContributor<Any>) return null
    return SeLabelProvider(SeAsyncContributorWrapper(legacyContributor))
  }
}
