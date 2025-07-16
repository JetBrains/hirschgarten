package org.jetbrains.bazel.languages.bazelquery.completion

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.startOffset
import com.intellij.util.ProcessingContext
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.languages.bazelquery.BazelQueryLanguage
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryTokenSets
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryTokenType
import org.jetbrains.bazel.languages.bazelquery.psi.BazelQueryFile

class BazelQueryCompletionContributor : CompletionContributor() {
  init {
    extend(CompletionType.BASIC, BazelWordCompletionProvider.psiPattern, BazelWordCompletionProvider())
  }
}

val knownCommands =
  BazelQueryTokenSets.COMMANDS.types
    .map { tokenType ->
      tokenType as BazelQueryTokenType
      tokenType.completionText() + "()"
    }.toList()

val knownOperations =
  BazelQueryTokenSets.OPERATIONS.types
    .map { tokenType ->
      tokenType as BazelQueryTokenType
      tokenType.completionText()
    }.toList()

class BazelQueryPrefixMatcher(prefix: String) : PrefixMatcher(prefix) {
  override fun prefixMatches(name: String): Boolean = name.startsWith(prefix, ignoreCase = true)

  override fun cloneWithPrefix(newPrefix: String): PrefixMatcher = BazelQueryPrefixMatcher(newPrefix)
}

private val wordsOrCommandsTokens =
  TokenSet.create(
    *BazelQueryTokenSets.WORDS.types,
    *BazelQueryTokenSets.COMMANDS.types,
    *BazelQueryTokenSets.PATTERNS.types,
  )

private class BazelWordCompletionProvider : CompletionProvider<CompletionParameters>() {
  private val startTargetSign = "//"

  companion object {
    val psiPattern =
      psiElement()
        .withLanguage(BazelQueryLanguage)
        .and(
          psiElement().inside(psiElement(BazelQueryFile::class.java)),
        ).andOr(
          psiElement().withElementType(wordsOrCommandsTokens),
          psiElement().atStartOf(psiElement().withElementType(wordsOrCommandsTokens)),
        )
  }

  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet,
  ) {
    val prefix =
      parameters.position.text
        .take(parameters.offset - parameters.position.startOffset)
        .trim()
        .removePrefix("\"")
        .removePrefix("'")
    val project = parameters.editor.project ?: return
    val targetSuggestions =
      TargetCompletionsGenerator(project)
        .getTargetsList(prefix)

    result.withPrefixMatcher(BazelQueryPrefixMatcher(prefix)).run {
      addAllElements(
        knownCommands.map { functionLookupElement(it, 1.0, parameters.offset) },
      )
      addAllElements(
        knownOperations.map { functionLookupElement(it, 0.0, parameters.offset) },
      )
      addAllElements(
        targetSuggestions.map {
          functionLookupElement(
            it,
            when {
              it.startsWith(startTargetSign) &&
                (it.endsWith(":all") || it.endsWith(":all-targets")) -> 0.1

              it.startsWith(startTargetSign) -> 0.3

              !it.startsWith(startTargetSign) &&
                (it.endsWith(":all") || it.endsWith(":all-targets")) -> 0.2

              else -> 0.4
            },
            parameters.offset,
          )
        },
      )
    }
  }

  private fun functionLookupElement(
    name: String,
    priority: Double,
    caretOffset: Int,
  ): LookupElement =
    PrioritizedLookupElement.withPriority(
      LookupElementBuilder
        .create(name)
        .withBoldness(true)
        .withIcon(BazelPluginIcons.bazel)
        .withInsertHandler { context, item ->
          /**
           * Inserting a selected suggestion with a target path using 'tab' pastes a fragment of the suggestion
           * to the slash occurrence (corresponding to next subpackage on the path), for example, for the entered
           * prefix "//p", selecting the suggestion "//path/to/my:target" using 'tab' will result in "//path/"
           * and potentially shorten the list of suggestions.
           */
          if (context.completionChar == '\t') {
            val startOffset = context.startOffset
            val cursorPositionInSuggestion = caretOffset - startOffset
            val currentText = item.lookupString
            val nextSlashIndex = currentText.indexOf('/', cursorPositionInSuggestion)

            if (nextSlashIndex > 0) {
              val shortenedText = currentText.take(nextSlashIndex + 1)
              context.document.replaceString(context.startOffset, context.tailOffset, shortenedText)
              context.editor.caretModel.moveToOffset(context.startOffset + shortenedText.length)
              AutoPopupController.getInstance(context.project)?.autoPopupMemberLookup(context.editor, null)
            }
          }
          /**
           * Regardless of whether we select a suggestion using tab or enter, if we complete the name of a function
           * (ended with parentheses), we place the cursor in the middle of the parentheses to continue entering its
           * arguments (in Bazel Query, all functions have at least one argument).
           */
          val offset = context.editor.caretModel.offset
          if (offset > 0 && context.document.charsSequence[offset - 1] == ')') {
            context.editor.caretModel.moveToOffset(offset - 1)
          }
        },
      priority,
    )
}
