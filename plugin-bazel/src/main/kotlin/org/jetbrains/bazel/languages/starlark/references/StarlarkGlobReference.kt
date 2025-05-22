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
import org.jetbrains.bazel.languages.starlark.globbing.StarlarkUnixGlob
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkGlobExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression

class StarlarkGlobReference(element: StarlarkGlobExpression) :
  PsiPolyVariantReferenceBase<StarlarkGlobExpression>(
    element,
    TextRange(0, element.textLength),
  ),
  PsiPolyVariantReference {
  override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {
    val containingDirectory = element.containingFile.parent?.virtualFile
    if (containingDirectory == null) {
      return ResolveResult.EMPTY_ARRAY
    }
    val includes = resolveListContents(element.getIncludes())
    val excludes = resolveListContents(element.getExcludes())

    val directoriesExcluded = element.areDirectoriesExcluded()
    if (includes.isEmpty()) {
      return ResolveResult.EMPTY_ARRAY
    }
    val project = element.getProject()

    val files: List<VirtualFile> =
      StarlarkUnixGlob
        .forPath(containingDirectory)
        .addPatterns(includes)
        .addExcludes(excludes)
        .setExcludeDirectories(directoriesExcluded)
        .setDirectoryFilter(directoryFilter(containingDirectory.path))
        .glob()

    val results: MutableList<ResolveResult> = arrayListOf()
    for (file in files) {
      val psiFile: PsiFileSystemItem? = resolveFile(file, project)
      if (psiFile != null) {
        results.add(PsiElementResolveResult(psiFile))
      }
    }

    return results.toTypedArray()
  }

  fun resolveFile(vf: VirtualFile, project: Project): PsiFileSystemItem? {
    val manager = PsiManager.getInstance(project)
    return if (vf.isDirectory) manager.findDirectory(vf) else manager.findFile(vf)
  }

  private fun directoryFilter(base: String): (VirtualFile) -> Boolean =
    { file ->
      file.path == base || findBuildFile(file) == null
    }

  private fun resolveListContents(expr: PsiElement?): List<String> {
    if (expr !is StarlarkListLiteralExpression) {
      return listOf()
    }
    val children = expr.getElements()
    val strings: MutableList<String> = mutableListOf()
    for (child in children) {
      if (child is StarlarkStringLiteralExpression) {
        strings.add(child.getStringContents())
      }
    }
    return strings
  }

  /**
   * Returns true iff the complete, resolved glob references the specified file.
   *
   *
   * In particular, it's not concerned with individual patterns referencing the file, only
   * whether the overall glob does (i.e. returns false if the file is explicitly excluded).
   */
  fun matches(packageRelativePath: String, isDirectory: Boolean): Boolean {
    if (isDirectory && element.areDirectoriesExcluded()) {
      return false
    }
    for (exclude in resolveListContents(element.getExcludes())) {
      if (StarlarkUnixGlob.matches(exclude, packageRelativePath)) {
        return false
      }
    }
    for (include in resolveListContents(element.getIncludes())) {
      if (StarlarkUnixGlob.matches(include, packageRelativePath)) {
        return true
      }
    }
    return false
  }
}
