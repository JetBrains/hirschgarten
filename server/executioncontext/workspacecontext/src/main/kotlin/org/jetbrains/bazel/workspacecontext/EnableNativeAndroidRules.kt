package org.jetbrains.bazel.workspacecontext

import org.jetbrains.bazel.executioncontext.api.ExecutionContextEntityExtractor
import org.jetbrains.bazel.executioncontext.api.ExecutionContextSingletonEntity
import org.jetbrains.bazel.projectview.model.ProjectView

data class EnableNativeAndroidRules(override val value: Boolean) : ExecutionContextSingletonEntity<Boolean>()

internal object EnableNativeAndroidRulesExtractor :
  ExecutionContextEntityExtractor<EnableNativeAndroidRules> {
  override fun fromProjectView(projectView: ProjectView): EnableNativeAndroidRules =
    EnableNativeAndroidRules(projectView.enableNativeAndroidRules?.value ?: false)
}
