package org.jetbrains.bazel.languages.bazelrc.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.bazel.languages.bazelrc.BazelrcFileType
import org.jetbrains.bazel.languages.bazelrc.BazelrcLanguage

private const val DUMMY_FILENAME = "dummy.bazelrc"

class BazelrcElementGenerator(val project: Project) {
  private fun createDummyFile(contents: String): BazelrcFile {
    val factory = PsiFileFactory.getInstance(project)
    val virtualFile = LightVirtualFile(DUMMY_FILENAME, BazelrcFileType, contents)

    return (factory as PsiFileFactoryImpl).trySetupPsiForFile(virtualFile, BazelrcLanguage, false, true) as BazelrcFile
  }

  fun createFlagName(flagName: String): PsiElement = createDummyFile("common --$flagName").lines[0].flags[0].name!!
}
