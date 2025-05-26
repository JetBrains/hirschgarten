package org.jetbrains.bazel.languages.starlark.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression

object StarlarkElementFactory {
  fun createStringLiteral(project: Project, content: String): StarlarkStringLiteralExpression {
    val quoted = "\"$content\""
    val fileContent =
      """
      kt_jvm_library(
          name = $quoted
      )
      """.trimIndent()

    val file =
      PsiFileFactory
        .getInstance(project)
        .createFileFromText("BUILD", StarlarkLanguage, fileContent)

    return PsiTreeUtil.findChildOfType(file, StarlarkStringLiteralExpression::class.java)
      ?: error("Could not find string literal in generated file")
  }
}
