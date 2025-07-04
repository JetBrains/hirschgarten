package org.jetbrains.bazel.python.resolve

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.externalSystem.autoimport.AutoImportProjectTracker
import com.intellij.openapi.util.Disposer
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
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BazelPyImportResolverTest : MockProjectBaseTest() {
  @BeforeEach
  override fun beforeEach() {
    super.beforeEach()
    project.isBazelProject = true
    prepareFiles()
  }

  /** Dispose of the auto‑import tracker created for this test project. */
  @AfterEach
  fun tearDownTracker() {
    Disposer.dispose(AutoImportProjectTracker.getInstance(project))
  }

  @Test
  fun `should find top-level root import of a directory`() {
    val result = tryResolve("dir1")
    result.shouldNotBeNull()
    result.shouldBeInstanceOf<PsiDirectory>()
    result.virtualFile.name shouldBe "dir1"
  }

  @Test
  fun `should find top-level root import of a file`() {
    val result = tryResolve("file1")
    result.shouldNotBeNull()
    result.shouldBeInstanceOf<PyFile>()
    result.virtualFile.name shouldBe "file1.py"
  }

  @Test
  fun `should find complex root import of a directory`() {
    val result = tryResolve("dir2.dir21.dir211")
    result.shouldNotBeNull()
    result.shouldBeInstanceOf<PsiDirectory>()
    result.virtualFile.name shouldBe "dir211"
  }

  @Test
  fun `should find complex root import of a file`() {
    val result = tryResolve("dir2.dir21.dir211.file2111")
    result.shouldNotBeNull()
    result.shouldBeInstanceOf<PyFile>()
    result.virtualFile.name shouldBe "file2111.py"
  }

  @Test
  fun `should return null when import is invalid`() {
    val result = tryResolve("abc.def.ghi")
    result.shouldBeNull()
  }

  @Test
  fun `should ignore non-Bazel project`() {
    project.isBazelProject = false
    val result = tryResolve("file1")
    result.shouldBeNull()
  }

  @Test
  fun `should prioritise directories over files`() {
    val result = tryResolve("dir1.conflict")
    result.shouldNotBeNull()
    result.shouldBeInstanceOf<PsiDirectory>()
    result.virtualFile.name shouldBe "conflict"
  }

  @Test
  fun `should ignore directories that look like Python files`() {
    val result = tryResolve("dir1.directory")
    result.shouldBeNull()
  }

  @Test
  fun `should ignore files that have no extension`() {
    val result = tryResolve("dir1.file")
    result.shouldBeNull()
  }

  @Test
  fun `should ignore files that have non-Python extension`() {
    val result = tryResolve("file2")
    result.shouldBeNull()
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
    return BazelPyImportResolver().resolveImportReference(nameObject, context, false)
  }
}
