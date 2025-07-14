package org.jetbrains.bazel.languages.starlark.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.util.Processor
import org.jetbrains.bazel.languages.starlark.completion.lookups.StarlarkLookupElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.rename.RenameUtils

private fun getNamedArgumentValue(element: StarlarkCallExpression, argName: String): String? =
  (
    element
      .getArgumentList()
      ?.getArguments()
      ?.find { arg ->
        arg.name == argName
      }?.getValue() as? StarlarkStringLiteralExpression
  )?.getStringContents()

internal class BazelDepProcessor(private val lookingForModuleName: String) : Processor<StarlarkElement> {
  val result: MutableList<StarlarkElement> = mutableListOf()

  private fun getFunctionName(element: StarlarkCallExpression): String? = element.getNamePsi()?.text

  override fun process(element: StarlarkElement): Boolean {
    if (element !is StarlarkCallExpression) return true
    if (getFunctionName(element) != "bazel_dep") return true
    if (getNamedArgumentValue(element, "name") == lookingForModuleName) {
      result.add(element)
    }
    return true
  }
}

class StarlarkFunctionCallReference(element: StarlarkCallExpression, rangeInElement: TextRange) :
  PsiReferenceBase<StarlarkCallExpression>(element, rangeInElement, true) {
  override fun resolve(): PsiElement? =
    myElement?.let {
      val functionName = element.name
      when (functionName) {
        null -> null
        "archive_override", "git_override" -> {
          val moduleNameValue = getNamedArgumentValue(it, "module_name") ?: return@let null
          val processor = BazelDepProcessor(moduleNameValue)
          val file = it.containingFile as? StarlarkFile ?: return@let null
          file.searchInFunctionCalls(processor)
          processor.result.firstOrNull()
        }
        else -> {
          val processor = StarlarkResolveProcessor(mutableListOf(), it)
          SearchUtils.searchInFile(it, processor)
          processor.result.firstOrNull()
        }
      }
    }

  override fun getVariants(): Array<StarlarkLookupElement> = emptyArray()

  override fun handleElementRename(name: String): PsiElement {
    val namePsi = myElement.getNamePsi() ?: return myElement
    val oldNode = namePsi.getNameNode() ?: return myElement
    val newNode = RenameUtils.createNewName(myElement.project, name)
    namePsi.node.replaceChild(oldNode, newNode)
    return myElement
  }
}
