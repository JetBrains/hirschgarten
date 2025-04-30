package org.jetbrains.bazel.python.resolve

import com.intellij.openapi.command.WriteCommandAction
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BazelPyImportResolverTest : MockProjectBaseTest() {
  @BeforeEach
  override fun beforeEach() {
    super.beforeEach()
    project.isBazelProject = true
    prepareFiles()
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

  /**
   * ```
   * root
   *  |- file1.py
   *  |- dir1
   *  |  |- file11.py
   *  |  |- file12.py
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

      val dir1 = root.createChildDirectory(this, "dir1")
      dir1.createChildData(this, "file11.py")
      dir1.createChildData(this, "file12.py")

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
