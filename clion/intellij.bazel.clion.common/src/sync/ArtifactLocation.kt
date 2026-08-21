package org.jetbrains.bazel.clion.sync

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeContributor
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeEntry
import org.jetbrains.bazel.sync.workspace.persistence.type
import java.nio.file.Path

data class ArtifactLocation(
  /**
   * The root beneath which this file resides. Not equal to File.root.path,
   * normalizes relative paths and therefore also includes the bazel-bin prefix
   * for the artifact.
   */
  val rootPath: String,

  /**
   * The path of this file relative to its root. This excludes the
   * aforementioned root, i.e. configuration-specific fragments of the path.
   * This path can be different to File.short_path.
   */
  val relativePath: String,

  /**
   * True if this is a source file, i.e. it is not generated.
   */
  val isSource: Boolean,

  /**
   * Whether this artifact comes from an external repository. This might be
   * different from what the aspect reports, since this takes nested and
   * local modules into consideration.
   */
  val isExternal: Boolean,

  /**
   * The eagerly resolved path to the actual artifact on disc.
   */
  val resolvedPath: Path,
)

internal class ArtifactLocationWorkspaceTypeContributor : WorkspaceTypeContributor {

  override fun contribute(project: Project): List<WorkspaceTypeEntry> = buildList { type<ArtifactLocation>() }
}
