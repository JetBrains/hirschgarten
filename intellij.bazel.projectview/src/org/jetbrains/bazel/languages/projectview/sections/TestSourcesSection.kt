package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.bazel.languages.projectview.ListSection
import org.jetbrains.bazel.languages.projectview.SectionKey

internal class TestSourcesSection : ListSection<List<String>>() {
  override val name = NAME
  override val sectionKey = KEY

  /**
   * Copied from [OG plugin docs](https://ij.bazel.build/docs/project-views.html#test_sources)
   */
  override val doc =
    "A list of workspace-relative glob patterns matching directories. Determines which sources IntelliJ treats as a tests."

  override fun fromRawValues(rawValues: List<String>): List<String> = rawValues

  companion object {
    const val NAME = "test_sources"
    val KEY = SectionKey<List<String>>(NAME)
  }
}
