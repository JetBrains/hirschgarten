package org.jetbrains.bazel.languages.starlark.completion.lookups

import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import javax.swing.Icon

abstract class StarlarkLookupElement(val name: String, private val wrapping: StarlarkQuote = StarlarkQuote.UNQUOTED) : LookupElement() {
  override fun getLookupString(): String = wrapping.wrap(name)

  override fun renderElement(presentation: LookupElementPresentation) {
    presentation.itemText = name
    presentation.icon = icon
  }

  override fun handleInsert(context: InsertionContext) {
    if (wrapping == StarlarkQuote.UNQUOTED) {
      super.handleInsert(context)
      return
    }

    adjustQuotation(context)
    context.commitDocument()
  }

  private fun adjustQuotation(context: InsertionContext) {
    val suffixElement = context.file.findElementAt(context.tailOffset) ?: return
    if (suffixElement.text.startsWith(wrapping.quote)) {
      val offset = suffixElement.textOffset
      context.document.deleteString(offset, offset + wrapping.quote.length)
    }
  }

  abstract val icon: Icon?
}
