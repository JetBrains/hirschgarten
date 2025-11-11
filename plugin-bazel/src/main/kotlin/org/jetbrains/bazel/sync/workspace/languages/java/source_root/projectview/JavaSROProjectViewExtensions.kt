package org.jetbrains.bazel.sync.workspace.languages.java.source_root.projectview

import org.jetbrains.bazel.languages.projectview.ProjectView

val ProjectView.javaSROEnable: Boolean
  get() = getSection(JavaSROEnableSection.KEY) ?: false

val ProjectView.javaSROPatterns: List<String>
  get() = getSection(JavaSROPatternsSection.KEY) ?: emptyList()
