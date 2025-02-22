package org.jetbrains.bazel.workspacecontext

import org.jetbrains.bazel.executioncontext.api.ExecutionContextEntityExtractor
import org.jetbrains.bazel.executioncontext.api.ExecutionContextSingletonEntity
import org.jetbrains.bazel.projectview.model.ProjectView
import java.nio.file.Path

data class IdeJavaHomeOverrideSpec(override val value: Path?) : ExecutionContextSingletonEntity<Path?>()

internal object IdeJavaHomeOverrideSpecExtractor : ExecutionContextEntityExtractor<IdeJavaHomeOverrideSpec> {
  override fun fromProjectView(projectView: ProjectView): IdeJavaHomeOverrideSpec =
    IdeJavaHomeOverrideSpec(projectView.ideJavaHomeOverride?.value)
}
