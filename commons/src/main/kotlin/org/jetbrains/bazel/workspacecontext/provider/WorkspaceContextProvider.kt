package org.jetbrains.bazel.workspacecontext.provider

import org.jetbrains.bazel.projectview.generator.DefaultProjectViewGenerator
import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.projectview.parser.DefaultProjectViewParser
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.FeatureFlags
import java.nio.file.Path
import kotlin.io.path.notExists

interface WorkspaceContextProvider {
  /**
   * Prefer to pass [WorkspaceContext] as a parameter where possible instead of calling this method to avoid reparsing the project view.
   */
  fun readWorkspaceContext(): WorkspaceContext

  fun currentFeatureFlags(): FeatureFlags
}

class DefaultWorkspaceContextProvider(
  var workspaceRoot: Path,
  var projectViewPath: Path,
  dotBazelBspDirPath: Path,
  var featureFlags: FeatureFlags,
) : WorkspaceContextProvider {
  private val workspaceContextConstructor = WorkspaceContextConstructor(workspaceRoot, dotBazelBspDirPath, projectViewPath)

  override fun readWorkspaceContext(): WorkspaceContext {
    val projectView = ensureProjectViewExistsAndParse()

    return workspaceContextConstructor.construct(projectView)
  }

  override fun currentFeatureFlags(): FeatureFlags = featureFlags

  private fun ensureProjectViewExistsAndParse(): ProjectView {
    if (projectViewPath.notExists()) {
      generateEmptyProjectView()
    }
    return DefaultProjectViewParser(workspaceRoot).parse(projectViewPath)
  }

  private fun generateEmptyProjectView() {
    val emptyProjectView = ProjectView.Builder().build()
    DefaultProjectViewGenerator.generatePrettyStringAndSaveInFile(emptyProjectView, projectViewPath)
  }
}
