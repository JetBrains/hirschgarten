package org.jetbrains.bazel.workspacecontext.provider

import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.workspacecontext.EnableNativeAndroidRules

internal object EnableNativeAndroidRulesExtractor :
  WorkspaceContextEntityExtractor<EnableNativeAndroidRules> {
  override fun fromProjectView(projectView: ProjectView): EnableNativeAndroidRules =
    EnableNativeAndroidRules(projectView.enableNativeAndroidRules?.value ?: false)
}
