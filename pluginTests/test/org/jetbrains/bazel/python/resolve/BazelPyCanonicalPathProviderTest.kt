package org.jetbrains.bazel.python.resolve

import com.intellij.bazel.python.backend.BazelPyCanonicalPathProvider
import com.intellij.bazel.python.backend.updateBazelPythonResolveIndex
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.QualifiedName
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.resolve.PyCanonicalPathProvider
import io.kotest.matchers.collections.shouldExist
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.jetbrains.bazel.test.framework.testBazel
import org.jetbrains.bazel.test.framework.testproject.uninstall
import org.jetbrains.bazel.test.framework.testproject.writeFile
import org.junit.jupiter.api.Test
import java.nio.file.Path

@BazelTestApplication
class BazelPyCanonicalPathProviderTest {
  private val myFixture by bazelCodeInsightFixture()

  private val project get() = myFixture.project

  private lateinit var moduleFile: VirtualFile
  private lateinit var modulePath: Path

  /** Creates `src/pkg/mymod.py` under the project root with a single top-level class and returns that class. */
  private fun createModuleWithClass(): PyClass {
    moduleFile = myFixture.testBazel.writeFile("src/pkg/mymod.py", "class MyClass:\n    pass\n")
    modulePath = moduleFile.toNioPath()
    return runReadActionBlocking {
      val pyFile = PsiManager.getInstance(project).findFile(moduleFile) as PyFile
      pyFile.topLevelClasses.first()
    }
  }

  @Test
  fun testShouldBeRegisteredAsExtension() {
    PyCanonicalPathProvider.EP_NAME.extensionList shouldExist { it is BazelPyCanonicalPathProvider }
  }

  @Test
  fun testShouldReturnBazelQualifiedNameForIndexedFile() {
    val myClass = createModuleWithClass()
    // The Bazel import path (`pkg.mymod`) intentionally differs from the source-root layout (`src.pkg.mymod`).
    project.updateBazelPythonResolveIndex(mapOf(QualifiedName.fromDottedString("pkg.mymod") to modulePath))

    val result = runReadActionBlocking {
      BazelPyCanonicalPathProvider()
        .getCanonicalPath(myClass, QualifiedName.fromDottedString("src.pkg.mymod"), myClass.containingFile)
    }

    result shouldBe QualifiedName.fromDottedString("pkg.mymod")
  }

  @Test
  fun `test should return pkg qualified name for symbol in init file`() {
    val initFile = myFixture.testBazel.writeFile("src/pkg/__init__.py", "class MyClass:\n    pass\n")
    val myClass = runReadActionBlocking {
      (PsiManager.getInstance(project).findFile(initFile) as PyFile).topLevelClasses.first()
    }
    // index maps a package to its directory, not to it `__init__.py` file.
    project.updateBazelPythonResolveIndex(mapOf(QualifiedName.fromDottedString("pkg") to initFile.parent.toNioPath()))

    val result = runReadActionBlocking {
      BazelPyCanonicalPathProvider()
        .getCanonicalPath(myClass, QualifiedName.fromDottedString("src.pkg"), myClass.containingFile)
    }

    result shouldBe QualifiedName.fromDottedString("pkg")
  }

  @Test
  fun testShouldDeferForFileNotInIndex() {
    val myClass = createModuleWithClass()
    project.updateBazelPythonResolveIndex(emptyMap())

    val result = runReadActionBlocking {
      BazelPyCanonicalPathProvider()
        .getCanonicalPath(myClass, QualifiedName.fromDottedString("src.pkg.mymod"), myClass.containingFile)
    }

    result.shouldBeNull()
  }

  @Test
  fun testShouldDeferForNonBazelProject() {
    val myClass = createModuleWithClass()
    project.updateBazelPythonResolveIndex(mapOf(QualifiedName.fromDottedString("pkg.mymod") to modulePath))
    myFixture.testBazel.uninstall()

    val result = runReadActionBlocking {
      BazelPyCanonicalPathProvider()
        .getCanonicalPath(myClass, QualifiedName.fromDottedString("src.pkg.mymod"), myClass.containingFile)
    }

    result.shouldBeNull()
  }
}
