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
import com.jetbrains.python.extensions.toPsi
import org.jetbrains.bazel.languages.starlark.globbing.StarlarkUnixGlob
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkGlobExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression


class StarlarkGlobReference(element: StarlarkGlobExpression) : PsiPolyVariantReferenceBase<StarlarkGlobExpression>(
  element,
  TextRange(0, element.textLength),
),
  PsiPolyVariantReference {
  override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult?> {
    val containingDirectory =
      element.containingFile.parent?.virtualFile // : File? = (containingFile as BuildFile).getFile().getParentFile()
    if (containingDirectory == null) {
      return ResolveResult.EMPTY_ARRAY
    }
    val includes = resolveListContents(element.getIncludes()) //resolveListContents(element.getIncludes())
    val excludes = resolveListContents(element.getExcludes())

    val directoriesExcluded: Boolean = false // element.areDirectoriesExcluded()
    if (includes.isEmpty()) {
      return ResolveResult.EMPTY_ARRAY
    }
    val project = element.getProject()

    try {
      val files: MutableList<VirtualFile?> =
        StarlarkUnixGlob.forPath(containingDirectory)
          .addPatterns(includes)
          .addExcludes(excludes)
          .setExcludeDirectories(directoriesExcluded)
          .setDirectoryFilter(directoryFilter(containingDirectory.path))
          .glob()

      val results: MutableList<ResolveResult?> = arrayListOf()
      for (file in files) {
        val psiFile: PsiFileSystemItem? = resolveFile(file, project)
        if (psiFile != null) {
          results.add(PsiElementResolveResult(psiFile))
        }
      }

      return results.toTypedArray()
    } catch (e: Exception) {
      return ResolveResult.EMPTY_ARRAY
    }
  }

  fun resolveFile(vf: VirtualFile?, project: Project): PsiFileSystemItem? {
    if (vf == null) {
      return null
    }
    val manager = PsiManager.getInstance(project)
    return if (vf.isDirectory) manager.findDirectory(vf) else manager.findFile(vf)
  }

  private fun findBuildFile(packageDir: VirtualFile): VirtualFile? =
    BUILD_FILE_NAMES.mapNotNull { packageDir.findChild(it) }.firstOrNull()

  private fun directoryFilter(base: String): (VirtualFile?) -> Boolean {
    return { file ->
      file?.path == base || file == null || findBuildFile(file) == null
    }
  }

  private fun resolveListContents(expr: PsiElement?): MutableList<String> {
    if (expr == null) {
      return mutableListOf()
    }
    if (expr !is StarlarkListLiteralExpression) {
      return mutableListOf()
    }
    val list = expr as StarlarkListLiteralExpression
    val children = list.getElements()
    val strings: MutableList<String> = mutableListOf()
    for (child in children) {
      if (child is StarlarkStringLiteralExpression) {
        strings.add((child as StarlarkStringLiteralExpression).getStringContents())
      }
    }
    return strings
  }

//  override fun resolve(): PsiElement? {
//    println("Resolving")
//    return null
//  }
}
