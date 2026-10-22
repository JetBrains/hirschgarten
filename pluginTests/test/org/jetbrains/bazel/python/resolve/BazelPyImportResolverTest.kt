package org.jetbrains.bazel.python.resolve

import com.intellij.bazel.python.backend.BazelPyImportResolver
import com.intellij.bazel.python.backend.PythonSynonymProvider
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.QualifiedName
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.resolve.PyQualifiedNameResolveContextImpl
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.jetbrains.bazel.test.framework.testBazel
import org.jetbrains.bazel.test.framework.testproject.createDirectory
import org.jetbrains.bazel.test.framework.testproject.uninstall
import org.jetbrains.bazel.test.framework.testproject.writeFile
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@BazelTestApplication
class BazelPyImportResolverTest {
  private val myFixture by bazelCodeInsightFixture()
  private val testBazel get() = myFixture.testBazel

  private val project get() = myFixture.project

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
    testBazel.uninstall()
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
    PythonSynonymProvider.ep.point.registerExtension(synonymProvider, testBazel.disposable)
    val result = tryResolve("synonym.dir2.dir21.dir211.file2111")
    result.shouldNotBeNull()
    result.shouldBeInstanceOf<PyFile>()
    result.virtualFile.name shouldBe "file2111.py"
    synonymProvider.wasGetSynonymCalled shouldBe true
  }

  @Test
  fun testShouldNotCauseErrorIfSynonymProviderReturnsNull() {
    val synonymProvider = MockSynonymProvider()
    PythonSynonymProvider.ep.point.registerExtension(synonymProvider, testBazel.disposable)
    val result = tryResolve("null")
    result.shouldBeNull()
    synonymProvider.wasGetSynonymCalled shouldBe true
  }

  @Test
  fun testShouldNotCallSynonymProviderIfNotNecessary() {
    val synonymProvider = MockSynonymProvider()
    PythonSynonymProvider.ep.point.registerExtension(synonymProvider, testBazel.disposable)
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
    testBazel.writeFile("file1.py")
    testBazel.writeFile("file2.java")

    testBazel.writeFile("dir1/conflict/__init__.py")
    testBazel.createDirectory("dir1/directory.py")
    testBazel.writeFile("dir1/conflict.py")
    testBazel.writeFile("dir1/file")

    testBazel.writeFile("dir2/dir21/dir211/file2111.py")
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
