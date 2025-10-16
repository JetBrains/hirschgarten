package org.jetbrains.bazel.sdkcompat.k2

import com.intellij.openapi.externalSystem.autolink.ExternalSystemUnlinkedProjectAware
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.k2.refactoring.move.descriptor.K2MoveDescriptor
import org.jetbrains.kotlin.idea.k2.refactoring.move.descriptor.K2MoveTargetDescriptor
import org.jetbrains.kotlin.idea.k2.refactoring.move.processor.K2MoveDeclarationsRefactoringListener

/**
 * If we move a Kotlin class to a package that's contained by a dummy module,
 * then the Kotlin plugin gets confused and deletes all import statements in the moved files.
 * Also, no references are updated to point to the new package.
 * That's happening because after the move the resolve scope doesn't contain the needed classes anymore.
 *
 * So we do a HACK: temporarily re-add the moved file to the module that it was originally contained in so that
 * all the imports are changed correctly, and then remove it again.
 * Since both [beforeMove] and [afterMove] are called in a single write action, to the outside world it doesn't make a difference.
 */
class BazelK2MoveDeclarationsRefactoringListener : K2MoveDeclarationsRefactoringListener {
  private val contentEntriesToRemove = mutableMapOf<K2MoveDescriptor.Declarations, Pair<Module, ContentEntry>>()

  override fun beforeMove(moveDescriptor: K2MoveDescriptor.Declarations) {
    val project = moveDescriptor.project
    if (!project.isBazelProject) return

    val target = moveDescriptor.target as? K2MoveTargetDescriptor.File ?: return

    val baseDirectory = target.baseDirectory.virtualFile
    val newModule = ProjectFileIndex.getInstance(project).getModuleForFile(baseDirectory) ?: return

    if (!newModule.isDummyModule()) return

    val elementToMove = moveDescriptor.source.elements.firstOrNull() ?: return
    val originalModule = ModuleUtilCore.findModuleForPsiElement(elementToMove as PsiElement) ?: return
    if (originalModule.isDummyModule()) return

    val modifiableModel = originalModule.rootManager.modifiableModel
    val urlToAdd =
      baseDirectory
        .toNioPath()
        .resolve(target.fileName)
        .toUri()
        .toString()
    val contentEntry = modifiableModel.addContentEntry(urlToAdd)
    contentEntriesToRemove[moveDescriptor] = originalModule to contentEntry
    contentEntry.addSourceFolder(urlToAdd, false)
    // We're called from a write action, so this is legal
    modifiableModel.commit()
  }

  override fun afterMove(moveDescriptor: K2MoveDescriptor.Declarations) {
    val (module, contentEntry) = contentEntriesToRemove.remove(moveDescriptor) ?: return
    val modifiableModel = module.rootManager.modifiableModel
    modifiableModel.removeContentEntry(contentEntry)
    modifiableModel.commit()
  }

  private fun Module.isDummyModule(): Boolean = moduleTypeName == BAZEL_DUMMY_MODULE_TYPE

  companion object {
    // We can't depend on other classes from sdkcompat
    private const val BAZEL_DUMMY_MODULE_TYPE = "BAZEL_DUMMY_MODULE_TYPE"
  }
}

/**
 * See [BazelUnlinkedProjectAware]. Unfortunately, we can't use is from here directly.
 */
private val Project.isBazelProject: Boolean
  get() =
    ExternalSystemUnlinkedProjectAware.EP_NAME.findFirstSafe { it.systemId.id == "bazel" }?.isLinkedProject(this, this.basePath.orEmpty())
      ?: false
