package org.jetbrains.bazel.workspacecontext

import org.jetbrains.bazel.executioncontext.api.ExecutionContextEntityExtractor
import org.jetbrains.bazel.executioncontext.api.ExecutionContextSingletonEntity
import org.jetbrains.bazel.projectview.model.ProjectView

data class AndroidMinSdkSpec(override val value: Int?) : ExecutionContextSingletonEntity<Int?>()

internal object AndroidMinSdkSpecExtractor : ExecutionContextEntityExtractor<AndroidMinSdkSpec> {
  override fun fromProjectView(projectView: ProjectView): AndroidMinSdkSpec = AndroidMinSdkSpec(projectView.androidMinSdkSection?.value)
}
