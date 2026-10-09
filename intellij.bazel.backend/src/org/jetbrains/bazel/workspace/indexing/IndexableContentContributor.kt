package org.jetbrains.bazel.workspace.indexing

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
interface IndexableContentContributor {

  fun getIndexableContent(project: Project): IndexableContent

  companion object {
    val ep: ExtensionPointName<IndexableContentContributor> = ExtensionPointName("org.jetbrains.bazel.indexableContentContributor")
  }
}
