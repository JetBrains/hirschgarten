package org.jetbrains.bazel.languages.bazelversion.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import org.jetbrains.bazel.languages.bazelversion.BazelVersionFileType
import org.jetbrains.bazel.languages.bazelversion.psi.BazelVersionFile
import org.jetbrains.bazel.languages.bazelversion.service.BazelVersionCheckerService

class BazelVersionInspection : LocalInspectionTool() {
  override fun isAvailableForFile(file: PsiFile): Boolean {
    return file.fileType is BazelVersionFileType
  }

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return BazelVersionVisitor(holder)
  }

  class BazelVersionVisitor(val holder: ProblemsHolder) : PsiElementVisitor() {
    override fun visitFile(psiFile: PsiFile) {
      val file = psiFile as? BazelVersionFile ?: return
      val currentVersion = file.bazelVersion ?: return
      val latestVersion = holder.project.service<BazelVersionCheckerService>()
        .getLatestBazelVersion() ?: return
      if (latestVersion.version.isGreaterThan(currentVersion.version)) {
        holder.registerProblem(
          psiFile, "New bazel version available: $latestVersion",
          BazelVersionQuickFix(),
        )
      }
    }
  }

  class BazelVersionQuickFix() : LocalQuickFix {
    override fun getFamilyName(): String = "Update version"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
      val manager = PsiDocumentManager.getInstance(project)
      val doc = manager.getDocument(descriptor.psiElement as PsiFile) ?: return
      val version = project.service<BazelVersionCheckerService>().getLatestBazelVersion() ?: return
      WriteCommandAction.runWriteCommandAction(project) {
        doc.setText(version.toString())
        manager.commitDocument(doc)
      }
    }
  }
}
