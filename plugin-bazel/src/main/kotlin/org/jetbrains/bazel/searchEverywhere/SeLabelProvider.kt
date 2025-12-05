// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.bazel.searchEverywhere

import com.intellij.ide.ui.icons.rpcId
import com.intellij.openapi.application.EDT
import com.intellij.openapi.util.Disposer
import com.intellij.platform.searchEverywhere.SeExtendedInfo
import com.intellij.platform.searchEverywhere.SeItem
import com.intellij.platform.searchEverywhere.SeItemsProvider
import com.intellij.platform.searchEverywhere.SeParams
import com.intellij.platform.searchEverywhere.presentations.SeItemPresentation
import com.intellij.platform.searchEverywhere.presentations.SeSimpleItemPresentation
import com.intellij.platform.searchEverywhere.providers.AsyncProcessor
import com.intellij.platform.searchEverywhere.providers.SeAsyncContributorWrapper
import com.intellij.platform.searchEverywhere.providers.getExtendedInfo
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
  val extendedInfo: SeExtendedInfo?,
  val isMultiSelectionSupported: Boolean,
) : SeItem {
  override fun weight(): Int = weight

  override suspend fun presentation(): SeItemPresentation =
    SeSimpleItemPresentation(
      iconId = BazelPluginIcons.bazel.rpcId(),
      text = legacyItem.displayName,
      extendedInfo = extendedInfo,
      isMultiSelectionSupported = isMultiSelectionSupported,
    )
}

@ApiStatus.Internal
class SeLabelProvider(private val contributorWrapper: SeAsyncContributorWrapper<Any>) : SeItemsProvider {
  private val contributor = contributorWrapper.contributor
  override val id: String get() = SE_LABEL_PROVIDER_ID
  override val displayName: String
    get() = contributor.fullGroupName

  override suspend fun collectItems(params: SeParams, collector: SeItemsProvider.Collector) {
    contributorWrapper.fetchElements(
      params.inputQuery,
      object : AsyncProcessor<Any> {
        override suspend fun process(item: Any, weight: Int): Boolean {
          if (item !is LabelWithPreview) return true
          return collector.put(
            SeLabelItem(
              item,
              weight,
              contributor.getExtendedInfo(item),
              contributorWrapper.contributor.isMultiSelectionSupported,
            ),
          )
        }
      },
    )
  }

  override suspend fun itemSelected(
    item: SeItem,
    modifiers: Int,
    searchText: String,
  ): Boolean {
    val legacyItem = (item as? SeLabelItem)?.legacyItem ?: return false

    return withContext(Dispatchers.EDT) {
      contributor.processSelectedItem(legacyItem, modifiers, searchText)
    }
  }

  override suspend fun canBeShownInFindResults(): Boolean = contributor.showInFindResults()

  override fun dispose() {
    Disposer.dispose(contributorWrapper)
  }
}
