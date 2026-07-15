package org.jetbrains.bazel.languages.projectview.sections

import com.intellij.openapi.util.io.toNioPathOrNull
import org.jetbrains.bazel.languages.projectview.DOT_IDEA_DIRECTORY_LOCATION_KEY
import org.jetbrains.bazel.languages.projectview.ScalarSection
import org.jetbrains.bazel.languages.projectview.SectionKey
import java.nio.file.Path

internal class DotIdeaDirectoryLocationSection : ScalarSection<Path?>() {

  override val sectionKey: SectionKey<Path?>
    get() = DOT_IDEA_DIRECTORY_LOCATION_KEY

  override fun fromRawValue(rawValue: String): Path? = rawValue.toNioPathOrNull()
}
