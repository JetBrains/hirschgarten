package org.jetbrains.bazel.inspections

import com.intellij.codeInsight.multiverse.ModuleContext
import com.intellij.codeInsight.multiverse.codeInsightContext
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.jvm.JvmClass
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.libraries.LibraryContext
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiReference
import com.intellij.psi.scope.util.PsiScopesUtil
import com.intellij.workspaceModel.ide.legacyBridge.ModuleBridge
import com.intellij.workspaceModel.ide.legacyBridge.findLibraryEntity
import com.intellij.workspaceModel.ide.legacyBridge.findModuleEntity
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.repomapping.toApparentLabelOrThis
import org.jetbrains.bazel.workspacemodel.entities.BazelModuleExtensionEntity
import org.jetbrains.bazel.workspacemodel.entities.bazelLibraryExtension
import org.jetbrains.bazel.workspacemodel.entities.bazelModuleExtension
import org.jetbrains.bazel.workspacemodel.entities.targetKey
import org.jetbrains.bsp.protocol.StrictDependencyCheckedType

@ApiStatus.Internal
class JavaStrictDependenciesInspection : LocalInspectionTool() {

  override fun buildVisitor(
    holder: ProblemsHolder,
    isOnTheFly: Boolean,
  ): PsiElementVisitor {
    val psiFile = (holder.file as? PsiJavaFile) ?: return PsiElementVisitor.EMPTY_VISITOR
    val bazelModuleExtension = bazelModuleExtension(psiFile) ?: return PsiElementVisitor.EMPTY_VISITOR

    val moduleLabel = bazelModuleExtension.targetKey.label
    val strictDependenciesCheck = bazelModuleExtension.strictDependencies.check
    val strictDependencies = bazelModuleExtension.strictDependencies.labels.toSet()
    if (strictDependenciesCheck == StrictDependencyCheckedType.OFF || strictDependencies.isEmpty())
      return PsiElementVisitor.EMPTY_VISITOR

    return object : JavaElementVisitor() {
      override fun visitReferenceElement(reference: PsiJavaCodeReferenceElement) {
        reference.reference?.let {
          checkReference(it)
        }
      }

      fun checkReference(reference: PsiReference) {
        val psiClass = reference.resolve() as? JvmClass
        if (psiClass != null) {
          val psiClassLabel = getTargetLabel(psiClass.sourceElement)
          if (psiClassLabel != null &&
              moduleLabel != psiClassLabel &&
              !strictDependencies.contains(psiClassLabel.toString())) {
            val apparentLabel = psiClassLabel.toApparentLabelOrThis(holder.project)

            holder.registerProblem(
              reference.element,
              JavaInspectionsBundle.message(
                "java.strict.deps.indirect.dependency",
                psiClass.qualifiedName ?: psiClass.name ?: "<undefined>",
                apparentLabel,
              ),
              if (strictDependenciesCheck == StrictDependencyCheckedType.ERROR)
                ProblemHighlightType.ERROR
              else
                ProblemHighlightType.WARNING,
            )
          }
        }
      }
    }
  }

  private fun bazelModuleExtension(file: PsiFile?): BazelModuleExtensionEntity? {
    if (file == null)
      return null

    val multiverseModule = (file.codeInsightContext as? ModuleContext)?.getModule()?.findModuleEntity()?.bazelModuleExtension
    if (multiverseModule != null)
      return multiverseModule

    return ProjectFileIndex.getInstance(file.project).getModuleForFile(file.virtualFile)?.findModuleEntity()?.bazelModuleExtension
  }

  private fun getTargetLabel(element: PsiElement?): Label? {
    val file = element?.containingFile ?: return null

    val multiverseModule = (file.codeInsightContext as? ModuleContext)?.getModule()
      ?.findModuleEntity()
      ?.bazelModuleExtension
    if (multiverseModule != null)
      return multiverseModule.targetKey.label

    val library = (file.codeInsightContext as? LibraryContext)?.getLibrary()
    if (library != null) {
      val multiverseLibrary = library
        .findLibraryEntity(element.project.workspaceModel.currentSnapshot)
        ?.bazelLibraryExtension
      if (multiverseLibrary != null)
        return multiverseLibrary.targetKey.label
    }

    // Fallback
    return ProjectFileIndex.getInstance(file.project).getModuleForFile(file.virtualFile)
      ?.findModuleEntity()?.bazelModuleExtension?.targetKey?.label
  }
}
