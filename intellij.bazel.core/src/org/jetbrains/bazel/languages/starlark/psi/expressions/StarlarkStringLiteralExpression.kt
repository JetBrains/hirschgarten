package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.lang.ASTNode
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.util.TextRange
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.intellij.psi.impl.source.tree.LeafElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.StarlarkReferenceSupport
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkArgumentList
import org.jetbrains.bazel.languages.starlark.utils.StarlarkQuote

@ApiStatus.Internal
class StarlarkStringLiteralExpression(node: ASTNode) :
  StarlarkBaseElement(node),
  PsiLanguageInjectionHost {

  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitStringLiteralExpression(this)

  fun getStringContents(): String = getQuote().unwrap(getTextWithQuotes())

  fun getStringContentsOffset(): TextRange = getQuote().rangeWithinQuotes(getTextWithQuotes()).shiftRight(getRawPrefixLength())

  fun getQuote(): StarlarkQuote = StarlarkQuote.ofString(getTextWithQuotes())

  fun getTextWithQuotes(): String = text.drop(getRawPrefixLength())

  /** Whether this is a raw string literal (`r"..."`), in which backslashes are not escape characters. */
  fun isRaw(): Boolean = text.substring(0, getRawPrefixLength()).contains('r', ignoreCase = true)

  private fun getRawPrefixLength(): Int {
    val prefix = text.takeWhile { !it.isQuote() }
    return if (prefix in ALLOWED_STRING_PREFIXES) prefix.length else 0
  }

  private fun Char?.isQuote(): Boolean = this == '\'' || this == '"'

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

  override fun getReference(): PsiReference? = serviceOrNull<StarlarkReferenceSupport>()?.getReference(this)

  fun argumentListContext(): StarlarkArgumentList? = (parent as? StarlarkArgumentExpression)?.parent as? StarlarkArgumentList

  fun isUseExtensionArgument(): Boolean = (argumentListContext()?.parent as? StarlarkCallExpression)?.getCalledFunctionName() == "use_extension"

  fun useExtensionLoadedFileName(): StarlarkStringLiteralExpression? {
    val arguments = argumentListContext()?.getArguments() ?: return null
    if (arguments.isEmpty()) return null
    return arguments[0].firstChild as? StarlarkStringLiteralExpression
  }
}

private val ALLOWED_STRING_PREFIXES = setOf("", "r", "b", "rb", "br") // capital letters are not allowed here in Starlark
