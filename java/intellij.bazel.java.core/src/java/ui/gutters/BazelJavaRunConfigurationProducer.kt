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
import org.jetbrains.bazel.run.test.createTestFilterAction
import org.jetbrains.bazel.run.test.useJetBrainsTestRunner
import org.jetbrains.bazel.ui.gutters.BazelRunConfigurationProducer
import org.jetbrains.bsp.protocol.BuildTarget

@ApiStatus.Internal  // External plugins (e.g., Scala) should extend BazelRunConfigurationProducer instead
open class BazelJavaRunConfigurationProducer : BazelRunConfigurationProducer() {
  override fun isDumbAware(): Boolean = true

  override fun getGutterAction(element: PsiElement, target: BuildTarget): GutterAction? {
    if (element.containingFile?.virtualFile?.fileSystem is JarFileSystem) return null
    val psiIdentifier = PsiTreeUtil.getParentOfType(element, PsiNameIdentifierOwner::class.java, true) ?: return null
    if (psiIdentifier.nameIdentifier != element) return null
    val (psiClass, psiMethod) = toPsiClassOrMethod(psiIdentifier)
    val classOrMethod = psiClass ?: psiMethod ?: return null
    if (isMainMethod(element)) {
      return GutterAction()
    }

    val project = element.project
    val className = getContainingClassFqn(psiIdentifier) ?: return null
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

    var gutterAction = createTestFilterAction(project, testFilter)
    // Support running a @Disabled JUnit test if we clicked on it explicitely
    val junitDisabledCondition = DisabledConditionUtil.getDisabledCondition(classOrMethod)
    if (junitDisabledCondition != null) {
      val jvmFlag = "--wrapper_script_flag=--jvm_flag=-Djunit.jupiter.conditions.deactivate=$junitDisabledCondition"
      gutterAction = gutterAction.copy(
        programArguments = gutterAction.programArguments + listOf(jvmFlag),
      )
    }
    return gutterAction.copy(additionalLocationString = psiMethod?.name)
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

  open fun getContainingClassFqn(element: PsiElement): String? {
    val psiClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java, false) ?: return null
    return JvmClassUtil.getJvmClassName(psiClass)
  }

  protected open fun toPsiClassOrMethod(element: PsiNameIdentifierOwner): Pair<PsiClass?, PsiMethod?> =
    (element as? PsiClass) to (element as? PsiMethod)

  open fun isMainMethod(element: PsiElement): Boolean {
    val identifier = PsiTreeUtil.getParentOfType(element, PsiNameIdentifierOwner::class.java, true)
    return identifier is PsiMethod && JvmMainMethodUtil.isMainMethod(identifier) || identifier is PsiClass && JvmMainMethodUtil.hasMainMethodInHierarchy(
      identifier,
    )
  }
}
