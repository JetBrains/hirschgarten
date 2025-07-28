package org.jetbrains.bazel.workspacecontext.provider

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.findPsiFile
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile
import org.jetbrains.bazel.projectview.generator.DefaultProjectViewGenerator
import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.projectview.parser.DefaultProjectViewParser
import org.jetbrains.bazel.projectview.parser.splitter.ProjectViewRawSection
import org.jetbrains.bazel.projectview.parser.splitter.ProjectViewRawSections
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
  val project: Project? = null,
) : WorkspaceContextProvider {
  private val workspaceContextConstructor = WorkspaceContextConstructor(workspaceRoot, dotBazelBspDirPath, projectViewPath)

  private fun getSectionsFromPsiFile(): Map<String, List<String>>? {
    val virtualFile = VirtualFileManager.getInstance().findFileByNioPath(projectViewPath)!!
    val psiFile = virtualFile.findPsiFile(project!!) as? ProjectViewPsiFile
    val sections =
      psiFile
        ?.getSections()
        ?.map {
          val name = it.getKeyword()?.text ?: return@map null
          val items = it.getItems().map { item -> item.text }
          Pair(name, items)
        }?.filterNotNull()
    return sections?.associate { it }
  }

  override fun readWorkspaceContext(): WorkspaceContext {
    val projectView = ensureProjectViewExistsAndParse()
    val rawSections = getSectionsFromPsiFile()

    return workspaceContextConstructor.construct(projectView, rawSections!!)
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
