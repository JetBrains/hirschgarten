package org.jetbrains.bazel.kotlin.target

import com.intellij.lang.Language
import org.jetbrains.bazel.target.lang.BuildTargetLangInferenceContributor
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import org.jetbrains.kotlin.idea.KotlinLanguage

class KotlinBuildTargetLangInferenceContributor : BuildTargetLangInferenceContributor {
  override fun getLanguagesInTarget(target: BuildTarget): Sequence<Language> {
    return sequence {
      if (target.data is KotlinBuildTarget) {
        yield(KotlinLanguage.INSTANCE)
      }
    }
  }
}
