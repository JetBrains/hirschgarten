package org.jetbrains.bazel.python.resolve

import com.intellij.bazel.python.backend.BazelPyCanonicalPathProvider
import com.intellij.bazel.python.backend.updateBazelPythonResolveIndex
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.QualifiedName
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor
import com.intellij.testFramework.fixtures.TempDirTestFixture
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.resolve.PyCanonicalPathProvider
import io.kotest.matchers.collections.shouldExist
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.project.BazelProjectFixtures.deinitializeBazelProject
import org.jetbrains.bazel.test.framework.BazelBasePlatformTestCase
import java.nio.file.Path

class BazelPyCanonicalPathProviderTest : BazelBasePlatformTestCase() {

  override fun getProjectDescriptor(): LightProjectDescriptor = DefaultLightProjectDescriptor()

  override fun createTempDirTestFixture(): TempDirTestFixture = TempDirTestFixtureImpl()

  private lateinit var moduleFile: VirtualFile
  private lateinit var modulePath: Path

  /** Creates `src/pkg/mymod.py` under the project root with a single top-level class and returns that class. */
  private fun createModuleWithClass(): PyClass {
    WriteCommandAction.runWriteCommandAction(project) {
      val pkgDir = project.rootDir
        .createChildDirectory(this, "src")
        .createChildDirectory(this, "pkg")
      moduleFile = pkgDir.createChildData(this, "mymod.py")
      VfsUtil.saveText(moduleFile, "class MyClass:\n    pass\n")
    }
    modulePath = moduleFile.toNioPath()
    val pyFile = PsiManager.getInstance(project).findFile(moduleFile) as PyFile
    return pyFile.topLevelClasses.first()
  }

  fun testShouldBeRegisteredAsExtension() {
    PyCanonicalPathProvider.EP_NAME.extensionList shouldExist { it is BazelPyCanonicalPathProvider }
  }

  fun testShouldReturnBazelQualifiedNameForIndexedFile() {
    val myClass = createModuleWithClass()
    // The Bazel import path (`pkg.mymod`) intentionally differs from the source-root layout (`src.pkg.mymod`).
    project.updateBazelPythonResolveIndex(mapOf(QualifiedName.fromDottedString("pkg.mymod") to modulePath))

    val result = BazelPyCanonicalPathProvider()
      .getCanonicalPath(myClass, QualifiedName.fromDottedString("src.pkg.mymod"), myClass.containingFile)

    result shouldBe QualifiedName.fromDottedString("pkg.mymod")
  }

  fun testShouldDeferForFileNotInIndex() {
    val myClass = createModuleWithClass()
    project.updateBazelPythonResolveIndex(emptyMap())

    val result = BazelPyCanonicalPathProvider()
      .getCanonicalPath(myClass, QualifiedName.fromDottedString("src.pkg.mymod"), myClass.containingFile)

    result.shouldBeNull()
  }

  fun testShouldDeferForNonBazelProject() {
    val myClass = createModuleWithClass()
    project.updateBazelPythonResolveIndex(mapOf(QualifiedName.fromDottedString("pkg.mymod") to modulePath))
    deinitializeBazelProject(project)

    val result = BazelPyCanonicalPathProvider()
      .getCanonicalPath(myClass, QualifiedName.fromDottedString("src.pkg.mymod"), myClass.containingFile)

    result.shouldBeNull()
  }
}
