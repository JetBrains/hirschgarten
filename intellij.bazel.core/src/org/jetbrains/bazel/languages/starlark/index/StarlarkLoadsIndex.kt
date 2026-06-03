package org.jetbrains.bazel.languages.starlark.index

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.ID
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement
import org.jetbrains.bazel.languages.starlark.references.resolveFileTargetToVirtualFile

@ApiStatus.Internal
object StarlarkLoadsIndex {

  val NAME: ID<String, Collection<String>> = ID.create("starlark.loads")

  /**
   * Process files that load [element] from given [scope].
   */
  fun processFiles(
    element: StarlarkNamedElement,
    scope: GlobalSearchScope,
    processor: Processor<VirtualFile>,
  ): Boolean {
    val name = element.name ?: return false
    val containingFile = element.containingFile?.virtualFile ?: return false
    val project = element.project
    return FileBasedIndex
      .getInstance()
      .processValues(
        NAME,
        name,
        null,
        { file, labels ->
          file == containingFile
          || labels.none { resolveFileTargetToVirtualFile(project, it, file) == containingFile }
          || processor.process(file)
        },
        scope,
      )
  }

  /**
   * Checks whether [element] was loaded in [scope].
   */
  fun isLoaded(element: StarlarkNamedElement, scope: GlobalSearchScope): Boolean {
    var found = false
    processFiles(element, scope) {
      found = true
      false
    }
    return found
  }

  private fun resolveFileTargetToVirtualFile(project: Project, rawLabel: String, file: VirtualFile): VirtualFile? {
    val label = Label.parseOrNull(rawLabel) ?: return null
    return resolveFileTargetToVirtualFile(project, label, file)
  }
}
