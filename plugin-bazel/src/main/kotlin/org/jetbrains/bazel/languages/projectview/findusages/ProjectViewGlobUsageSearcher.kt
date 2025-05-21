package org.jetbrains.bazel.languages.projectview.findusages

//import org.jetbrains.bazel.languages.starlark.references.BUILD_FILE_NAMES
import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import com.intellij.util.PathUtil
import com.intellij.util.Processor
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkGlobExpression
import java.nio.file.Path


/**
 * Searches for references to a file in globs. These aren't picked up by a standard string search,
 * and are only evaluated on demand, so we can't just check a reference cache.
 *
 *
 * Unlike resolving a glob, this requires no file system calls (beyond finding the parent blaze
 * package), because we're only interested in a single file, which is already known to exist.
 *
 *
 * This is always a local search (as glob references can't cross package boundaries).
 */
class ProjectViewGlobUsageSearcher : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>(true) {
  override fun processQuery(queryParameters: ReferencesSearch.SearchParameters, consumer: Processor<in PsiReference?>) {
//      val file = queryParameters.elementToSearch.containingFile
    val file = asFileSystemItemSearch(queryParameters.elementToSearch)
    if (file == null) {
      return
    }
    val containingPackage = findContainingPackage(file.virtualFile)?: return
    val buildFile = findBuildFile(containingPackage)?: return
    if (!inScope(queryParameters, buildFile)) {
      return
    }
    val relativePath = getRelativePathToChild(buildFile, file.virtualFile)?: return


//    val buildFilePsi = buildFile.getPsiFile(queryParameters.project)
    val psiManager = PsiManager.getInstance(queryParameters.project)
    val buildFilePsi = psiManager.findFile(buildFile)

    val globs: Collection<StarlarkGlobExpression> =
      PsiTreeUtil.findChildrenOfType(buildFilePsi, StarlarkGlobExpression::class.java)
    for (glob in globs) {
      if (glob.matches(relativePath, file.isDirectory)) {
        consumer.process(globReference(glob, file))
      }
    }
  }

  fun getRelativePathToChild(parent: VirtualFile, child: VirtualFile): String? {
    val packageDirPath = Path.of(PathUtil.getParentPath(parent.path))
    val filePathPath = Path.of(child.path)
    return if (filePathPath.startsWith(packageDirPath)) filePathPath.subpath(packageDirPath.nameCount, filePathPath.nameCount).toString()
    else null
  }

  fun findContainingPackage(directory: VirtualFile?): VirtualFile? =
    directory?.let {
      if (findBuildFile(directory) != null) directory
      else findContainingPackage(directory.parent)
    }

  private fun findBuildFile(packageDir: VirtualFile): VirtualFile? =
    BUILD_FILE_NAMES.mapNotNull { packageDir.findChild(it) }.firstOrNull()

  companion object {
    private val BUILD_FILE_NAMES = listOf("BUILD.bazel", "BUILD")

    fun asFileSystemItemSearch(elementToSearch: PsiElement): PsiFileSystemItem? {
      if (elementToSearch is PsiFileSystemItem) {
        return elementToSearch
      }
      return asFileSearch(elementToSearch)
    }

    fun asFileSearch(elementToSearch: PsiElement): PsiFile? {
      if (elementToSearch is PsiFile) {
        return elementToSearch
      }
//      for (provider in  PsiFileProvider.EP_NAME.getExtensions()) {
//        val file: PsiFile? = provider.asFileSearch(elementToSearch)
//        if (file != null) {
//          return file
//        }
//      }
      return null
    }

    private fun globReference(glob: StarlarkGlobExpression, file: PsiFileSystemItem): PsiReference =
      object : PsiReferenceBase.Immediate<StarlarkGlobExpression>(
        glob,
        glob.getReferenceTextRange(),
        file,
      ) {
        @Throws(IncorrectOperationException::class)
        override fun bindToElement(element: PsiElement): PsiElement = glob
      }

    private fun inScope(queryParameters: ReferencesSearch.SearchParameters, buildFile: VirtualFile): Boolean {
      val scope = queryParameters.scopeDeterminedByUser
      if (scope is GlobalSearchScope) {
        return scope.contains(buildFile)
      }
      return (scope as LocalSearchScope).isInScope(buildFile)
    }


  }
}
