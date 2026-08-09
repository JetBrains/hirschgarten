package org.jetbrains.bazel.ui.gutters

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.FakePsiElement
import org.jetbrains.annotations.NonNls
import org.jetbrains.bsp.protocol.BuildTarget

/**
 * A fake PSI element for cases where we want to get executor actions from Bazel target tool window, for example.
 */
internal class PsiBazelTarget(private val project: Project, val target: BuildTarget) : FakePsiElement() {
  override fun getParent(): PsiElement? = null

  override fun getContainingFile(): PsiFile? = null

  override fun getProject(): Project = project

  override fun getManager(): PsiManager = PsiManager.getInstance(project)

  override fun isValid(): Boolean = true

  override fun getText(): @NonNls String? {
    return super.getText()
  }
}
