package org.jetbrains.bazel.kotlin.ui.gutters

import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.kotlin.idea.codeInsight.KotlinRunLineMarkerHider

/**
 * Hides Kotlin's standard run gutter marker when the Bazel contributor owns the same element.
 *
 * [org.jetbrains.bazel.run.BazelRunConfigurationProducerSuppressor] handles the later path where IntelliJ asks run
 * configuration producers to create a configuration for that element. This extension runs earlier, while Kotlin builds
 * its gutter marker, so the duplicate Kotlin action is not shown next to the Bazel action.
 */
@ApiStatus.Internal
class BazelKotlinRunLineMarkerHider(
  private val bazelContributor: BazelKotlinRunLineMarkerContributor = BazelKotlinRunLineMarkerContributor(),
) : KotlinRunLineMarkerHider {
  override fun isDumbAware(): Boolean = true

  override fun shouldHideRunLineMarker(element: PsiElement): Boolean =
    bazelContributor.getInfo(element) != null
}
