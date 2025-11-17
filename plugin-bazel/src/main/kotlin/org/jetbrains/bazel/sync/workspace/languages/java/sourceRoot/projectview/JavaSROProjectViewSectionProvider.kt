package org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.projectview

import org.jetbrains.bazel.languages.projectview.ProjectViewSectionProvider
import org.jetbrains.bazel.languages.projectview.Section

class JavaSROProjectViewSectionProvider : ProjectViewSectionProvider {
  override val sections: List<Section<*>> = listOf(
    JavaSROEnableSection(),
    JavaSROPatternsSection(),
  )
}
