package org.jetbrains.bazel.languages.projectview.language.sections

import org.jetbrains.bazel.languages.projectview.language.SectionKey
import org.jetbrains.bazel.languages.projectview.language.sections.presets.IntScalarSection

class AndroidMinSdkSection : IntScalarSection() {
  override val name = NAME
  override val sectionKey = KEY
  override val doc = "Override the minimum Android SDK version globally for the whole project."

  companion object {
    const val NAME = "android_min_sdk"
    val KEY = SectionKey<Int>(NAME)
  }
}
