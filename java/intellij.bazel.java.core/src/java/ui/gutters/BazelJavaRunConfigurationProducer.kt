package org.jetbrains.bazel.java.ui.gutters

import com.intellij.execution.junit.DisabledConditionUtil
import com.intellij.lang.jvm.util.JvmClassUtil
import com.intellij.lang.jvm.util.JvmMainMethodUtil
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.annotations.ApiStatus
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

    val className = getContainingClassFqn(psiIdentifier) ?: return null
    val testFilter = JvmTestFilterExtension.getInstance(element.project, target).getTestFilter(className, psiMethod)
    val junitDisabledCondition = DisabledConditionUtil.getDisabledCondition(classOrMethod)
    return GutterAction(
      testFilter = testFilter,
      programArguments = when {
        // Support running a @Disabled JUnit test if we clicked on it explicitely
        junitDisabledCondition != null -> listOf("--wrapper_script_flag=--jvm_flag=-Djunit.jupiter.conditions.deactivate=$junitDisabledCondition")
        else -> emptyList()
      },
      additionalLocationString = psiMethod?.name,
    )
  }

  open fun getContainingClassFqn(element: PsiElement): String? {
    val psiClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java, false) ?: return null
    return JvmClassUtil.getJvmClassName(psiClass)
  }

  open fun toPsiClassOrMethod(element: PsiNameIdentifierOwner): Pair<PsiClass?, PsiMethod?> =
    (element as? PsiClass) to (element as? PsiMethod)

  open fun isMainMethod(element: PsiElement): Boolean {
    val identifier = PsiTreeUtil.getParentOfType(element, PsiNameIdentifierOwner::class.java, true)
    return identifier is PsiMethod && JvmMainMethodUtil.isMainMethod(identifier) || identifier is PsiClass && JvmMainMethodUtil.hasMainMethodInHierarchy(
      identifier,
    )
  }
}

@ApiStatus.Internal
fun getTestFilter(className: String, methodName: String?): String =
  if (methodName != null) {
    // Include
    "${className.normalizeNestedClassSeparator()}.$methodName$"
  }
  else {
    className.normalizeNestedClassSeparator()
  }

/**
 * Any `$` separating a nested class is replaced with `.`, because `$` would otherwise be interpreted as a regex end-of-input anchor.
 */
private fun String.normalizeNestedClassSeparator(): String = replace("$", ".")
