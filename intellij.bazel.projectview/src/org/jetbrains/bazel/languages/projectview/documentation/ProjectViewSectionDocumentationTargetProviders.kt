package org.jetbrains.bazel.languages.projectview.documentation

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.lang.documentation.QuickDocHighlightingHelper
import com.intellij.model.Pointer
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.DocumentationTargetProvider
import com.intellij.platform.backend.documentation.LookupElementDocumentationTargetProvider
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.psi.PsiFile
import org.jetbrains.bazel.languages.projectview.ProjectViewSection
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage
import org.jetbrains.bazel.languages.projectview.highlighting.ProjectViewHighlightingColors
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionName

internal class ProjectViewSectionDocumentationTargetProvider : DocumentationTargetProvider {
  override fun documentationTargets(file: PsiFile, offset: Int): List<DocumentationTarget> {
    val element = file.findElementAt(offset) ?: return emptyList()
    if (element.language !is ProjectViewLanguage) return emptyList()
    if (element.parent !is ProjectViewPsiSectionName) return emptyList()
    val section = ProjectViewSection.findByPsi(element) ?: return emptyList()
    return listOf(SectionDocumentationTarget(section))
  }
}

internal class ProjectViewSectionLookupElementDocumentationTargetProvider : LookupElementDocumentationTargetProvider {
  override fun documentationTarget(
    psiFile: PsiFile,
    lookupElement: LookupElement,
    offset: Int,
  ): DocumentationTarget? {
    val psiElement = psiFile.findElementAt(offset) ?: return null
    if (!projectViewSectionElement.accepts(psiElement)) return null
    val section = ProjectViewSection.findByName(lookupElement.lookupString) ?: return null
    return SectionDocumentationTarget(section)
  }

  companion object {
    private val projectViewSectionElement =
      psiElement()
        .withLanguage(ProjectViewLanguage)
        .withSuperParent(2, ProjectViewPsiFile::class.java)
        .inFile(psiElement(ProjectViewPsiFile::class.java))
  }
}

internal class SectionDocumentationTarget(
  private val section: ProjectViewSection<*>,
) : DocumentationTarget, Pointer<SectionDocumentationTarget> {
  override fun createPointer(): Pointer<SectionDocumentationTarget> = this
  override fun dereference(): SectionDocumentationTarget = this

  override fun computePresentation(): TargetPresentation = TargetPresentation.builder(section.key.name).presentation()

  override fun computeDocumentation(): DocumentationResult {
    val sectionName = QuickDocHighlightingHelper.getStyledFragment(section.key.name, ProjectViewHighlightingColors.KEYWORD)
    @Suppress("HardCodedStringLiteral")
    val documentation = "<pre>${sectionName}</pre><hr/> ${section.documentation}"
    return DocumentationResult.documentation(documentation)
  }
}
