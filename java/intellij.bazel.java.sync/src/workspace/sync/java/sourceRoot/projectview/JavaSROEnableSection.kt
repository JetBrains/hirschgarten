package org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.projectview

import org.jetbrains.bazel.languages.projectview.SectionKey
import org.jetbrains.bazel.languages.projectview.sections.presets.BooleanScalarSection

internal class JavaSROEnableSection : BooleanScalarSection() {
  override val sectionKey: SectionKey<Boolean> = JAVA_SOURCE_ROOT_OPTIMIZATION_KEY
  override val doc: String = "Enable java source root optimization"
}
