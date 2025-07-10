package org.jetbrains.bazel.languages.projectview.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.endOffset
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext
import org.jetbrains.bazel.languages.bazelrc.flags.BazelFlagSymbol
import org.jetbrains.bazel.languages.bazelrc.flags.Flag
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection

class BuildFlagCompletionProvider : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet,
  ) {
    val section = PsiTreeUtil.getParentOfType(parameters.position, ProjectViewPsiSection::class.java) ?: return
    val previousFlags = section.getItems().map { it.text.trim() }.filter { it.isNotEmpty() }
    val pos = parameters.position
    val flags = Flag.all().filter { !previousFlags.contains(it.key) }
    result.run {
      addAllElements(
        flags.entries.map { it -> Pair(it.key, BazelFlagSymbol(it.value, pos.project)) }.map { (k, flagSymbol) ->
          LookupElementBuilder
            .create(flagSymbol, k)
            .withLookupString("${k}_xxx")
            .withTypeText(" ${flagSymbol.flag.option.valueHelp}")
            .withPresentableText(k)
            .withIcon(PlatformIcons.PARAMETER_ICON)
            .withInsertHandler { ctx, _ ->
              ctx.file.findElementAt(ctx.startOffset)?.let { psiElement ->
                if (psiElement.text != k) {
                  ctx.editor.document.replaceString(psiElement.textOffset, psiElement.endOffset, k)
                }
              }
            }
        },
      )
    }
  }
}
