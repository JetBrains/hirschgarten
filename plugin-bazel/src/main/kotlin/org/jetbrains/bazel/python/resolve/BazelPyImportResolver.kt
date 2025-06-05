package org.jetbrains.bazel.python.resolve

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.util.QualifiedName
import com.jetbrains.python.psi.impl.PyImportResolver
import com.jetbrains.python.psi.resolve.PyQualifiedNameResolveContext
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.sync.SyncCache

class BazelPyImportResolver : PyImportResolver {
  val cacheKey = "PythonResolveIndex"

  override fun resolveImportReference(
    name: QualifiedName,
    context: PyQualifiedNameResolveContext,
    withRoots: Boolean,
  ): PsiElement? {
    if (!context.project.isBazelProject) return null

    val rootImportResolve = resolveRootImport(name, context)
    if (rootImportResolve != null) return rootImportResolve

    return resolveShortImport(name, context)
  }

  /**
   * Root import is an absolute import, whose qualified name aligns with the project file structure.
   * For example `import aaa.bbb.ccc` that references `aaa/bbb/ccc.py` file or `aaa/bbb/ccc` directory.
   */
  private fun resolveRootImport(name: QualifiedName, context: PyQualifiedNameResolveContext): PsiElement? {
    // VirtualFile.findFileByRelativePath always expects a slash ("/"), even on Windows
    val relativeToRoot = name.components.joinToString("/")
    return ReadAction.compute<PsiElement?, Throwable> {
      findDirectoryOrPythonFile(relativeToRoot, context)
    }
  }

  /**
   * In rare cases, both a directory and a file match the import.
   *
   * Precedence, as per [PEP-420](https://peps.python.org/pep-0420/#specification):
   *
   * "While looking for a module or package named “foo”, for each directory in the parent path:
   *
   * 1. If `<directory>/foo/__init__.py` is found, a regular package is imported and returned.
   *
   * 2. If not, but `<directory>/foo.{py,pyc,so,pyd}` is found, a module is imported and returned (...)"
   *
   * 3. If not, but `<directory>/foo` is found and is a directory,
   * it is recorded and the scan continues with the next directory in the parent path.
   *
   * 4. Otherwise the scan continues with the next directory in the parent path.
   *
   * If the scan completes without returning a module or package,
   * and at least one directory was recorded, then a namespace package is created."
   *
   * The paragraph above mentions looking "for each directory",
   * but this function is only responsible for looking in one (the project root).
   */
  private fun findDirectoryOrPythonFile(pathRelativeToRoot: String, context: PyQualifiedNameResolveContext): PsiElement? {
    val rootDir = context.project.rootDir
    val psiManager = context.psiManager
    val directory = rootDir.findFileByRelativePath(pathRelativeToRoot)?.directoryOrNull()

    // case 1
    if (directory?.findChild("__init__.py") != null) {
      return psiManager.findDirectory(directory) // case 1
    }

    // case 2
    val file = rootDir.findFileByRelativePath("$pathRelativeToRoot.py")?.fileOrNull()
    if (file != null) return psiManager.findFile(file)

    // case 3
    // TODO (BAZEL-1998) - verify if simply returning the namespace package directory is a good approach
    if (directory != null) {
      return psiManager.findDirectory(directory)
    }

    // case 4
    return null
  }

  private fun resolveShortImport(name: QualifiedName, context: PyQualifiedNameResolveContext): PsiElement? {
    val sourcesIndex =
      SyncCache
        .getInstance(context.project)
        .get(cacheKey, PythonResolveIndex()) ?: return null
    val resolvedName = sourcesIndex[name] ?: return null
    return resolvedName(context.psiManager)
  }
}

private fun VirtualFile.fileOrNull(): VirtualFile? = if (!isDirectory) this else null

private fun VirtualFile.directoryOrNull(): VirtualFile? = if (isDirectory) this else null
