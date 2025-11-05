package org.jetbrains.bazel.sync.workspace.languages.java.source_root.projectview

import org.jetbrains.bazel.languages.projectview.ProjectView

val ProjectView.javaSROEnable: Boolean
  get() = getSection(JavaSROEnableSection.KEY) ?: false

val ProjectView.javaSROExcludePatterns: List<String>
  get() = getSection(JavaSROExcludePatternsSection.KEY) ?: emptyList()

val ProjectView.javaSROIncludePatterns: List<String>
  get() = getSection(JavaSROIncludePatternsSection.KEY) ?: emptyList()
