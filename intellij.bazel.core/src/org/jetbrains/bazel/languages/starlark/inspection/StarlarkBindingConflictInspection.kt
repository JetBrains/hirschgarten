package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.util.InspectionMessage
import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModCommand
import com.intellij.modcommand.Presentation
import com.intellij.modcommand.PsiBasedModCommandAction
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.util.applyIf
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStringLoadValue


@ApiStatus.Internal
class StarlarkBindingConflictInspection : LocalInspectionTool() {
  override fun isAvailableForFile(file: PsiFile): Boolean = file.fileType is StarlarkFileType

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = BindingVisitor(holder)

  class BindingVisitor(private val holder: ProblemsHolder) : StarlarkElementVisitor() {
    override fun visitFile(psiFile: PsiFile) {
      val file = psiFile as? StarlarkFile ?: return
      if (file.name in EXCLUDED_FILE_NAMES) return

      val declarationsByName = HashMap<String, MutableList<DeclarationInfo>>()

      for (child in file.children) {
        when (child) {
          is StarlarkFunctionDeclaration -> {
            val name = child.name ?: continue
            val element = child.nameIdentifier ?: child
            collectDeclarationInfo(name, element, BindingScope.GLOBAL, declarationsByName)
          }

          is StarlarkAssignmentStatement -> {
            if (!child.isTopLevel()) continue
            val name = child.name ?: continue
            collectDeclarationInfo(name, child, BindingScope.GLOBAL, declarationsByName)
          }

          is StarlarkLoadStatement -> {
            for (symbol in child.getLoadedSymbolsPsi()) {
              val name = if (symbol is StarlarkStringLoadValue) symbol.getLoadValueExpression()?.getStringContents() else symbol.name
              if (name == null) continue
              collectDeclarationInfo(name, symbol, BindingScope.FILE_LOCAL, declarationsByName)
            }
          }
        }
      }

      for ((name, declarations) in declarationsByName) {
        if (declarations.size <= 1) continue

        val allNavigatableDeclarations = declarations.asSequence().mapNotNull { it.navigatable }.distinct().toList()
        val crossKindConflict = declarations.any { it.scope == BindingScope.GLOBAL } && declarations.any { it.scope == BindingScope.FILE_LOCAL }
        var seenGlobal = false
        var seenFileLocal = false

        for ((element, scope, navigatable) in declarations) {
          val first = !seenGlobal && !seenFileLocal
          val sameScopeConflict = if (scope == BindingScope.GLOBAL) seenGlobal else seenFileLocal

          val message = when {
            first && !crossKindConflict -> StarlarkBundle.message("inspection.description.binding.conflict.original", name)
            sameScopeConflict -> StarlarkBundle.message("inspection.description.binding.conflict.same.kind", name)
            else -> StarlarkBundle.message("inspection.description.binding.conflict.cross.kind", name, scope.qualifier)
          }

          val fix = navigatable
            ?.let { ShowConflictingDeclarationsAction(name, it, allNavigatableDeclarations) }
            ?.let(LocalQuickFix::from)

          registerProblem(element, message, fix)

          if (scope == BindingScope.GLOBAL) seenGlobal = true else seenFileLocal = true
        }
      }
    }

    private fun collectDeclarationInfo(
      name: String,
      elem: PsiElement,
      scope: BindingScope,
      declarationsByName: MutableMap<String, MutableList<DeclarationInfo>>
    ) {
      val navigatable = (elem as? NavigatablePsiElement)
                        ?: (elem.parent as? NavigatablePsiElement)
                        ?: (elem.navigationElement as? NavigatablePsiElement)

      declarationsByName.getOrPut(name) { mutableListOf() }.add(DeclarationInfo(elem, scope, navigatable))
    }

    private fun registerProblem(element: PsiElement, @InspectionMessage message: String, fix: LocalQuickFix?) {
      if (fix == null) holder.registerProblem(element, message)
      else holder.registerProblem(element, message, fix)
    }
  }

  class ShowConflictingDeclarationsAction(
    private val declarationName: String,
    originalDeclaration: NavigatablePsiElement,
    conflictingDeclarations: List<NavigatablePsiElement>,
  ) : PsiBasedModCommandAction<NavigatablePsiElement>(originalDeclaration) {
    private val conflictingDeclarationsPointers: List<SmartPsiElementPointer<NavigatablePsiElement>> =
      conflictingDeclarations.map { SmartPointerManager.createPointer(it) }

    private val conflictingDeclarations: List<NavigatablePsiElement>
      get() = conflictingDeclarationsPointers.mapNotNull { it.element }

    override fun getFamilyName(): String = StarlarkBundle.message("inspection.quickfix.show.conflicting.declarations.family")

    override fun getPresentation(context: ActionContext, element: NavigatablePsiElement): Presentation =
      Presentation.of(StarlarkBundle.message("inspection.quickfix.show.conflicting.declarations.text", declarationName))

    override fun perform(context: ActionContext, element: NavigatablePsiElement): ModCommand {
      val navigateActions = buildList {
        for (conflictingDeclaration in conflictingDeclarations) {
          if (conflictingDeclaration != element) add(NavigateToConflictingDeclarationAction(conflictingDeclaration))
        }
        add(NavigateToConflictingDeclarationAction(element, isOriginalDeclaration = true))
      }
      return ModCommand.chooseAction(StarlarkBundle.message("inspection.quickfix.show.conflicting.declarations.popup.title"), navigateActions)
    }

    override fun generatePreview(context: ActionContext, element: NavigatablePsiElement): IntentionPreviewInfo {
      val builder = HtmlBuilder().apply {
        for (conflictingDeclaration in conflictingDeclarations) {
          if (conflictingDeclaration != element) {
            append(IntentionPreviewInfo.navigatePreviewHtmlChunk(conflictingDeclaration.containingFile, conflictingDeclaration.textOffset))
            br()
          }
        }
        append(IntentionPreviewInfo.navigatePreviewHtmlChunk(element.containingFile, element.textOffset))
        append(HtmlChunk.text(StarlarkBundle.message("inspection.quickfix.show.conflicting.declarations.current.marker.text")).bold())
      }
      return IntentionPreviewInfo.Html(builder.toFragment())
    }
  }

  class NavigateToConflictingDeclarationAction(
    declaration: NavigatablePsiElement,
    private val isOriginalDeclaration: Boolean = false,
  ) : PsiBasedModCommandAction<NavigatablePsiElement>(declaration) {
    override fun getFamilyName(): String = StarlarkBundle.message("inspection.quickfix.show.conflicting.declarations.navigate.family")

    override fun getPresentation(context: ActionContext, element: NavigatablePsiElement): Presentation {
      val title = StarlarkBundle.message(
        "inspection.quickfix.show.conflicting.declarations.navigate.text",
        element.containingFile.name, if (isOriginalDeclaration) 1 else 0,
      )
      return Presentation.of(title)
        .withIcon(element.containingFile.getIcon(0))
        .applyIf(element.containingFile == context.file) { withHighlighting(element.textRange) }
    }

    override fun generatePreview(context: ActionContext, element: NavigatablePsiElement): IntentionPreviewInfo = IntentionPreviewInfo.snippet(element)

    override fun perform(context: ActionContext, element: NavigatablePsiElement): ModCommand {
      val target = (element as? PsiNameIdentifierOwner)?.nameIdentifier ?: element
      return ModCommand.select(target)
    }
  }

  companion object {
    private val EXCLUDED_FILE_NAMES = Constants.BUILD_FILE_NAMES +
                                      arrayOf(Constants.WORKSPACE_FILE_NAME, "WORKSPACE.bazel", "WORKSPACE.bzlmod")

    private enum class BindingScope(val qualifier: String) {
      GLOBAL("global"),
      FILE_LOCAL("file-local"),
    }

    private data class DeclarationInfo(
      val element: PsiElement,
      val scope: BindingScope,
      val navigatable: NavigatablePsiElement?,
    )
  }
}
