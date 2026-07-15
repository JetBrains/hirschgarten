package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.bazel.languages.projectview.PREFER_CLASS_JARS_OVER_SOURCELESS_JARS_KEY
import org.jetbrains.bazel.languages.projectview.SectionKey
import org.jetbrains.bazel.languages.projectview.sections.presets.BooleanScalarSection

internal class PreferClassJarsOverSourcelessJarsSection : BooleanScalarSection() {
  override val sectionKey: SectionKey<Boolean> = PREFER_CLASS_JARS_OVER_SOURCELESS_JARS_KEY
  override val doc: String = "In case library class jar is also present in the sources jars list, " +
    "prefer the class jar over the source jar."
}
