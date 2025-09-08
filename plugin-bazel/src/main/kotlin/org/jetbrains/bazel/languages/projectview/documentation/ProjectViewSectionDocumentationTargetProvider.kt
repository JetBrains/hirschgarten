package org.jetbrains.bazel.languages.projectview.documentation

import com.intellij.model.Pointer
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.DocumentationTargetProvider
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.createSmartPointer
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage
import org.jetbrains.bazel.languages.projectview.ProjectViewSections
import org.jetbrains.bazel.languages.projectview.Section
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionName

class ProjectViewSectionDocumentationTargetProvider : DocumentationTargetProvider {
  @Suppress("UnstableApiUsage")
  private class SectionDocumentationTarget(
    val element: PsiElement,
    val originalElement: PsiElement?,
    val section: Section<*>,
  ) : DocumentationTarget {
    override fun createPointer(): Pointer<out DocumentationTarget> {
      val elementPtr = element.createSmartPointer()
      val originalElementPtr = originalElement?.createSmartPointer()
      return Pointer {
        val element = elementPtr.dereference() ?: return@Pointer null
        val originalElement = originalElementPtr?.dereference()
        SectionDocumentationTarget(element, originalElement, section)
      }
    }

    override fun computePresentation(): TargetPresentation = TargetPresentation.builder(section.name).presentation()

    override fun computeDocumentation(): DocumentationResult? {
      val doc = section.doc ?: return null
      return DocumentationResult.documentation(doc)
    }
  }

  override fun documentationTargets(file: PsiFile, offset: Int): List<DocumentationTarget> {
    val element = file.findElementAt(offset) ?: return emptyList()
    if (element.language !is ProjectViewLanguage) return emptyList()
    if (element.parent !is ProjectViewPsiSectionName) return emptyList()
    val section = ProjectViewSections.getSectionByName(element.text) ?: return emptyList()
    return listOf(SectionDocumentationTarget(element, null, section))
  }
}
