package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference
import org.jetbrains.bazel.languages.starlark.completion.lookups.StarlarkQuote
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadValue
import org.jetbrains.bazel.languages.starlark.references.BazelLabelReference
import org.jetbrains.bazel.languages.starlark.references.StarlarkLoadReference

class StarlarkStringLiteralExpression(node: ASTNode) : StarlarkBaseElement(node) {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitStringLiteralExpression(this)

  fun getStringContents(): String? =
    node.findChildByType(StarlarkTokenTypes.STRING)?.text?.let {
      if (it.startsWith("\"\"\"")) {
        it.removeSurrounding("\"\"\"")
      } else {
        it.removeSurrounding("\"")
      }
    }

  fun getQuote(): StarlarkQuote =
    node.findChildByType(StarlarkTokenTypes.STRING)?.text?.let { string ->
      when {
        string.startsWith("\"\"\"") -> StarlarkQuote.TRIPLE
        string.startsWith("\"") -> StarlarkQuote.DOUBLE
        else -> StarlarkQuote.UNQUOTED
      }
    } ?: StarlarkQuote.UNQUOTED

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
