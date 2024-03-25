package org.jetbrains.bazel.languages.starlark.rename

import com.intellij.lang.ASTNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes

class StarlarkElementGenerator(val project: Project) {

  private fun createDummyFile(contents: String): PsiFile {
    val factory = PsiFileFactory.getInstance(project)
    val virtualFile = LightVirtualFile(DUMMY_FILENAME, StarlarkFileType, contents)
    val psiFile = (factory as PsiFileFactoryImpl).trySetupPsiForFile(virtualFile, StarlarkLanguage, false, true)!!
    return psiFile
  }

  fun createNameIdentifier(name: String): ASTNode {
    val dummyFile = createDummyFile(name)
    val referenceNode = dummyFile.node.firstChildNode.firstChildNode
    val nameNode = referenceNode.firstChildNode
    if (nameNode.elementType !== StarlarkTokenTypes.IDENTIFIER) {
      error("Expected elementType to be IDENTIFIER while creating dummy file")

    }
    return nameNode
  }

  companion object {
    private const val DUMMY_FILENAME = "dummy.bzl"
  }
}
