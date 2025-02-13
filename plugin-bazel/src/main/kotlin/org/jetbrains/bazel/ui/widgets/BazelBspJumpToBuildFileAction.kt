package org.jetbrains.bazel.ui.widgets

import com.intellij.icons.AllIcons
import com.intellij.ide.util.EditorHelper
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.remoteDev.util.addPathSuffix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.commons.label.SingleTarget
import org.jetbrains.bazel.commons.label.label
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.plugins.bsp.action.SuspendableAction
import org.jetbrains.plugins.bsp.action.getPsiFile
import org.jetbrains.plugins.bsp.target.targetUtils
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo
import java.net.URI
import kotlin.io.path.toPath

class BazelBspJumpToBuildFileAction(private val buildTargetInfo: BuildTargetInfo?) :
  SuspendableAction({ BazelPluginBundle.message("widget.open.build.file") }, AllIcons.Actions.OpenNewTab) {
  // Used in plugin.xml
  @Suppress("UNUSED")
  constructor() : this(null)

  override fun update(project: Project, e: AnActionEvent) {
    if (buildTargetInfo != null) return

    e.presentation.isEnabledAndVisible = (e.place == ActionPlaces.EDITOR_POPUP || e.place == ActionPlaces.KEYBOARD_SHORTCUT) &&
      CommonDataKeys.PSI_FILE.getData(e.dataContext)?.virtualFile?.let {
        project.targetUtils.getTargetsForFile(it).isNotEmpty()
      } == true
  }

  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    val buildFile =
      readAction {
        val buildTargetInfo = this.buildTargetInfo ?: getBuildTargetInfo(project, e.getPsiFile()) ?: return@readAction null
        findBuildFile(project, buildTargetInfo)
      } ?: return
    withContext(Dispatchers.EDT) {
      EditorHelper.openInEditor(buildFile, true, true)
    }
  }

  private fun getBuildTargetInfo(project: Project, file: PsiFile?): BuildTargetInfo? {
    val virtualFile = file?.virtualFile ?: return null
    val targetId = project.targetUtils.getTargetsForFile(virtualFile).firstOrNull() ?: return null
    return project.targetUtils.getBuildTargetInfoForId(targetId)
  }
}

fun findBuildFile(project: Project, buildTargetInfo: BuildTargetInfo): PsiElement? {
  val baseDirectoryPath = buildTargetInfo.baseDirectory?.let { URI.create(it) } ?: return null
  val buildFilePath = baseDirectoryPath.addPathSuffix("BUILD").toPath()
  val buildBazelFilePath = baseDirectoryPath.addPathSuffix("BUILD.bazel").toPath()
  val virtualFileManager = VirtualFileManager.getInstance()
  val virtualFile =
    virtualFileManager.findFileByNioPath(buildFilePath) ?: virtualFileManager.findFileByNioPath(buildBazelFilePath) ?: return null
  val buildFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return null

  // Try to jump to a specific target
  val target = buildTargetInfo.id.label().target as? SingleTarget
  if (target != null) {
    (buildFile as? StarlarkFile)?.findRuleTarget(target.targetName)?.let { return it }
  }
  // Fallback to the BUILD file itself
  return buildFile
}
