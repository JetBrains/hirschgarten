package org.jetbrains.bazel.languages.starlark.references

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkGlobExpression

@ApiStatus.Internal
class StarlarkGlobReference(element: StarlarkGlobExpression) :
  PsiPolyVariantReferenceBase<StarlarkGlobExpression>(
    element,
    TextRange(0, element.textLength),
  ),
  PsiPolyVariantReference {
  override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {
    val project = element.getProject()

    try {
      val files: List<VirtualFile> = element.getGlob()?.execute() ?: emptyList()

      val results: MutableList<ResolveResult> = arrayListOf()
      for (file in files) {
        val psiFile: PsiFileSystemItem? = resolveFile(file, project)
        if (psiFile != null) {
          results.add(PsiElementResolveResult(psiFile))
        }
      }

      return results.toTypedArray()
    } catch (_: Exception) {
      return ResolveResult.EMPTY_ARRAY
    }
  }

  fun resolveFile(vf: VirtualFile, project: Project): PsiFileSystemItem? {
    val manager = PsiManager.getInstance(project)
    return if (vf.isDirectory) manager.findDirectory(vf) else manager.findFile(vf)
  }

  override fun handleElementRename(newElementName: String): PsiElement? {
    return myElement
  }
}
