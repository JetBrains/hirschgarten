package org.jetbrains.bazel.java.ui.gutters

import com.intellij.execution.junit.DisabledConditionUtil
import com.intellij.lang.jvm.util.JvmClassUtil
import com.intellij.lang.jvm.util.JvmMainMethodUtil
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.run.test.createTestFilterDescriptor
import org.jetbrains.bazel.run.test.useJetBrainsTestRunner
import org.jetbrains.bazel.ui.gutters.BazelRunLineMarkerContributor

@ApiStatus.Internal  // External plugins (e.g., Scala) should implement BazelRunLineMarkerContributor instead
open class BazelJavaRunLineMarkerContributor : BazelRunLineMarkerContributor() {
  override fun isDumbAware(): Boolean = true

  override fun getGutterAction(element: PsiElement): GutterAction? {
    if (element.containingFile.virtualFile?.fileSystem is JarFileSystem) return null
    val psiIdentifier = PsiTreeUtil.getParentOfType(element, PsiNameIdentifierOwner::class.java, true) ?: return null
    if (psiIdentifier.nameIdentifier != element) return null
    val (psiClass, psiMethod) = psiIdentifier.toPsiClassOrMethod()
    val classOrMethod = psiClass ?: psiMethod ?: return null
    if (element.isMainMethod()) {
      return GutterAction()
    }

    val project = element.project
    val className = psiIdentifier.getContainingClassFqn() ?: return null
    val testFilter = if (psiMethod != null) {
      val methodName = psiMethod.name
      if (element.project.useJetBrainsTestRunner()) {
        val methodParameterTypes = psiMethod.getMethodParameterTypes()
        "$className:$methodName:$methodParameterTypes"
      } else {
        "$className.$methodName$"
      }
    }
    else {
      className
    }

    var runnerActionDescriptor = createTestFilterDescriptor(project, testFilter)
    // Support running a @Disabled JUnit test if we clicked on it explicitely
    val junitDisabledCondition = DisabledConditionUtil.getDisabledCondition(classOrMethod)
    if (junitDisabledCondition != null) {
      val jvmFlag = "--wrapper_script_flag=--jvm_flag=-Djunit.jupiter.conditions.deactivate=$junitDisabledCondition"
      runnerActionDescriptor = runnerActionDescriptor.copy(
        programArguments = runnerActionDescriptor.programArguments + listOf(jvmFlag),
      )
    }
    return GutterAction(runnerActionDescriptor = runnerActionDescriptor)
  }

  /**
   * See [JUnit docs](https://docs.junit.org/5.2.0/api/org/junit/platform/engine/discovery/MethodSelector.html#getMethodParameterTypes())
   */
  private fun PsiMethod.getMethodParameterTypes(): String =
    this.parameterList.parameters.map { it.type }.mapNotNull { type ->
      if (type is PsiClassType) {
        // canonicalText will include type arguments if they are present, avoid that in simple cases
        type.resolve()?.qualifiedName
      } else {
        type.canonicalText
      }
    }.joinToString(separator = ",")

  @ApiStatus.Internal
  protected open fun PsiElement.getContainingClassFqn(): String? {
    val psiClass = PsiTreeUtil.getParentOfType(this, PsiClass::class.java, false) ?: return null
    return JvmClassUtil.getJvmClassName(psiClass)
  }

  protected open fun PsiNameIdentifierOwner.toPsiClassOrMethod(): Pair<PsiClass?, PsiMethod?> =
    (this as? PsiClass) to (this as? PsiMethod)

  @ApiStatus.Internal
  protected open fun PsiElement.isMainMethod(): Boolean =
    this is PsiMethod && JvmMainMethodUtil.isMainMethod(this) ||
    this is PsiClass && JvmMainMethodUtil.hasMainMethodInHierarchy(this)
}
