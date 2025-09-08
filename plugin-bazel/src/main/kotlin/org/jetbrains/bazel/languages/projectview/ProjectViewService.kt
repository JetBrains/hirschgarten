package org.jetbrains.bazel.languages.projectview

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bazel.workspacecontext.provider.WorkspaceContextProvider
import org.jetbrains.bsp.protocol.FeatureFlags
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.notExists

@Service(Service.Level.PROJECT)
class ProjectViewService(private val project: Project) : WorkspaceContextProvider {
  private var cachedProjectView: ProjectView? = null
  private var cachedProjectViewPath: Path? = null
  private var cachedWorkspaceContext: WorkspaceContext? = null
  
  var featureFlags: FeatureFlags = FeatureFlags()
  
  // Temporary compatibility methods for interfacing with server
  var dotBazelBspDirPath: Path? = null
  
  /**
   * Gets the current ProjectView, parsing it if necessary.
   * Prefer to pass ProjectView as a parameter where possible instead of calling this method to avoid reparsing.
   */
  fun currentProjectView(): ProjectView {
    val projectViewPath = findProjectViewPath()
    
    // Return cached version if path hasn't changed and we have a cached result
    if (cachedProjectView != null && cachedProjectViewPath == projectViewPath) {
      return cachedProjectView!!
    }
    
    // Parse the project view
    val projectView = parseProjectView(projectViewPath)
    
    // Cache the result
    cachedProjectView = projectView
    cachedProjectViewPath = projectViewPath
    
    return projectView
  }
  
  /**
   * Clears the cached project view, forcing a reparse on next access.
   */
  fun invalidateCache() {
    cachedProjectView = null
    cachedProjectViewPath = null
    cachedWorkspaceContext = null
  }
  
  // WorkspaceContextProvider implementation for server compatibility
  override fun readWorkspaceContext(): WorkspaceContext {
    val projectView = currentProjectView()
    val projectViewPath = cachedProjectViewPath!!
    
    // Return cached version if we have it and it's still valid
    if (cachedWorkspaceContext != null) {
      return cachedWorkspaceContext!!
    }
    
    // Convert ProjectView to WorkspaceContext
    val workspaceRoot = project.rootDir.toNioPath()
    val dotBazelBspDir = dotBazelBspDirPath ?: workspaceRoot.resolve(".bazelbsp")
    
    val workspaceContext = ProjectViewToWorkspaceContextConverter.convert(
      projectView = projectView,
      dotBazelBspDirPath = dotBazelBspDir,
      workspaceRoot = workspaceRoot,
      projectViewPath = projectViewPath
    )
    
    // Cache the result
    cachedWorkspaceContext = workspaceContext
    
    return workspaceContext
  }
  
  override fun currentFeatureFlags(): FeatureFlags = featureFlags
  
  private fun findProjectViewPath(): Path {
    val rootDir = project.rootDir.toNioPath()
    val projectViewPath = rootDir.resolve(".bazelproject")
    
    if (projectViewPath.notExists()) {
      generateEmptyProjectView(projectViewPath)
    }
    
    return projectViewPath
  }
  
  private fun parseProjectView(projectViewPath: Path): ProjectView {
    val virtualFile = VirtualFileManager.getInstance().findFileByNioPath(projectViewPath)
      ?: error("Could not find project view file at $projectViewPath")
      
    val psiFile = PsiManager.getInstance(project).findFile(virtualFile) as? ProjectViewPsiFile
      ?: error("Could not parse project view file at $projectViewPath")
      
    return ProjectView.fromProjectViewPsiFile(psiFile)
  }
  
  private fun generateEmptyProjectView(projectViewPath: Path) {
    // Create an empty .bazelproject file
    projectViewPath.toFile().writeText("")
  }
  
  companion object {
    @JvmStatic
    fun getInstance(project: Project): ProjectViewService = project.getService(ProjectViewService::class.java)
  }
}