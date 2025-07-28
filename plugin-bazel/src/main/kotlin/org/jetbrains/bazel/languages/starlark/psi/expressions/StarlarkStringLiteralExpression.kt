package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiReference
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkFilenameLoadValue
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadValue
import org.jetbrains.bazel.languages.starlark.references.BazelLabelReference
import org.jetbrains.bazel.languages.starlark.references.StarlarkClassnameReference
import org.jetbrains.bazel.languages.starlark.references.StarlarkLoadReference
import org.jetbrains.bazel.languages.starlark.references.StarlarkVisibilityReference
import org.jetbrains.bazel.languages.starlark.utils.StarlarkQuote
import javax.swing.Icon

fun getCompletionLookupElemenent(
  name: String,
  icon: Icon,
  priority: Double = 0.0,
): LookupElement =
  PrioritizedLookupElement.withPriority(
    LookupElementBuilder
      .create("\"" + name + "\"")
      .withIcon(icon)
      .withPresentableText(name)
      .withInsertHandler { context, _ ->
        // This prevents inserting a duplicate quote at the end.
        val document = context.document
        val offset = context.tailOffset
        if (offset < document.textLength && document.charsSequence[offset] == '"') {
          document.deleteString(offset, offset + 1)
        }
      },
    priority,
  )

class StarlarkStringLiteralExpression(node: ASTNode) : StarlarkBaseElement(node) {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitStringLiteralExpression(this)

  fun getStringContents(): String = getQuote().unwrap(text)

  fun getStringContentsOffset(): TextRange = getQuote().rangeWithinQuotes(text)

  fun getQuote(): StarlarkQuote = StarlarkQuote.ofString(text)

  override fun getReference(): PsiReference? {
    if (isClassnameValue()) return StarlarkClassnameReference(this)
    if (isInVisibilityList()) return StarlarkVisibilityReference(this)
    if (isLoadFilenameValue()) return BazelLabelReference(this, true)
    val loadAncestor = findLoadStatement() ?: return BazelLabelReference(this, true)
    val loadedFileNamePsi = loadAncestor.getLoadedFileNamePsi() ?: return null
    val loadedFileReference = BazelLabelReference(loadedFileNamePsi, true)
    return when (loadedFileNamePsi) {
      this -> loadedFileReference
      else -> StarlarkLoadReference(this, loadedFileReference)
    }
  }

  private fun isLoadFilenameValue(): Boolean = parent is StarlarkFilenameLoadValue

  private fun findLoadStatement(): StarlarkLoadStatement? = (parent as? StarlarkLoadValue)?.getLoadStatement()

  private fun isInVisibilityList(): Boolean =
    (parent is StarlarkListLiteralExpression && (parent.parent as? StarlarkNamedArgumentExpression)?.name == "visibility")

  private val classParametersList = listOf(
    "classname",
    "main_class",
    "test_class",
    "processor_class",
    "genclass",
    "tag_class"
  )
  private fun isClassnameValue(): Boolean = (parent as? StarlarkNamedArgumentExpression)?.name in classParametersList
}
