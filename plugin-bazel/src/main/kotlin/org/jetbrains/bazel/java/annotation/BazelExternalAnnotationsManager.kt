package org.jetbrains.bazel.java.annotation

import com.intellij.codeInsight.ExternalAnnotationsManagerImpl
import com.intellij.codeInsight.ModCommandAwareExternalAnnotationsManager
import com.intellij.modcommand.ModCommand
import com.intellij.modcommand.ModNothing
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiNameValuePair
import org.jetbrains.bazel.config.isBazelProject

/**
 * See [YouTrack issue](https://youtrack.jetbrains.com/issue/BAZEL-1128).
 */
class BazelExternalAnnotationsManager(project: Project) :
  ModCommandAwareExternalAnnotationsManager(PsiManager.getInstance(project)),
  Disposable {
  private val delegate: ModCommandAwareExternalAnnotationsManager =
    if (project.isBazelProject) {
      DummyExternalAnnotationsManager(PsiManager.getInstance(project))
    } else {
      ExternalAnnotationsManagerImpl(project).also {
        Disposer.register(this, it)
      }
    }

  override fun hasAnnotationRootsForFile(file: VirtualFile): Boolean = delegate.hasAnnotationRootsForFile(file)

  override fun isExternalAnnotation(annotation: PsiAnnotation): Boolean = delegate.isExternalAnnotation(annotation)

  override fun findExternalAnnotation(listOwner: PsiModifierListOwner, annotationFQN: String): PsiAnnotation? =
    delegate.findExternalAnnotation(listOwner, annotationFQN)

  override fun findExternalAnnotations(listOwner: PsiModifierListOwner, annotationFQN: String): List<PsiAnnotation> =
    delegate.findExternalAnnotations(listOwner, annotationFQN)

  override fun isExternalAnnotationWritable(listOwner: PsiModifierListOwner, annotationFQN: String): Boolean =
    delegate.isExternalAnnotationWritable(listOwner, annotationFQN)

  override fun findExternalAnnotations(listOwner: PsiModifierListOwner): Array<out PsiAnnotation> =
    delegate.findExternalAnnotations(listOwner) ?: emptyArray() // v243: findExternalAnnotations was made non-nullable

  override fun findDefaultConstructorExternalAnnotations(aClass: PsiClass): List<PsiAnnotation>? =
    delegate.findDefaultConstructorExternalAnnotations(aClass)

  override fun findDefaultConstructorExternalAnnotations(aClass: PsiClass, annotationFQN: String): List<PsiAnnotation>? =
    delegate.findDefaultConstructorExternalAnnotations(aClass, annotationFQN)

  override fun annotateExternally(
    listOwner: PsiModifierListOwner,
    annotationFQName: String,
    fromFile: PsiFile,
    value: Array<out PsiNameValuePair>?,
  ) = delegate.annotateExternally(listOwner, annotationFQName, fromFile, value)

  override fun annotateExternallyModCommand(
    listOwner: PsiModifierListOwner,
    annotationFQName: String,
    value: Array<out PsiNameValuePair?>?,
  ): ModCommand = delegate.annotateExternallyModCommand(listOwner, annotationFQName, value)

  override fun annotateExternallyModCommand(
    listOwner: PsiModifierListOwner,
    annotationFQName: String,
    value: Array<out PsiNameValuePair?>?,
    annotationsToRemove: List<String>,
  ): ModCommand = delegate.annotateExternallyModCommand(listOwner, annotationFQName, value, annotationsToRemove)

  override fun deannotate(listOwner: PsiModifierListOwner, annotationFQN: String): Boolean = delegate.deannotate(listOwner, annotationFQN)

  override fun elementRenamedOrMoved(element: PsiModifierListOwner, oldExternalName: String) =
    delegate.elementRenamedOrMoved(element, oldExternalName)

  override fun editExternalAnnotation(
    listOwner: PsiModifierListOwner,
    annotationFQN: String,
    value: Array<out PsiNameValuePair>?,
  ): Boolean = delegate.editExternalAnnotation(listOwner, annotationFQN, value)

  override fun chooseAnnotationsPlaceNoUi(element: PsiElement): AnnotationPlace = delegate.chooseAnnotationsPlaceNoUi(element)

  override fun chooseAnnotationsPlace(element: PsiElement): AnnotationPlace = delegate.chooseAnnotationsPlaceNoUi(element)

  override fun findExternalAnnotationsFiles(listOwner: PsiModifierListOwner): List<PsiFile>? =
    delegate.findExternalAnnotationsFiles(listOwner)

  override fun hasConfiguredAnnotationRoot(owner: PsiModifierListOwner): Boolean = delegate.hasConfiguredAnnotationRoot(owner)

  override fun dispose() {}

  override fun isUnderAnnotationRoot(file: VirtualFile): Boolean = delegate.isUnderAnnotationRoot(file)

  override fun deannotateModCommand(listOwner: List<PsiModifierListOwner>, annotationFQNs: List<String>): ModCommand =
    delegate.deannotateModCommand(listOwner, annotationFQNs)

  override fun editExternalAnnotationModCommand(
    listOwner: PsiModifierListOwner,
    annotationFQN: String,
    value: Array<out PsiNameValuePair>?,
  ): ModCommand = delegate.editExternalAnnotationModCommand(listOwner, annotationFQN, value)
}

private class DummyExternalAnnotationsManager(psiManager: PsiManager) : ModCommandAwareExternalAnnotationsManager(psiManager) {
  override fun hasAnnotationRootsForFile(file: VirtualFile): Boolean = false

  override fun isExternalAnnotation(annotation: PsiAnnotation): Boolean = false

  override fun findExternalAnnotation(listOwner: PsiModifierListOwner, annotationFQN: String): PsiAnnotation? = null

  override fun findExternalAnnotations(listOwner: PsiModifierListOwner, annotationFQN: String): List<PsiAnnotation> = emptyList()

  override fun isExternalAnnotationWritable(listOwner: PsiModifierListOwner, annotationFQN: String): Boolean = false

  override fun findExternalAnnotations(listOwner: PsiModifierListOwner): Array<out PsiAnnotation> = emptyArray()

  override fun findDefaultConstructorExternalAnnotations(aClass: PsiClass): List<PsiAnnotation>? = null

  override fun findDefaultConstructorExternalAnnotations(aClass: PsiClass, annotationFQN: String): List<PsiAnnotation>? = null

  override fun annotateExternally(
    listOwner: PsiModifierListOwner,
    annotationFQName: String,
    fromFile: PsiFile,
    value: Array<out PsiNameValuePair>?,
  ) = Unit

  override fun annotateExternallyModCommand(
    listOwner: PsiModifierListOwner,
    annotationFQName: String,
    value: Array<out PsiNameValuePair?>?,
  ): ModCommand = ModNothing()

  override fun annotateExternallyModCommand(
    listOwner: PsiModifierListOwner,
    annotationFQName: String,
    value: Array<out PsiNameValuePair?>?,
    annotationsToRemove: List<String>,
  ): ModCommand = ModNothing()

  override fun deannotate(listOwner: PsiModifierListOwner, annotationFQN: String): Boolean = false

  override fun editExternalAnnotation(
    listOwner: PsiModifierListOwner,
    annotationFQN: String,
    value: Array<out PsiNameValuePair>?,
  ): Boolean = false

  override fun chooseAnnotationsPlaceNoUi(element: PsiElement): AnnotationPlace = AnnotationPlace.IN_CODE

  override fun chooseAnnotationsPlace(element: PsiElement): AnnotationPlace = AnnotationPlace.IN_CODE

  override fun findExternalAnnotationsFiles(listOwner: PsiModifierListOwner): List<PsiFile>? = null

  override fun hasConfiguredAnnotationRoot(owner: PsiModifierListOwner): Boolean = false

  override fun isUnderAnnotationRoot(file: VirtualFile): Boolean = false

  override fun deannotateModCommand(listOwner: List<PsiModifierListOwner>, annotationFQNs: List<String>): ModCommand = ModNothing()

  override fun editExternalAnnotationModCommand(
    listOwner: PsiModifierListOwner,
    annotationFQN: String,
    value: Array<out PsiNameValuePair>?,
  ): ModCommand = ModNothing()
}
