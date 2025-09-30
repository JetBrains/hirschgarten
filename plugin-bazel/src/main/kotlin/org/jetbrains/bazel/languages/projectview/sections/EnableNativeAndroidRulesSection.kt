package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.bazel.languages.projectview.SectionKey
import org.jetbrains.bazel.languages.projectview.sections.presets.BooleanScalarSection

class EnableNativeAndroidRulesSection : BooleanScalarSection() {
  override val name = NAME
  override val sectionKey = KEY
  override val doc = "Enable native (non-starlarkified) Android rules"

  companion object {
    const val NAME = "enable_native_android_rules"
    val KEY = SectionKey<Boolean>(NAME)
  }
}
