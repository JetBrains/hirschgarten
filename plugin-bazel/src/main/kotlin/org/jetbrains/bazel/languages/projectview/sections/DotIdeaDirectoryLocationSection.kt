package org.jetbrains.bazel.languages.projectview.sections

import com.intellij.openapi.util.io.toNioPathOrNull
import org.jetbrains.bazel.languages.projectview.ScalarSection
import org.jetbrains.bazel.languages.projectview.SectionKey
import java.nio.file.Path

internal class DotIdeaDirectoryLocationSection : ScalarSection<Path>() {

  override val name: String
    get() = NAME
  override val sectionKey: SectionKey<Path>
    get() = KEY

  override fun fromRawValue(rawValue: String): Path? = rawValue.toNioPathOrNull()

  companion object {
    const val NAME = "dot_idea_directory_location"
    val KEY = SectionKey<Path>(NAME)
  }
}
