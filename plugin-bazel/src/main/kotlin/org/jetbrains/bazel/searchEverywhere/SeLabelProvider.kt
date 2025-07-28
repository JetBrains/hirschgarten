// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.bazel.searchEverywhere

import com.intellij.ide.actions.searcheverywhere.FoundItemDescriptor
import com.intellij.ide.ui.icons.rpcId
import com.intellij.ide.util.DelegatingProgressIndicator
import com.intellij.openapi.application.EDT
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.coroutineToIndicator
import com.intellij.openapi.util.Disposer
import com.intellij.platform.searchEverywhere.SeItem
import com.intellij.platform.searchEverywhere.SeItemPresentation
import com.intellij.platform.searchEverywhere.SeItemsProvider
import com.intellij.platform.searchEverywhere.SeParams
import com.intellij.platform.searchEverywhere.SeSimpleItemPresentation
import com.intellij.platform.searchEverywhere.providers.AsyncProcessor
import com.intellij.platform.searchEverywhere.providers.SeAsyncWeightedContributorWrapper
import com.intellij.platform.searchEverywhere.providers.getExtendedDescription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.config.BazelPluginConstants.SE_LABEL_PROVIDER_ID
import org.jetbrains.bazel.ui.widgets.LabelSearchEverywhereContributor.LabelWithPreview

@ApiStatus.Internal
class SeLabelItem(
  val legacyItem: LabelWithPreview,
  private val weight: Int,
  val extendedDescription: String?,
  val isMultiSelectionSupported: Boolean,
) : SeItem {
  override fun weight(): Int = weight

  override suspend fun presentation(): SeItemPresentation =
    SeSimpleItemPresentation(
      iconId = BazelPluginIcons.bazel.rpcId(),
      text = legacyItem.displayName,
      extendedDescription = extendedDescription,
      isMultiSelectionSupported = isMultiSelectionSupported,
    )
}

@ApiStatus.Internal
class SeLabelProvider(private val contributorWrapper: SeAsyncWeightedContributorWrapper<Any>) : SeItemsProvider {
  override val id: String get() = SE_LABEL_PROVIDER_ID
  override val displayName: String
    get() = contributorWrapper.contributor.fullGroupName

  override suspend fun collectItems(params: SeParams, collector: SeItemsProvider.Collector) {
    coroutineToIndicator {
      val indicator = DelegatingProgressIndicator(ProgressManager.getGlobalProgressIndicator())
      contributorWrapper.fetchWeightedElements(
        params.inputQuery,
        indicator,
        object : AsyncProcessor<FoundItemDescriptor<Any>> {
          override suspend fun process(t: FoundItemDescriptor<Any>): Boolean {
            val legacyItem = t.item ?: return true
            if (legacyItem !is LabelWithPreview) return true
            val weight = t.weight
            return collector.put(
              SeLabelItem(
                legacyItem,
                weight,
                getExtendedDescription(legacyItem),
                contributorWrapper.contributor.isMultiSelectionSupported,
              ),
            )
          }
        },
      )
    }
  }

  fun getExtendedDescription(item: LabelWithPreview): String? = contributorWrapper.contributor.getExtendedDescription(item)

  override suspend fun itemSelected(
    item: SeItem,
    modifiers: Int,
    searchText: String,
  ): Boolean {
    val legacyItem = (item as? SeLabelItem)?.legacyItem ?: return false

    return withContext(Dispatchers.EDT) {
      contributorWrapper.contributor.processSelectedItem(legacyItem, modifiers, searchText)
    }
  }

  override suspend fun canBeShownInFindResults(): Boolean = contributorWrapper.contributor.showInFindResults()

  override fun dispose() {
    Disposer.dispose(contributorWrapper)
  }
}
