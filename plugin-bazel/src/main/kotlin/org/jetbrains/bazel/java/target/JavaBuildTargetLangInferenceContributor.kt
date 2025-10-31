package org.jetbrains.bazel.java.target

import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import org.jetbrains.bazel.target.lang.BuildTargetLangInferenceContributor
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.JvmBuildTarget

class JavaBuildTargetLangInferenceContributor : BuildTargetLangInferenceContributor {
  override fun getLanguagesInTarget(target: BuildTarget): Sequence<Language> {
    return sequence {
      if (target.data is JvmBuildTarget) {
        yield(JavaLanguage.INSTANCE)
      }
    }
  }
}
