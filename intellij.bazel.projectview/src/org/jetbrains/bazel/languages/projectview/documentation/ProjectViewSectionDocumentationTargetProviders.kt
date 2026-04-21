package org.jetbrains.bazel.languages.projectview.documentation

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.model.Pointer
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.DocumentationTargetProvider
import com.intellij.platform.backend.documentation.LookupElementDocumentationTargetProvider
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.psi.PsiFile
import org.jetbrains.bazel.languages.projectview.ProjectViewSections
import org.jetbrains.bazel.languages.projectview.Section
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionName

internal class ProjectViewSectionDocumentationTargetProvider : DocumentationTargetProvider {
  override fun documentationTargets(file: PsiFile, offset: Int): List<DocumentationTarget> {
    val element = file.findElementAt(offset) ?: return emptyList()
    if (element.language !is ProjectViewLanguage) return emptyList()
    if (element.parent !is ProjectViewPsiSectionName) return emptyList()
    val section = ProjectViewSections.getSectionByName(element.text) ?: return emptyList()
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
    val section = ProjectViewSections.getSectionByName(lookupElement.lookupString) ?: return null
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
  private val section: Section<*>,
) : DocumentationTarget, Pointer<SectionDocumentationTarget> {
  override fun createPointer(): Pointer<SectionDocumentationTarget> = this
  override fun dereference(): SectionDocumentationTarget = this

  override fun computePresentation(): TargetPresentation = TargetPresentation.builder(section.name).presentation()

  override fun computeDocumentation(): DocumentationResult? {
    val doc = section.doc ?: return null
    return DocumentationResult.documentation(doc)
  }
}
