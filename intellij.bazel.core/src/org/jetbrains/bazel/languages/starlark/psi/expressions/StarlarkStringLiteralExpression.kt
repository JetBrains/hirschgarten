package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.intellij.psi.impl.source.tree.LeafElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.injection.StarlarkStringLiteralEscaper
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkArgumentList
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkFilenameLoadValue
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadValue
import org.jetbrains.bazel.languages.starlark.references.BazelLabelReference
import org.jetbrains.bazel.languages.starlark.references.StarlarkLoadReference
import org.jetbrains.bazel.languages.starlark.references.StarlarkVisibilityReference
import org.jetbrains.bazel.languages.starlark.utils.StarlarkQuote
import javax.swing.Icon

@ApiStatus.Internal
fun getCompletionLookupElement(
  name: String,
  icon: Icon,
  priority: Double = 0.0,
  presentableText: String? = null,
  tailText: String = "",
  /**
   * isExpressionFinished indicates whether the lookup element finishes the expression:
   * `true` if the lookup element completes the entire expression (or its final part),
   * `false` if the lookup element completes only a part of the expression (for example,
   * when completing the package path to a class with subsequent subpackages)
   */
  isExpressionFinished: Boolean = true,
): LookupElement =
  PrioritizedLookupElement.withPriority(
    LookupElementBuilder
      .create("\"" + name + "\"")
      .withIcon(icon)
      .withPresentableText(presentableText ?: name)
      .withTailText(tailText, true)
      .withInsertHandler { context, _ ->
        // This prevents inserting a duplicate quote at the end.
        val document = context.document
        val offset = context.tailOffset
        if (offset < document.textLength && document.charsSequence[offset] == '"') {
          document.deleteString(offset, offset + 1)
        }

        // If the expression is not finished, continue completion inside the quotation mark.
        if (!isExpressionFinished) {
          context.editor.caretModel.moveToOffset(offset - 1)
          AutoPopupController.getInstance(context.project).scheduleAutoPopup(context.editor)
        }
      },
    priority,
  )

@ApiStatus.Internal
class StarlarkStringLiteralExpression(node: ASTNode) :
  StarlarkBaseElement(node),
  PsiLanguageInjectionHost {

  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitStringLiteralExpression(this)

  fun getStringContents(): String = getStringContentsOffset().substring(text)

  /** Range of the string contents (without the prefix and the quotes) relative to this element. */
  fun getStringContentsOffset(): TextRange {
    val literalText = text
    val quote = getQuote().quote
    val start = getPrefixLength() + quote.length
    val isTerminated = literalText.length >= start + quote.length && literalText.endsWith(quote)
    val end = if (isTerminated) literalText.length - quote.length else literalText.length
    return TextRange(start, maxOf(start, end))
  }

  fun getQuote(): StarlarkQuote = StarlarkQuote.ofString(text.substring(getPrefixLength()))

  /** Whether this is a raw string literal (`r"..."`), in which backslashes are not escape characters. */
  fun isRaw(): Boolean = text.substring(0, getPrefixLength()).contains('r', ignoreCase = true)

  /** Length of the optional `r`, `b`, `br` or `rb` prefix in front of the opening quote. */
  private fun getPrefixLength(): Int = text.indexOfFirst { it == '"' || it == '\'' }.coerceAtLeast(0)

  override fun isValidHost(): Boolean = true

  override fun updateText(text: String): PsiLanguageInjectionHost {
    val stringNode = node.firstChildNode as? LeafElement ?: return this
    stringNode.replaceWithText(text)
    return this
  }

  override fun createLiteralTextEscaper(): LiteralTextEscaper<out PsiLanguageInjectionHost> = StarlarkStringLiteralEscaper(this)

  /**
   * Detects whether this string literal is the value of the target name attribute.
   * It ensures that this value represents the whole name - any string concatenation or variable usage is rejected.
   */
  fun isTargetNameAttributeValue(): Boolean {
    val parent = parent as? StarlarkNamedArgumentExpression ?: return false
    if (!parent.isNameArgument()) return false
    val argList = parent.parent as? StarlarkArgumentList ?: return false
    if (argList.parent !is StarlarkCallExpression) return false
    val file = containingFile as? StarlarkFile ?: return false
    return file.isBuildFile()
  }

  override fun getReferences(): Array<PsiReference> {
    val contributedReferences = ReferenceProvidersRegistry.getReferencesFromProviders(this)
    if (contributedReferences.isNotEmpty()) {
      return contributedReferences
    }
    return super.getReferences()
  }

  override fun getReference(): PsiReference? {
    if (isInVisibilityList()) return StarlarkVisibilityReference(this)
    if (isLoadFilenameValue()) return BazelLabelReference(this, true)
    if (isUseExtensionArgument()) {
      val loadedFileNamePsi = useExtensionLoadedFileName() ?: return null
      val loadedFileReference = BazelLabelReference(loadedFileNamePsi, true)
      val reference = when (loadedFileNamePsi) {
        this -> loadedFileReference
        else -> StarlarkLoadReference(this, loadedFileReference)
      }
      return reference
    }
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
    (
      parent is StarlarkListLiteralExpression && (parent.parent as? StarlarkNamedArgumentExpression)?.name in
        listOf(
          "visibility",
          "default_visibility",
        )
    )

  fun argumentListContext(): StarlarkArgumentList? = (parent as? StarlarkArgumentExpression)?.parent as? StarlarkArgumentList

  fun isUseExtensionArgument(): Boolean = (argumentListContext()?.parent as? StarlarkCallExpression)?.getCalledFunctionName() == "use_extension"

  fun useExtensionLoadedFileName(): StarlarkStringLiteralExpression? {
    val arguments = argumentListContext()?.getArguments() ?: return null
    if (arguments.isEmpty()) return null
    return arguments[0].firstChild as? StarlarkStringLiteralExpression
  }

}
