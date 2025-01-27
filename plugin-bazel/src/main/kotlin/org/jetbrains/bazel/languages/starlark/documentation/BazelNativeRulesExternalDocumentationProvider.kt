package org.jetbrains.bazel.languages.starlark.documentation

import com.intellij.codeInsight.navigation.targetPresentation
import com.intellij.model.Pointer
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.createSmartPointer
import org.jetbrains.bazel.languages.bazelrc.BazelrcLanguage
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage

class BazelNativeRulesDocumentationTarget(val element: PsiElement, private val originalElement: PsiElement?) : DocumentationTarget {
  override fun createPointer(): Pointer<out DocumentationTarget> {
    val elementPtr = element.createSmartPointer()
    val originalElementPtr = originalElement?.createSmartPointer()
    return Pointer {
      val element = elementPtr.dereference() ?: return@Pointer null
      BazelNativeRulesDocumentationTarget(element, originalElementPtr?.dereference())
    }
  }

  override fun computePresentation(): TargetPresentation {
    return targetPresentation(element)
  }

  override fun computeDocumentation(): DocumentationResult? {
    val html = "TEST"
    return DocumentationResult.documentation(html)
  }
}

class BazelNativeRulesExternalDocumentationProvider : PsiDocumentationTargetProvider {
  override fun documentationTarget(element: PsiElement, originalElement: PsiElement?): DocumentationTarget? {
    return if (element.language.`is`(StarlarkLanguage)) {
      BazelNativeRulesDocumentationTarget(element, originalElement)
    } else {
      null
    }
  }
}
