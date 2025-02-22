package org.jetbrains.bazel.workspacecontext

import org.jetbrains.bazel.projectview.generator.DefaultProjectViewGenerator
import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.projectview.parser.DefaultProjectViewParser
import java.nio.file.Path
import kotlin.io.path.notExists

interface WorkspaceContextProvider {
  fun currentWorkspaceContext(): WorkspaceContext
}

class DefaultWorkspaceContextProvider(
  private val workspaceRoot: Path,
  private val projectViewPath: Path,
  dotBazelBspDirPath: Path,
) : WorkspaceContextProvider {
  private val workspaceContextConstructor = WorkspaceContextConstructor(workspaceRoot, dotBazelBspDirPath)

  override fun currentWorkspaceContext(): WorkspaceContext {
    val projectView = ensureProjectViewExistsAndParse()

    return workspaceContextConstructor.construct(projectView)
  }

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
