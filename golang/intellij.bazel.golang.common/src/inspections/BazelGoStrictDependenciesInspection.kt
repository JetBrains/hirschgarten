package org.jetbrains.bazel.golang.inspections

import com.goide.psi.GoFile
import com.goide.psi.GoVisitor
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.codeInspection.util.IntentionName
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.golang.resolve.BazelGoBundle
import org.jetbrains.bazel.golang.resolve.getGoPackageEntity
import org.jetbrains.bazel.golang.sync.GAZELLE_TARGET_OVERRIDE_KEY
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.projectview.gazelleTarget
import org.jetbrains.bazel.languages.projectview.projectView
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.sync.ProjectSyncScope
import org.jetbrains.bazel.sync.ProjectSyncService
import org.jetbrains.bazel.workspace.canonicalRepoNameToPath
import org.jetbrains.bazel.workspacemodel.entities.ImportPathId

@ApiStatus.Internal
class BazelGoStrictDependenciesInspection : LocalInspectionTool() {
  override fun isDumbAware(): Boolean = true

  override fun isAvailableForFile(file: PsiFile): Boolean =
    file.project.isBazelProject && file is GoFile

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    val gazelleTarget = getGazelleTarget(holder.project)

    return object : GoVisitor() {
      override fun visitGoFile(file: GoFile) {
        val goPackageEntity = getGoPackageEntity(file) ?: return
        val currentSnapshot = file.project.workspaceModel.currentSnapshot
        for (importSpec in file.imports) {
          val importPath = importSpec.path
          // Import path is inside strict dependencies, all good
          if (importPath in goPackageEntity.directDepsImportPaths) continue
          // Import path doesn't exist in the project at all, this is flagged by the Go plugin already
          if (!currentSnapshot.contains(ImportPathId(importPath))) continue
          // Import path exists in the project, but not in direct dependencies
          holder.registerProblem(
            importSpec,
            BazelGoBundle.message(
              "go.strict.deps.indirect.dependency",
              importPath,
            ),
            *gazelleTarget?.let { arrayOf(RunGazelleQuickFix(file.project, gazelleTarget)) }.orEmpty(),
          )
        }
      }
    }
  }

  private fun getGazelleTarget(project: Project): Label? {
    project.projectView().gazelleTarget?.let { return it }
    val hasGazelle = project.canonicalRepoNameToPath.keys.any { it.startsWith("gazelle") }
    return if (hasGazelle) {
      // Use the default Gazelle location if none was specified in the project view file
      Label.parse("//:gazelle")
    }
    else {
      null
    }
  }
}

private class RunGazelleQuickFix(
  private val project: Project,
  private val gazelleTarget: Label,
) : LocalQuickFix {
  override fun getName(): @IntentionName String {
    return BazelGoBundle.message("quickfix.run.gazelle", gazelleTarget.toShortString(project))
  }

  override fun getFamilyName(): @IntentionFamilyName String =
    BazelGoBundle.message("quickfix.run.gazelle.family.name")

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    BazelCoroutineService.getInstance(project).start {
      project.putUserData(GAZELLE_TARGET_OVERRIDE_KEY, gazelleTarget)
      try {
        project.service<ProjectSyncService>().sync(ProjectSyncScope.Full(build = false, phased = false))
      }
      finally {
        project.putUserData(GAZELLE_TARGET_OVERRIDE_KEY, null)
      }
    }
  }

  override fun startInWriteAction(): Boolean = false

  override fun generatePreview(project: Project, previewDescriptor: ProblemDescriptor): IntentionPreviewInfo =
    IntentionPreviewInfo.EMPTY
}
