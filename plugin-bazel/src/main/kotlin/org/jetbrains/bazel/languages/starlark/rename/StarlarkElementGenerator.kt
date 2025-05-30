package org.jetbrains.bazel.languages.starlark.rename

import com.intellij.lang.ASTNode
import com.intellij.lang.tree.util.children
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes

private const val DUMMY_FILENAME = "dummy.bzl"

class StarlarkElementGenerator(val project: Project) {
  fun createTokenType(tokenString: String): ASTNode = createDummyFile(tokenString).node.firstChildNode

  fun createNameIdentifier(name: String): ASTNode {
    val dummyFile = createDummyFile(name)
    val referenceNode = dummyFile.node.firstChildNode.firstChildNode
    val nameNode = referenceNode.firstChildNode
    if (nameNode.elementType !== StarlarkTokenTypes.IDENTIFIER) {
      error("Expected elementType to be IDENTIFIER while creating dummy file")
    }
    return nameNode
  }

  fun createStringLiteral(contents: String): ASTNode {
    val dummyFile = createDummyFile("\"$contents\"")
    val stringNode = dummyFile.node.firstChildNode.firstChildNode.firstChildNode
    if (stringNode.elementType !== StarlarkTokenTypes.STRING) {
      error("Expected elementType to be STRING while creating dummy file")
    }
    return stringNode
  }

  fun createStringLiteralExpression(contents: String): ASTNode {
    val dummyFile = createDummyFile("\"$contents\"")
    val stringNode = dummyFile.node.firstChildNode.firstChildNode
    if (stringNode.elementType !== StarlarkElementTypes.STRING_LITERAL_EXPRESSION) {
      error("Expected elementType to be STRING_LITERAL_EXPRESSION")
    }
    return stringNode
  }

  /**
   * You want to be passing contents sort of in the form
   *     `deps = [],`
   */
  fun createNamedArgument(contents: String): ASTNode {
    val dummyFile = createDummyFile("dummy($contents)")
    val argumentNode =
      dummyFile.node.firstChildNode.firstChildNode.lastChildNode
        .children()
        .elementAt(1)
    if (argumentNode.elementType !== StarlarkElementTypes.NAMED_ARGUMENT_EXPRESSION) {
      error("Expected elementType to be NAMED_ARGUMENT_EXPRESSION")
    }
    return argumentNode
  }

  private fun createDummyFile(contents: String): PsiFile {
    val factory = PsiFileFactory.getInstance(project)
    val virtualFile = LightVirtualFile(DUMMY_FILENAME, StarlarkFileType, contents)
    val psiFile = (factory as PsiFileFactoryImpl).trySetupPsiForFile(virtualFile, StarlarkLanguage, false, true)
    return psiFile ?: error("Failed to create dummy file")
  }
}
