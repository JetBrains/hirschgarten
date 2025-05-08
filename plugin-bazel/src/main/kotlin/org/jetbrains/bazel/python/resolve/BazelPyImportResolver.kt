package org.jetbrains.bazel.python.resolve

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.QualifiedName
import com.jetbrains.python.psi.impl.PyImportResolver
import com.jetbrains.python.psi.resolve.PyQualifiedNameResolveContext
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir

class BazelPyImportResolver : PyImportResolver {
  override fun resolveImportReference(
    name: QualifiedName,
    context: PyQualifiedNameResolveContext,
    withRoots: Boolean,
  ): PsiElement? {
    if (!context.project.isBazelProject) return null

    return resolveRootImport(name, context)
  }

  /**
   * Root import is an absolute import, whose qualified name aligns with the project file structure.
   * For example `import aaa.bbb.ccc` that references `aaa/bbb/ccc.py` file or `aaa/bbb/ccc` directory.
   */
  private fun resolveRootImport(name: QualifiedName, context: PyQualifiedNameResolveContext): PsiElement? {
    // VirtualFile.findFileByRelativePath always expects a slash ("/"), even on Windows
    val relativeToRoot = name.components.joinToString("/")
    return ReadAction.compute<PsiElement?, Throwable> {
      context.project.findDirectoryOrPythonFile(relativeToRoot)
    }
  }

  /**
   * In rare cases, both a directory and a file match the import.
   * When both `aaa/bbb/ccc` and `aaa/bbb/ccc.py` exist, Python chooses the directory when `import aaa.bbb.ccc` is used.
   * For that reason this function chooses the directory as well.
   */
  private fun Project.findDirectoryOrPythonFile(pathRelativeToRoot: String): PsiElement? =
    PsiManager.getInstance(this).let { psi ->
      rootDir.findFileByRelativePath(pathRelativeToRoot)?.let { psi.findDirectory(it) }
        ?: rootDir.findFileByRelativePath("$pathRelativeToRoot.py")?.let { psi.findFile(it) }
    }
}
