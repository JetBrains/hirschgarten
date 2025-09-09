package org.jetbrains.bazel.languages.projectview

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bazel.workspacecontext.provider.WorkspaceContextProvider
import org.jetbrains.bsp.protocol.FeatureFlags
import java.nio.file.Path

/**
 * Adapter service that bridges ProjectView to the legacy WorkspaceContext system.
 * This provides backward compatibility while the server layer migrates to use ProjectView directly.
 */
@Service(Service.Level.PROJECT)
class ProjectViewWorkspaceContextProvider(private val project: Project) : WorkspaceContextProvider {
  var featureFlags: FeatureFlags = FeatureFlags()
  var dotBazelBspDirPath: Path? = null

  override fun readWorkspaceContext(): WorkspaceContext {
    val projectViewService = ProjectViewService.getInstance(project)
    val projectView = projectViewService.getProjectView()

    val workspaceRoot = project.rootDir.toNioPath()
    val dotBazelBspDir = dotBazelBspDirPath ?: workspaceRoot.resolve(".bazelbsp")

    return ProjectViewToWorkspaceContextConverter.convert(
      projectView = projectView,
      dotBazelBspDirPath = dotBazelBspDir,
      workspaceRoot = workspaceRoot,
    )
  }

  override fun currentFeatureFlags(): FeatureFlags = featureFlags

  companion object {
    @JvmStatic
    fun getInstance(project: Project): ProjectViewWorkspaceContextProvider =
      project.getService(ProjectViewWorkspaceContextProvider::class.java)
  }
}
