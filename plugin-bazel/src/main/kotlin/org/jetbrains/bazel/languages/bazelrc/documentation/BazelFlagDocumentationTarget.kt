package org.jetbrains.bazel.languages.bazelrc.documentation

import com.intellij.markdown.utils.doc.DocMarkdownToHtmlConverter
import com.intellij.model.Pointer
import com.intellij.openapi.util.text.HtmlChunk.body
import com.intellij.openapi.util.text.HtmlChunk.br
import com.intellij.openapi.util.text.HtmlChunk.head
import com.intellij.openapi.util.text.HtmlChunk.html
import com.intellij.openapi.util.text.HtmlChunk.raw
import com.intellij.openapi.util.text.HtmlChunk.text
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.presentation.TargetPresentation
import org.jetbrains.bazel.languages.bazelrc.flags.BazelFlagSymbol

@Suppress("UnstableApiUsage")
class BazelFlagDocumentationTarget(symbol: BazelFlagSymbol) :
  DocumentationTarget,
  Pointer<BazelFlagDocumentationTarget> {
  val symbolPtr = symbol.createPointer()

  override fun createPointer() = this

  override fun dereference(): BazelFlagDocumentationTarget? = symbolPtr.dereference().documentationTarget

  override fun computePresentation(): TargetPresentation =
    symbolPtr.dereference().run {
      TargetPresentation
        .builder(flag.name)
        .presentation()
    }

  override fun computeDocumentation(): DocumentationResult? =
    symbolPtr.dereference().run {
      DocumentationResult.asyncDocumentation {
        val html =
          html().children(
            text(flag.name)
              .bold()
              .wrapWith(head()),
            br(),
            body().child(
              raw(
                DocMarkdownToHtmlConverter.convert(
                  project,
                  preprocessDescription(flag.description),
                ),
              ),
            ),
          )

        DocumentationResult.documentation(html.toString())
      }
    }

  companion object {
    // match `<space>--xxadsfas<space or end of word>`
    val reFlag = Regex("""(?<=\s)(--[\S=]+)(?=[\s$.])""")

    // match `<space>'--xxadsfas'<space or end of word>`
    val reFlagQuoted = Regex("""(?<=\s)('--['\S=]+')(?=[\s$.])""")

    // match a single newline in between non new-lines
    val reNewLine = Regex("""(?<!\n)\n(?=[^\n])""")

    // this will take the description and massage into a better markdown variant so that the transformed HTML is nicer
    fun preprocessDescription(description: String): String =
      description
        .replace(reNewLine, "") // -> _\n_ -> __
        .replace(reFlag) { "`${it.groups[1]?.value}`" } // -> _--asdfasd_ -> _`--asdfasd`_
        .replace(reFlagQuoted) { "`${it.groups[1]?.value}`" } // -> _'--asdfasd_' -> _`--asdfasd`_
  }
}
