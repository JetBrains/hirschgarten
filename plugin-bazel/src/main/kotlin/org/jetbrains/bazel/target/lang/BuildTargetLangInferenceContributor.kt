package org.jetbrains.bazel.target.lang

import com.intellij.lang.Language
import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.bsp.protocol.BuildTarget

interface BuildTargetLangInferenceContributor {
  fun getLanguagesInTarget(target: BuildTarget): Sequence<Language>

  companion object {
    val ep: ExtensionPointName<BuildTargetLangInferenceContributor> =
      ExtensionPointName.create("org.jetbrains.bazel.buildTargetLanguageInferenceContributor")

    fun getLanguagesInTarget(target: BuildTarget): Sequence<Language> =
      ep.extensionList.asSequence().flatMap { it.getLanguagesInTarget(target) }
  }
}
