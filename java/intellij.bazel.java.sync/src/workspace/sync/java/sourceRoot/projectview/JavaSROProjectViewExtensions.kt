package org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.projectview

import org.jetbrains.bazel.languages.projectview.ProjectView

internal val ProjectView.javaSROEnable: Boolean
  get() = getSection(JavaSROEnableSection.KEY) ?: false

internal val ProjectView.javaSROPatterns: List<String>
  get() = getSection(JavaSROPatternsSection.KEY) ?: emptyList()
