package org.jetbrains.bazel.golang.resolve

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
interface BazelGoPackageNavigationProvider {
  /** Returns the main target of [goPackage] in its BUILD file, or the BUILD file itself. See [BazelGoPackage.getMainLabel]. */
  fun findTargetDefinitionPsi(goPackage: BazelGoPackage): PsiElement?

  companion object {
    private val EP_NAME: ExtensionPointName<BazelGoPackageNavigationProvider> =
      ExtensionPointName.create("org.jetbrains.bazel.goPackageNavigationProvider")

    fun findTargetDefinitionPsi(goPackage: BazelGoPackage): PsiElement? =
      EP_NAME.extensionList.firstNotNullOfOrNull { it.findTargetDefinitionPsi(goPackage) }
  }
}
