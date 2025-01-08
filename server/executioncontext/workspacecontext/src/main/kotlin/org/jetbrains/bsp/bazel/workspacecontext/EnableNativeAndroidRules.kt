package org.jetbrains.bsp.bazel.workspacecontext

import org.jetbrains.bsp.bazel.executioncontext.api.ExecutionContextEntityExtractor
import org.jetbrains.bsp.bazel.executioncontext.api.ExecutionContextSingletonEntity
import org.jetbrains.bsp.bazel.projectview.model.ProjectView

data class EnableNativeAndroidRules(override val value: Boolean) : ExecutionContextSingletonEntity<Boolean>()

internal object EnableNativeAndroidRulesExtractor :
  ExecutionContextEntityExtractor<EnableNativeAndroidRules> {
  override fun fromProjectView(projectView: ProjectView): EnableNativeAndroidRules =
    EnableNativeAndroidRules(projectView.enableNativeAndroidRules?.value ?: false)
}
