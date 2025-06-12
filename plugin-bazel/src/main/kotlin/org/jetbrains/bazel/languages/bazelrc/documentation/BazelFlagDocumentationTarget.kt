package org.jetbrains.bazel.languages.bazelrc.documentation

import com.intellij.markdown.utils.doc.DocMarkdownToHtmlConverter
import com.intellij.model.Pointer
import com.intellij.openapi.util.text.HtmlChunk.body
import com.intellij.openapi.util.text.HtmlChunk.fragment
import com.intellij.openapi.util.text.HtmlChunk.head
import com.intellij.openapi.util.text.HtmlChunk.html
import com.intellij.openapi.util.text.HtmlChunk.raw
import com.intellij.openapi.util.text.HtmlChunk.text
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.presentation.TargetPresentation
import org.jetbrains.bazel.languages.bazelrc.flags.BazelFlagSymbol
import org.jetbrains.bazel.languages.bazelrc.flags.Flag

@Suppress("UnstableApiUsage")
class BazelFlagDocumentationTarget(symbol: BazelFlagSymbol) :
  DocumentationTarget,
  Pointer<BazelFlagDocumentationTarget> {
  val symbolPtr = symbol.createPointer()

  override fun createPointer() = this

  override fun dereference(): BazelFlagDocumentationTarget? = symbolPtr.dereference().documentationTarget

  override fun computePresentation(): TargetPresentation =
    symbolPtr.dereference().run {
      TargetPresentation.builder(flag.title()).presentation()
    }

  override fun computeDocumentation(): DocumentationResult? =
    symbolPtr.dereference().run {
      DocumentationResult.asyncDocumentation {
        val markdownText = flagToDocumentationMarkdownText(flag)

        val body = raw(DocMarkdownToHtmlConverter.convert(project, markdownText))
        val fragment =
          fragment(
            text(flag.title()).bold().wrapWith(head()),
            body.wrapWith(body()),
          )

        DocumentationResult.documentation(fragment.wrapWith(html()).toString())
      }
    }

  companion object {
    // match `<space>--xxadsfas<space or end of word>`
    val reFlag = Regex("""(?<=\s)(--[\S=]+)(?=[\s$.])""")

    // match `<space>'--xxadsfas'<space or end of word>`
    val reFlagQuoted = Regex("""(?<=\s)('--['\S=]+')(?=[\s$.])""")

    // this will take the description and massage it into a better markdown variant so that the transformed HTML is nicer
    fun Flag.help(): String =
      option.help
        .trimIndent()
        .replace(reFlag) { "`${it.groups[1]?.value}`" } // -> _--asdfasd_ -> _`--asdfasd`_
        .replace(reFlagQuoted) { "`${it.groups[1]?.value}`" } // -> _'--asdfasd_' -> _`--asdfasd`_

    fun Flag.title(): String =
      this
        .takeIf { it is Flag.Boolean }
        ?.let { "[no]${option.name}" }
        ?: option.name

    fun Flag.expandsTo(): String =
      option
        .expandsTo
        .takeUnless { it.isEmpty() }
        ?.let {
          """
        |**Expands to**:
        |```commandline
        |${option.expandsTo.joinToString(" ")}
        |```
          """.trimMargin()
        }
        ?: ""

    fun Flag.type(): String =
      option
        .valueHelp
        .takeUnless(String::isEmpty)
        ?.let { """**type**${"\n"}: `$it`""" }
        ?: ""

    fun Flag.default(): String =
      option
        .defaultValue
        .takeUnless(String::isEmpty)
        ?.let { it.replace("\n+", "") }
        ?.let { """**default**${"\n"}: `$it`""" }
        ?: ""

    fun Flag.oldName(): String =
      option
        .oldName
        .takeUnless(String::isEmpty)
        ?.let { """**old name**${"\n"}: `$it`""" }
        ?: ""

    fun Flag.allowMultiple(): String =
      option
        .allowMultiple
        .takeIf { it }
        ?.let { """**can be used multiples times**""" }
        ?: ""

    fun Flag.effects(): String =
      option
        .effectTags
        .takeUnless { it.isEmpty() }
        ?.let { tags -> tags.joinToString(", ") { """_${it.name.lowercase()}_""" } }
        ?.let { """**Effects**: $it""" }
        ?: ""

    fun Flag.metadataTags(): String =
      option
        .metadataTags
        .takeUnless { it.isEmpty() }
        ?.let { tags -> tags.joinToString(", ") { """_${it.name.lowercase()}_""" } }
        ?.let { """**Metadata tags**: $it""" } ?: ""

    fun Flag.commands(): String =
      option
        .commands
        .takeUnless { it.isEmpty() }
        ?.let { commands -> commands.joinToString(", ") { it.lowercase() } }
        ?.let { """**Commands**: `$it`""" } ?: ""

    fun flagToDocumentationMarkdownText(flag: Flag): String {
      val markdownText =
        """
          |
          |${flag.type()}
          |
          |${flag.default()}
          |
          |${flag.oldName()}
          |
          |${flag.allowMultiple()}
          |
          |${flag.help()}
          |
          |${flag.expandsTo()}
          |
          |${flag.effects()}
          |
          |${flag.metadataTags()}
          |
          |${flag.commands()}
        """.trimMargin()
      return markdownText
    }
  }
}
