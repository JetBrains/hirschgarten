package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiReference
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadValue
import org.jetbrains.bazel.languages.starlark.references.BazelLabelReference
import org.jetbrains.bazel.languages.starlark.references.StarlarkLoadReference
import org.jetbrains.bazel.languages.starlark.utils.StarlarkQuote

class StarlarkStringLiteralExpression(node: ASTNode) : StarlarkBaseElement(node) {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitStringLiteralExpression(this)

  fun getStringContents(): String = getQuote().unwrap(text)

  fun getStringContentsOffset(): TextRange = getQuote().rangeWithinQuotes(text)

  fun getQuote(): StarlarkQuote = StarlarkQuote.ofString(text)

  override fun getReference(): PsiReference? {
    val loadAncestor = findLoadStatement() ?: return BazelLabelReference(this, true)
    val loadedFileNamePsi = loadAncestor.getLoadedFileNamePsi() ?: return null
    val loadedFileReference = BazelLabelReference(loadedFileNamePsi, true)
    return when (loadedFileNamePsi) {
      this -> loadedFileReference
      else -> StarlarkLoadReference(this, loadedFileReference)
    }
  }

  private fun findLoadStatement(): StarlarkLoadStatement? = (parent as? StarlarkLoadValue)?.getLoadStatement()
}
