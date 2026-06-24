package org.jetbrains.bazel.kotlin.ui.gutters

import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.kotlin.idea.codeInsight.KotlinRunLineMarkerHider

@ApiStatus.Internal
class BazelKotlinRunLineMarkerHider(
  private val bazelContributor: BazelKotlinRunLineMarkerContributor = BazelKotlinRunLineMarkerContributor(),
) : KotlinRunLineMarkerHider {
  override fun isDumbAware(): Boolean = true

  override fun shouldHideRunLineMarker(element: PsiElement): Boolean =
    bazelContributor.getInfo(element) != null
}
