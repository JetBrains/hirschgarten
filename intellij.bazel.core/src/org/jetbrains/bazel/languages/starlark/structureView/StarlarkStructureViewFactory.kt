package org.jetbrains.bazel.languages.starlark.structureView

import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase
import com.intellij.ide.util.treeView.smartTree.Sorter
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkParenthesizedExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTupleExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.isBazelFileTopLevelCall
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkExpressionStatement
import org.jetbrains.bazel.languages.starlark.repomapping.calculateLabel
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import javax.swing.Icon

internal class StarlarkStructureViewFactory : PsiStructureViewFactory {
  override fun getStructureViewBuilder(psiFile: PsiFile): TreeBasedStructureViewBuilder? {
    if (psiFile !is StarlarkFile) return null
    return object : TreeBasedStructureViewBuilder() {
      override fun createStructureViewModel(editor: Editor?): StructureViewModel = StarlarkStructureViewModel(psiFile, editor)
    }
  }
}

private class StarlarkStructureViewModel(file: StarlarkFile, editor: Editor?) :
  StructureViewModelBase(file, editor, StarlarkFileStructureViewElement(file)),
  StructureViewModel.ElementInfoProvider {

  override fun getSorters(): Array<Sorter> = arrayOf(Sorter.ALPHA_SORTER)

  override fun isSuitable(element: PsiElement): Boolean = when (element) {
    is StarlarkFunctionDeclaration -> element.isTopLevel()
    is StarlarkTargetExpression -> isTopLevelAssignmentTarget(element)
    is StarlarkCallExpression -> element.isBazelFileTopLevelCall() && element.isRuleTarget()
    else -> false
  }

  override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean = false

  override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean = element !is StarlarkFileStructureViewElement

  private fun isTopLevelAssignmentTarget(target: StarlarkTargetExpression): Boolean {
    val assignment = PsiTreeUtil.getParentOfType(target, StarlarkAssignmentStatement::class.java) ?: return false
    return assignment.isTopLevel() && target in getAssignmentTargets(assignment.leftHandSide())
  }
}

// presentableText is the Structure item name, locationString is shown gray next to the item name
private class StarlarkFileStructureViewElement(file: StarlarkFile) : PsiTreeElementBase<StarlarkFile>(file) {
  override fun getPresentableText(): String? = element?.name

  override fun getLocationString(): String? {
    val file = element?.takeIf { it.isBuildFile() } ?: return null
    val virtualFile = file.virtualFile ?: return null
    return calculateLabel(file.project, virtualFile)?.toShortString(file.project)
  }

  override fun getChildrenBase(): Collection<StructureViewTreeElement> {
    val file = element ?: return emptyList()
    return buildList {
      for (child in file.children) {
        when (child) {
          is StarlarkFunctionDeclaration -> add(StarlarkStructureItem(child))
          is StarlarkAssignmentStatement -> {
            val targets = getAssignmentTargets(child.leftHandSide())
            targets.sortedBy { it.textOffset }.mapTo(this) { StarlarkStructureItem(it) }
          }
          is StarlarkExpressionStatement -> {
            val call = child.callExpressionOrNull()
            if (call != null && call.isRuleTarget()) add(StarlarkRuleStructureItem(call))
          }
        }
      }
    }
  }
}

private fun getAssignmentTargets(element: PsiElement?): List<StarlarkTargetExpression> = when (element) {
  is StarlarkTargetExpression -> listOf(element)
  is StarlarkTupleExpression, is StarlarkListLiteralExpression, is StarlarkParenthesizedExpression ->
    element.children.flatMap { getAssignmentTargets(it) }
  else -> emptyList()
}

private class StarlarkStructureItem(element: StarlarkNamedElement) : PsiTreeElementBase<StarlarkNamedElement>(element) {
  override fun getPresentableText(): String? = element?.name

  override fun getChildrenBase(): Collection<StructureViewTreeElement> = emptyList()
}

private class StarlarkRuleStructureItem(call: StarlarkCallExpression) : PsiTreeElementBase<StarlarkCallExpression>(call) {
  override fun getPresentableText(): String? = element?.getNameAttributeValue()

  override fun getLocationString(): String? = element?.getCalledFunctionName()

  override fun getIcon(open: Boolean): Icon = BazelPluginIcons.bazel

  override fun getChildrenBase(): Collection<StructureViewTreeElement> = emptyList()
}
