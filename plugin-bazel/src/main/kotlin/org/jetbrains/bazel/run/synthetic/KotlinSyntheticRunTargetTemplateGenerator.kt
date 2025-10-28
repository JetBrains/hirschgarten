package org.jetbrains.bazel.run.synthetic

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import org.intellij.lang.annotations.Language
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.idea.base.psi.KotlinPsiHeuristics
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class KotlinSyntheticRunTargetTemplateGenerator : MainClassSyntheticRunTargetTemplateGenerator() {
  override fun getMainClass(element: PsiElement): String? {
    return tryGetKotlinMainClassInClass(element)
      ?: tryGetKotlinMainClassInFile(element)
  }

  override fun getBuildContent(
    target: BuildTarget,
    syntheticTarget: String,
    mainClass: String,
  ): String {
    @Language("starlark") val build = """
      load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_binary")

      kt_jvm_binary(
        name = "$syntheticTarget",
        main_class = "${mainClass}",
        runtime_deps = ["${target.id}"],
      )
    """.trimIndent()
    return build
  }

  override val id: String
    get() = "kotlin"

  override fun isSupported(target: BuildTarget): Boolean = target.data is KotlinBuildTarget

  fun tryGetKotlinMainClassInClass(element: PsiElement): String? {
    val parent = element.getStrictParentOfType<PsiNameIdentifierOwner>() ?: return null
    val ktClass = parent.getNonStrictParentOfType<KtClassOrObject>() ?: return null
    return KotlinPsiHeuristics.getJvmName(ktClass)
  }

  fun tryGetKotlinMainClassInFile(element: PsiElement): String? {
    val parent = element.getStrictParentOfType<PsiNameIdentifierOwner>() ?: return null
    val ktFile = parent.getNonStrictParentOfType<KtFile>() ?: return null
    return ktFile.javaFileFacadeFqName.asString()
  }
}
