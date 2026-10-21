package org.jetbrains.bazel.python.resolve

import com.intellij.bazel.python.backend.BazelPyImportResolver
import com.intellij.bazel.python.backend.PythonSynonymProvider
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.QualifiedName
import com.intellij.testFramework.junit5.TestDisposable
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.resolve.PyQualifiedNameResolveContextImpl
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.project.BazelProjectFixtures.deinitializeBazelProject
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@BazelTestApplication
class BazelPyImportResolverTest {
  private val myFixture by bazelCodeInsightFixture()

  private val project get() = myFixture.project

  @TestDisposable
  lateinit var disposable: Disposable

  @BeforeEach
  fun setUp() {
    prepareFiles()
  }

  @Test
  fun testShouldFindTopLevelRootImportOfDirectory() {
    val result = tryResolve("dir1")
    result.shouldNotBeNull()
    result.shouldBeInstanceOf<PsiDirectory>()
    result.virtualFile.name shouldBe "dir1"
  }

  @Test
  fun testShouldFindTopLevelRootImportOfFile() {
    val result = tryResolve("file1")
    result.shouldNotBeNull()
    result.shouldBeInstanceOf<PyFile>()
    result.virtualFile.name shouldBe "file1.py"
  }

  @Test
  fun testShouldFindComplexRootImportOfDirectory() {
    val result = tryResolve("dir2.dir21.dir211")
    result.shouldNotBeNull()
    result.shouldBeInstanceOf<PsiDirectory>()
    result.virtualFile.name shouldBe "dir211"
  }

  @Test
  fun testShouldFindComplexRootImportOfFile() {
    val result = tryResolve("dir2.dir21.dir211.file2111")
    result.shouldNotBeNull()
    result.shouldBeInstanceOf<PyFile>()
    result.virtualFile.name shouldBe "file2111.py"
  }

  @Test
  fun testShouldReturnNullWhenImportIsInvalid() {
    val result = tryResolve("abc.def.ghi")
    result.shouldBeNull()
  }

  @Test
  fun testShouldIgnoreNonBazelProject() {
    deinitializeBazelProject(project)
    val result = tryResolve("file1")
    result.shouldBeNull()
  }

  @Test
  fun testShouldPrioritiseDirectoriesOverFiles() {
    val result = tryResolve("dir1.conflict")
    result.shouldNotBeNull()
    result.shouldBeInstanceOf<PsiDirectory>()
    result.virtualFile.name shouldBe "conflict"
  }

  @Test
  fun testShouldIgnoreDirectoriesThatLookLikePythonFiles() {
    val result = tryResolve("dir1.directory")
    result.shouldBeNull()
  }

  @Test
  fun testShouldIgnoreFilesThatHaveNoExtension() {
    val result = tryResolve("dir1.file")
    result.shouldBeNull()
  }

  @Test
  fun testShouldIgnoreFilesThatHaveNonPythonExtension() {
    val result = tryResolve("file2")
    result.shouldBeNull()
  }

  @Test
  fun testShouldAcceptSynonym() {
    val synonymProvider = MockSynonymProvider()
    PythonSynonymProvider.ep.point.registerExtension(synonymProvider, disposable)
    val result = tryResolve("synonym.dir2.dir21.dir211.file2111")
    result.shouldNotBeNull()
    result.shouldBeInstanceOf<PyFile>()
    result.virtualFile.name shouldBe "file2111.py"
    synonymProvider.wasGetSynonymCalled shouldBe true
  }

  @Test
  fun testShouldNotCauseErrorIfSynonymProviderReturnsNull() {
    val synonymProvider = MockSynonymProvider()
    PythonSynonymProvider.ep.point.registerExtension(synonymProvider, disposable)
    val result = tryResolve("null")
    result.shouldBeNull()
    synonymProvider.wasGetSynonymCalled shouldBe true
  }

  @Test
  fun testShouldNotCallSynonymProviderIfNotNecessary() {
    val synonymProvider = MockSynonymProvider()
    PythonSynonymProvider.ep.point.registerExtension(synonymProvider, disposable)
    val result = tryResolve("file1")
    result.shouldNotBeNull()
    synonymProvider.wasGetSynonymCalled shouldBe false
  }

  /**
   * ```
   * root
   *  |- file1.py
   *  |- file2.java
   *  |- dir1
   *  |  |- conflict
   *  |  |  |- __init__.py
   *  |  |- directory.py (this is a directory, not a file)
   *  |  |- conflict.py
   *  |  |- file
   *  |- dir2
   *  |  |- dir21
   *  |  |  |- dir211
   *  |  |  |  |- file2111.py
   * ```
   */
  private fun prepareFiles() {
    WriteCommandAction.runWriteCommandAction(project) {
      val root = project.rootDir

      root.createChildData(this, "file1.py")
      root.createChildData(this, "file2.java")

      val dir1 = root.createChildDirectory(this, "dir1")
      dir1.createChildDirectory(this, "conflict").also { it.createChildData(this, "__init__.py") }
      dir1.createChildDirectory(this, "directory.py")
      dir1.createChildData(this, "conflict.py")
      dir1.createChildData(this, "file")

      val dir2 = root.createChildDirectory(this, "dir2")
      val dir21 = dir2.createChildDirectory(this, "dir21")
      val dir211 = dir21.createChildDirectory(this, "dir211")
      dir211.createChildData(this, "file2111.py")
    }
  }

  private fun tryResolve(qualifiedName: String): PsiElement? {
    val nameObject = QualifiedName.fromComponents(qualifiedName.split('.'))
    val context = PyQualifiedNameResolveContextImpl(PsiManager.getInstance(project), null, null, null)
    return runReadActionBlocking { BazelPyImportResolver().resolveImportReference(nameObject, context, false) }
  }
}

class MockSynonymProvider : PythonSynonymProvider {
  var wasGetSynonymCalled: Boolean = false

  override fun getSynonym(qualifiedNameComponents: List<String>): List<String>? {
    wasGetSynonymCalled = true
    return if (qualifiedNameComponents.firstOrNull() == "synonym") {
      qualifiedNameComponents.drop(1)
    } else {
      null
    }
  }
}
