package org.jetbrains.bazel.languages.projectview.references

import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FilePathReferenceProvider
import com.intellij.util.ProcessingContext
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionItem
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionName

class ProjectViewFilePathReferenceContributor : PsiReferenceContributor() {
  //private fun sectionItemElement(sectionName: String) =
  //  psiElement()
  //    .withLanguage(ProjectViewLanguage)
  //    .withSuperParent(
  //      2,
  //      psiElement(ProjectViewPsiSection::class.java).withFirstChild(
  //        psiElement(ProjectViewPsiSectionName::class.java).withText(sectionName),
  //      ),
  //    )

  private fun sectionItemElement(sectionName: String) = psiElement(ProjectViewPsiSectionItem::class.java)

  override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
    registrar.registerReferenceProvider(
      sectionItemElement("directories"),
      FilePathReferenceProvider(),
      PsiReferenceRegistrar.LOWER_PRIORITY,
    )
  }
}
