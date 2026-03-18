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

internal class StarlarkFunctionCallReference(element: StarlarkCallExpression, rangeInElement: TextRange) :
  PsiReferenceBase<StarlarkCallExpression>(element, rangeInElement, true) {
  override fun resolve(): PsiElement? {
    val element = myElement ?: return null
    when (element.name) {
      null -> {
        return null
      }

      "archive_override", "git_override" -> {
        val moduleNameValue = getNamedArgumentValue(element, "module_name") ?: return null
        val processor = BazelDepProcessor(moduleNameValue)
        val file = element.containingFile as? StarlarkFile ?: return null
        file.searchInFunctionCalls(processor)
        return processor.result.firstOrNull()
      }

      else -> {
        val processor = StarlarkResolveProcessor(mutableListOf(), element)
        SearchUtils.searchInFile(element, processor)
        return processor.result.firstOrNull()
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
