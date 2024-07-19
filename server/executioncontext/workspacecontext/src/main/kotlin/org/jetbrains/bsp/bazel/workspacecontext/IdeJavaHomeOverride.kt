package org.jetbrains.bsp.bazel.workspacecontext

import java.nio.file.Path
import org.jetbrains.bsp.bazel.executioncontext.api.ExecutionContextEntityExtractor
import org.jetbrains.bsp.bazel.executioncontext.api.ExecutionContextSingletonEntity
import org.jetbrains.bsp.bazel.projectview.model.ProjectView

data class IdeJavaHomeOverrideSpec(
    override val value: Path?,
) : ExecutionContextSingletonEntity<Path?>()

internal object IdeJavaHomeOverrideSpecExtractor :
    ExecutionContextEntityExtractor<IdeJavaHomeOverrideSpec> {

  override fun fromProjectView(projectView: ProjectView): IdeJavaHomeOverrideSpec =
      IdeJavaHomeOverrideSpec(projectView.ideJavaHomeOverride?.value)
}
