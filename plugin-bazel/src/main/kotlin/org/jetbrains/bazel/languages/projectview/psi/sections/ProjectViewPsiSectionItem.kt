package org.jetbrains.bazel.languages.projectview.psi.sections

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FilePathReferenceProvider
import com.intellij.psi.util.startOffset
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewBaseElement
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewElementVisitor

class ProjectViewPsiSectionItem(node: ASTNode) : ProjectViewBaseElement(node) {
  override fun acceptVisitor(visitor: ProjectViewElementVisitor) {
    visitor.visitSectionItem(this)
  }

  override fun getReferences(): Array<out PsiReference?> {
    val element = this
    val text = element.text
    val offset = element.startOffset
    val references = FilePathReferenceProvider().getReferencesByElement(this, text, 0, true)
    return references
  }
}
