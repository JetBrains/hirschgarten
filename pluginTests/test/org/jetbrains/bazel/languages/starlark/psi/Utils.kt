package org.jetbrains.bazel.languages.starlark.psi

import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType

fun CodeInsightTestFixture.requireCallWithNameAttribute(name: String): StarlarkCallExpression {
  return findCallWithNameAttribute(name) ?: error("Call with name '$name' not present!")
}

fun CodeInsightTestFixture.findCallWithNameAttribute(targetName: String): StarlarkCallExpression? {
  val starlarkFile = file as? StarlarkFile ?: return null
  return starlarkFile.findDescendantOfType<StarlarkCallExpression> { it.getNameAttributeValue() == targetName }
}
