package org.jetbrains.bazel.kotlin.coverage

import com.intellij.psi.PsiElement
import org.jetbrains.bazel.java.coverage.FullyQualifiedNameProvider
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

internal class KotlinFullyQualifiedNameProvider : FullyQualifiedNameProvider {
  override fun getFullyQualifiedName(psiElement: PsiElement): String? {
    val classOrObject = psiElement.getNonStrictParentOfType<KtClassOrObject>() ?: return null
    return classOrObject.fqName?.asString()
  }
}
