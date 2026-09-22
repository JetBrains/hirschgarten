package org.jetbrains.bazel.inspections

import com.intellij.codeInsight.multiverse.CodeInsightContext
import com.intellij.codeInsight.multiverse.CodeInsightContextManager
import com.intellij.codeInsight.multiverse.ModuleContext
import com.intellij.codeInsight.multiverse.codeInsightContext
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.jvm.JvmClass
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.libraries.LibraryContext
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiReference
import com.intellij.util.containers.sequenceOfNotNull
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

    fun isDirectDependency(label: Label): Boolean =
      moduleLabel == label || strictDependencies.contains(label.toString())

    val project = psiFile.project

    return object : JavaElementVisitor() {
      override fun visitReferenceElement(reference: PsiJavaCodeReferenceElement) {
        reference.reference?.let {
          checkReference(it)
        }
      }

      fun checkReference(reference: PsiReference) {
        val psiClass = reference.resolve() as? JvmClass ?: return

        val psiClassLabel = getTargetLabels(
          psiClass.sourceElement,
          psiClass.sourceElement?.codeInsightContext,
        ).singleOrNull() ?: return
        if (isDirectDependency(psiClassLabel)) return

        // Several targets may provide the very same class: two maven installs shipping the same
        // artifact, or one source file listed in the srcs of two targets. Resolution picks just one
        // of them, and that one may be an indirect dependency even though a direct dependency
        // provides the same class, in which case Bazel itself is happy. Report only when no target
        // providing this class is a direct dependency.
        if (allTargetLabelsOf(psiClass, reference.element).any(::isDirectDependency))
          return

        val apparentLabel = psiClassLabel.toApparentLabelOrThis(project)
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

      private fun getTargetLabels(element: PsiElement?, explicitContext: CodeInsightContext?): Sequence<Label> {
        val file = element?.containingFile ?: return emptySequence()
        val virtualFile = file.virtualFile ?: return emptySequence()

        val contexts = explicitContext?.let { listOf(it) }
                       ?: CodeInsightContextManager.getInstance(project).getCodeInsightContexts(virtualFile)
        val contextLabels = contexts.mapNotNull { getTargetLabel(it, project) }
        if (contextLabels.isNotEmpty())
          return contextLabels.asSequence()

        // Fallback
        val fallbackLabel = ProjectFileIndex.getInstance(project).getModuleForFile(virtualFile)
          ?.findModuleEntity()?.bazelModuleExtension?.targetKey?.label
        return sequenceOfNotNull(fallbackLabel)
      }

      private fun getTargetLabel(context: CodeInsightContext, project: Project): Label? {
        val multiverseModule = (context as? ModuleContext)?.getModule()
          ?.findModuleEntity()
          ?.bazelModuleExtension
        if (multiverseModule != null)
          return multiverseModule.targetKey.label

        val library = (context as? LibraryContext)?.getLibrary()
        if (library != null) {
          val multiverseLibrary = library
            .findLibraryEntity(project.workspaceModel.currentSnapshot)
            ?.bazelLibraryExtension
          if (multiverseLibrary != null)
            return multiverseLibrary.targetKey.label
        }

        return null
      }

      /**
       * Labels of every target that provides [psiClass] to [place]: all the classes with the same qualified
       * name visible from [place], each of them mapped through all of the contexts of its containing file,
       * because a single source file may belong to several targets.
       */
      private fun allTargetLabelsOf(psiClass: JvmClass, place: PsiElement): Sequence<Label> {
        val qualifiedName = psiClass.qualifiedName ?: return emptySequence()
        return JavaPsiFacade.getInstance(project)
          .findClasses(qualifiedName, place.resolveScope)
          .asSequence()
          .flatMap { getTargetLabels(it.containingFile, explicitContext = null) }
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
}
