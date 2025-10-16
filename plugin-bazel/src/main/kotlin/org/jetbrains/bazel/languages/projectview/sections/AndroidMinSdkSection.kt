package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.bazel.languages.projectview.SectionKey
import org.jetbrains.bazel.languages.projectview.sections.presets.IntScalarSection

class AndroidMinSdkSection : IntScalarSection() {
  override val name = NAME
  override val sectionKey = KEY
  override val doc = "Override the minimum Android SDK version globally for the whole project."

  companion object {
    const val NAME = "android_min_sdk"
    val KEY = SectionKey<Int>(NAME)
  }
}
