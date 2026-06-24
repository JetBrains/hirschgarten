package org.jetbrains.bazel.kotlin.ui.gutters

import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.kotlin.idea.codeInsight.KotlinRunLineMarkerHider

/**
 * Hides Kotlin's platform run marker when the Bazel contributor owns the same gutter element.
 *
 * [org.jetbrains.bazel.run.BazelRunConfigurationProducerSuppressor] suppresses run configuration producers after a
 * platform action is invoked; this extension prevents the duplicate platform marker/action from being offered at all.
 */
@ApiStatus.Internal
class BazelKotlinRunLineMarkerHider(
  private val bazelContributor: BazelKotlinRunLineMarkerContributor = BazelKotlinRunLineMarkerContributor(),
) : KotlinRunLineMarkerHider {
  override fun isDumbAware(): Boolean = true

  override fun shouldHideRunLineMarker(element: PsiElement): Boolean =
    bazelContributor.getInfo(element) != null
}
