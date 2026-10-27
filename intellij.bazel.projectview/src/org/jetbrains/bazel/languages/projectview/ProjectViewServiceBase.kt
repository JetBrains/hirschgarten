package org.jetbrains.bazel.languages.projectview

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewProblem
import java.nio.file.Path

@ApiStatus.Internal
abstract class ProjectViewServiceBase(protected val project: Project) : ProjectViewService {

  @Volatile
  final override var projectView: ProjectView = ProjectView.EMPTY
    private set

  override suspend fun forceReparseCurrentProjectViewFiles(onEachProblem: ((ProjectViewProblem) -> Unit)?) {
    loadProjectView(projectViewPath, onEachProblem)
  }

  protected fun loadProjectView(path: Path?, onEachProblem: ((ProjectViewProblem) -> Unit)? = null) {
    when (path) {
      null -> log.info("Project view path is missing. Falling back to the default project view.")
      else -> log.info("Using $path project view file.")
    }
    val parsed = path?.let { ProjectViewFactory.fromOrNull(project = project, source = it, onEachProblem = onEachProblem) }
    projectView = transformProjectView(parsed ?: defaultProjectView())
  }

  protected open fun defaultProjectView(): ProjectView = ProjectViewFactory.fromDefault(project)

  protected open fun transformProjectView(projectView: ProjectView): ProjectView = projectView
}

private val log = logger<ProjectViewServiceBase>()
