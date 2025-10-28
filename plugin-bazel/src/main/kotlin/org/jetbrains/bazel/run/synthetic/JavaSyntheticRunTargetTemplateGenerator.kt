package org.jetbrains.bazel.run.synthetic

import com.intellij.lang.jvm.util.JvmClassUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import org.intellij.lang.annotations.Language
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import org.jetbrains.bsp.protocol.ScalaBuildTarget
import org.jetbrains.bsp.protocol.utils.extractJvmBuildTarget
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.idea.base.psi.KotlinPsiHeuristics
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class JavaSyntheticRunTargetTemplateGenerator : MainClassSyntheticRunTargetTemplateGenerator() {
  override val id: String = "jvm"

  override fun isSupported(target: BuildTarget): Boolean {
    if (target.data is KotlinBuildTarget || target.data is ScalaBuildTarget) {
      return false
    }
    return extractJvmBuildTarget(target) != null
  }

  override fun getMainClass(element: PsiElement): String? {
    val psiClass = element.getNonStrictParentOfType<PsiClass>() ?: return null
    return JvmClassUtil.getJvmClassName(psiClass)
  }

  override fun getBuildContent(target: BuildTarget, syntheticTarget: String, mainClass: String): String {
    @Language("starlark") val build = """
      load("@rules_java//java:defs.bzl", "java_binary")

      java_binary(
        name = "$syntheticTarget",
        main_class = "${mainClass}",
        runtime_deps = ["${target.id}"],
      )
    """.trimIndent()
    return build
  }

}
