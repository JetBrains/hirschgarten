package org.jetbrains.bsp.bazel.workspacecontext

import org.jetbrains.bsp.bazel.executioncontext.api.ExecutionContextEntityExtractor
import org.jetbrains.bsp.bazel.executioncontext.api.ExecutionContextSingletonEntity
import org.jetbrains.bsp.bazel.projectview.model.ProjectView

data class AndroidMinSdkSpec(override val value: Int?) : ExecutionContextSingletonEntity<Int?>()

internal object AndroidMinSdkSpecExtractor : ExecutionContextEntityExtractor<AndroidMinSdkSpec> {
  override fun fromProjectView(projectView: ProjectView): AndroidMinSdkSpec = AndroidMinSdkSpec(projectView.androidMinSdkSection?.value)
}
