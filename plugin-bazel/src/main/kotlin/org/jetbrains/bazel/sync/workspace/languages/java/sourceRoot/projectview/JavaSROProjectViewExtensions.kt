package org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.projectview

import org.jetbrains.bazel.languages.projectview.ProjectView

val ProjectView.javaSROEnable: Boolean
  get() = getSection(JavaSROEnableSection.KEY) ?: false

val ProjectView.javaSROPatterns: List<String>
  get() = getSection(JavaSROPatternsSection.KEY) ?: emptyList()
