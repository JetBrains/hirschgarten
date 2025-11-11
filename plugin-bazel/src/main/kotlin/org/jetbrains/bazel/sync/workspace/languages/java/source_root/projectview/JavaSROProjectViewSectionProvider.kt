package org.jetbrains.bazel.sync.workspace.languages.java.source_root.projectview

import org.jetbrains.bazel.languages.projectview.ProjectViewSectionProvider
import org.jetbrains.bazel.languages.projectview.Section

class JavaSROProjectViewSectionProvider : ProjectViewSectionProvider {
  override val sections: List<Section<*>> = listOf(
    JavaSROEnableSection(),
    JavaSROPatternsSection(),
  )
}
