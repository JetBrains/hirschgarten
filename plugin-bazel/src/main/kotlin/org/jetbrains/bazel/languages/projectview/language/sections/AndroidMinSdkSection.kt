package org.jetbrains.bazel.languages.projectview.language.sections

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.projectview.ProjectViewBundle
import org.jetbrains.bazel.languages.projectview.language.ScalarSection
import org.jetbrains.bazel.languages.projectview.language.SectionKey

class AndroidMinSdkSection : ScalarSection<Int>() {
  override val name = NAME
  override val sectionKey = KEY
  override val doc = "Override the minimum Android SDK version globally for the whole project."

  override fun fromRawValue(rawValue: String): Int? = rawValue.toIntOrNull()

  override fun annotateValue(element: PsiElement, holder: AnnotationHolder) {
    if (fromRawValue(element.text) == null) {
      val message = ProjectViewBundle.getMessage("annotator.cannot.parse.value", element.text)
      holder.annotateError(element, message)
    }
  }

  companion object {
    const val NAME = "android_min_sdk"
    val KEY = SectionKey<Int>(NAME)
  }
}
