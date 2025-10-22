package org.jetbrains.bazel.languages.projectview.documentation

import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.DocumentationTargetProvider
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.bazel.languages.bazelrc.documentation.BazelFlagSymbolLessDocumentationTarget
import org.jetbrains.bazel.languages.bazelrc.flags.Flag
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionItem

class ProjectViewFlagDocumentationTargetProvider : DocumentationTargetProvider {
  override fun documentationTargets(file: PsiFile, offset: Int): List<DocumentationTarget> {
    val element = file.findElementAt(offset) ?: return emptyList()
    if (element.language !is ProjectViewLanguage) return emptyList()
    if (element !is LeafPsiElement) return emptyList()
    if (element.parent !is ProjectViewPsiSectionItem) return emptyList()
    val flag = Flag.byName(element.text) ?: return emptyList()
    return listOf(BazelFlagSymbolLessDocumentationTarget(element, null, flag))
  }
}
