package org.jetbrains.bazel.python.resolve

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.externalSystem.autoimport.AutoImportProjectTracker
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.QualifiedName
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.resolve.PyQualifiedNameResolveContextImpl
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.test.framework.BazelBasePlatformTestCase


class BazelPyImportResolverTest : BazelBasePlatformTestCase() {

  override fun getProjectDescriptor(): LightProjectDescriptor? {
    return DefaultLightProjectDescriptor()
  }

  override fun setUp() {
    super.setUp()
    prepareFiles()
  }

  override fun tearDown() {
    PluginManagerCore.loadedPlugins
    Disposer.dispose(AutoImportProjectTracker.getInstance(project))
    super.tearDown()
  }

  fun testShouldFindTopLevelRootImportOfDirectory() {
    val result = tryResolve("dir1")
    result.shouldNotBeNull()
    result.shouldBeInstanceOf<PsiDirectory>()
    result.virtualFile.name shouldBe "dir1"
  }

  fun testShouldFindTopLevelRootImportOfFile() {
    val result = tryResolve("file1")
    result.shouldNotBeNull()
    result.shouldBeInstanceOf<PyFile>()
    result.virtualFile.name shouldBe "file1.py"
  }

  fun testShouldFindComplexRootImportOfDirectory() {
    val result = tryResolve("dir2.dir21.dir211")
    result.shouldNotBeNull()
    result.shouldBeInstanceOf<PsiDirectory>()
    result.virtualFile.name shouldBe "dir211"
  }

  fun testShouldFindComplexRootImportOfFile() {
    val result = tryResolve("dir2.dir21.dir211.file2111")
    result.shouldNotBeNull()
    result.shouldBeInstanceOf<PyFile>()
    result.virtualFile.name shouldBe "file2111.py"
  }

  fun testShouldReturnNullWhenImportIsInvalid() {
    val result = tryResolve("abc.def.ghi")
    result.shouldBeNull()
  }

  fun testShouldIgnoreNonBazelProject() {
    project.isBazelProject = false
    val result = tryResolve("file1")
    result.shouldBeNull()
  }

  fun testShouldPrioritiseDirectoriesOverFiles() {
    val result = tryResolve("dir1.conflict")
    result.shouldNotBeNull()
    result.shouldBeInstanceOf<PsiDirectory>()
    result.virtualFile.name shouldBe "conflict"
  }

  fun testShouldIgnoreDirectoriesThatLookLikePythonFiles() {
    val result = tryResolve("dir1.directory")
    result.shouldBeNull()
  }

  fun testShouldIgnoreFilesThatHaveNoExtension() {
    val result = tryResolve("dir1.file")
    result.shouldBeNull()
  }

  fun testShouldIgnoreFilesThatHaveNonPythonExtension() {
    val result = tryResolve("file2")
    result.shouldBeNull()
  }

  fun testShouldAcceptSynonym() {
    val synonymProvider = MockSynonymProvider()
    PythonSynonymProvider.ep.registerExtension(synonymProvider)
    val result = tryResolve("synonym.dir2.dir21.dir211.file2111")
    result.shouldNotBeNull()
    result.shouldBeInstanceOf<PyFile>()
    result.virtualFile.name shouldBe "file2111.py"
    synonymProvider.wasGetSynonymCalled shouldBe true
  }

  fun testShouldNotCauseErrorIfSynonymProviderReturnsNull() {
    val synonymProvider = MockSynonymProvider()
    PythonSynonymProvider.ep.registerExtension(synonymProvider)
    val result = tryResolve("null")
    result.shouldBeNull()
    synonymProvider.wasGetSynonymCalled shouldBe true
  }

  fun testShouldNotCallSynonymProviderIfNotNecessary() {
    val synonymProvider = MockSynonymProvider()
    PythonSynonymProvider.ep.registerExtension(synonymProvider)
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
      val root = LightPlatformTestCase.getSourceRoot()

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
    return BazelPyImportResolver().resolveImportReference(nameObject, context, false)
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
