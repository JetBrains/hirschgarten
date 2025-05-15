package org.jetbrains.bazel.languages.bazelquery.documentation

import com.intellij.markdown.utils.doc.DocMarkdownToHtmlConverter
import com.intellij.model.Pointer
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.HtmlChunk.body
import com.intellij.openapi.util.text.HtmlChunk.fragment
import com.intellij.openapi.util.text.HtmlChunk.head
import com.intellij.openapi.util.text.HtmlChunk.html
import com.intellij.openapi.util.text.HtmlChunk.raw
import com.intellij.openapi.util.text.HtmlChunk.text
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.presentation.TargetPresentation
import org.jetbrains.bazel.languages.bazelquery.functions.BazelQueryFunction
import org.jetbrains.bazel.languages.bazelquery.functions.BazelQueryFunctionSymbol

@Suppress("UnstableApiUsage")
class BazelQueryFunctionDocumentationTarget(symbol: BazelQueryFunctionSymbol, val project: Project) :
  DocumentationTarget,
  Pointer<BazelQueryFunctionDocumentationTarget> {
  private val function = symbol.function

  override fun createPointer() = this

  override fun dereference(): BazelQueryFunctionDocumentationTarget? = this

  override fun computePresentation(): TargetPresentation = TargetPresentation.builder(function.name).presentation()

  override fun computeDocumentation(): DocumentationResult? =
    DocumentationResult.asyncDocumentation {
      val markdownText =
        """
          |${function.description}
          |
          |**Arguments:**
          |${function.argumentsMarkdown()}
          |
          |**Example Usage:**
          ```
            ${if (function is BazelQueryFunction.SimpleFunction) function.exampleUsage else "N/A"}
          ```
        """.trimMargin()

      val body =
        raw(
          DocMarkdownToHtmlConverter.convert(project, markdownText),
        )
      val fragment =
        fragment(
          text(function.name).bold().wrapWith(head()),
          body.wrapWith(body()),
        )

      DocumentationResult.documentation(fragment.wrapWith(html()).toString())
    }

  private fun BazelQueryFunction.argumentsMarkdown(): String =
    arguments.joinToString(separator = "\n") { arg ->
      "- `${arg.name}` (${arg.type}${if (arg.optional) ", optional" else ""}): ${arg.description}"
    }
}
