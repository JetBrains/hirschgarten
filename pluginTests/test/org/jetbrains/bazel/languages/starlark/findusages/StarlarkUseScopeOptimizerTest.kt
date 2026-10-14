package org.jetbrains.bazel.languages.starlark.findusages

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.common.timeoutRunBlocking
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration
import org.jetbrains.bazel.project.BazelProjectFixtures
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.Test

@BazelTestApplication
class StarlarkUseScopeOptimizerTest {
  private val myFixture by bazelCodeInsightFixture()

  private val project get() = myFixture.project

  @Test
  fun `should narrow scope for top-level public function to defining and loading files`() {
    myFixture.addFileToProject("MODULE.bazel", "")
    myFixture.addFileToProject("BUILD", "")
    val defsFile = myFixture.addFileToProject(
      "defs.bzl",
      """
      def helper(x):
          return x + 1
      """.trimIndent(),
    )
    val consumerFile = myFixture.addFileToProject(
      "consumer.bzl",
      """
      load("//:defs.bzl", "helper")
      helper(42)
      """.trimIndent(),
    )
    val unrelatedFile = myFixture.addFileToProject(
      "unrelated.bzl",
      """
      def unrelated():
          pass
      """.trimIndent(),
    )
    openFileInEditor(defsFile.virtualFile)

    val useScope = runReadActionBlocking {
      val helperFn = PsiTreeUtil
        .collectElementsOfType(myFixture.file, StarlarkFunctionDeclaration::class.java)
        .single { it.name == "helper" }
      PsiSearchHelper.getInstance(project).getUseScope(helperFn)
    }
    useScope.shouldBeInstanceOf<GlobalSearchScope>()
    runReadActionBlocking { useScope.contains(defsFile.virtualFile) } shouldBe true
    runReadActionBlocking { useScope.contains(consumerFile.virtualFile) } shouldBe true
    runReadActionBlocking { useScope.contains(unrelatedFile.virtualFile) } shouldBe false
  }

  @Test
  fun `should narrow scope for top-level public variable to defining and loading files`() {
    myFixture.addFileToProject("MODULE.bazel", "")
    myFixture.addFileToProject("BUILD", "")
    val defsFile = myFixture.addFileToProject(
      "defs.bzl",
      """
      MY_CONST = 42
      """.trimIndent(),
    )
    val consumerFile = myFixture.addFileToProject(
      "consumer.bzl",
      """
      load("//:defs.bzl", "MY_CONST")
      x = MY_CONST + 1
      """.trimIndent(),
    )
    val unrelatedFile = myFixture.addFileToProject(
      "unrelated.bzl",
      """
      OTHER = 99
      """.trimIndent(),
    )
    openFileInEditor(defsFile.virtualFile)

    val useScope = runReadActionBlocking {
      val myConst = PsiTreeUtil.collectElementsOfType(myFixture.file, StarlarkTargetExpression::class.java)
        .single { it.name == "MY_CONST" }
      PsiSearchHelper.getInstance(project).getUseScope(myConst)
    }
    useScope.shouldBeInstanceOf<GlobalSearchScope>()
    runReadActionBlocking { useScope.contains(defsFile.virtualFile) } shouldBe true
    runReadActionBlocking { useScope.contains(consumerFile.virtualFile) } shouldBe true
    runReadActionBlocking { useScope.contains(unrelatedFile.virtualFile) } shouldBe false
  }

  @Test
  fun `should private functions scope should remain local`() {
    val defsFile = myFixture.addFileToProject(
      "defs.bzl",
      """
      def _private_helper(x):
          return x + 1
      """.trimIndent(),
    )
    openFileInEditor(defsFile.virtualFile)

    val useScope = runReadActionBlocking {
      val fn = PsiTreeUtil
        .collectElementsOfType(myFixture.file, StarlarkFunctionDeclaration::class.java)
        .single { it.name == "_private_helper" }
      PsiSearchHelper.getInstance(project).getUseScope(fn)
    }
    useScope.shouldBeInstanceOf<LocalSearchScope>()
  }

  @Test
  fun `should not narrow scope for top-level private variable`() {
    val defsFile = myFixture.addFileToProject(
      "defs.bzl",
      """
      _PRIVATE_CONST = 42
      """.trimIndent(),
    )
    openFileInEditor(defsFile.virtualFile)

    val useScope = runReadActionBlocking {
      val privateConst = PsiTreeUtil
        .collectElementsOfType(myFixture.file, StarlarkTargetExpression::class.java)
        .single { it.name == "_PRIVATE_CONST" }
      PsiSearchHelper.getInstance(project).getUseScope(privateConst)
    }
    useScope.shouldBeInstanceOf<LocalSearchScope>()
  }

  @Test
  fun `should not narrow scope for local variable inside function`() {
    val defsFile = myFixture.addFileToProject(
      "defs.bzl",
      """
      def helper():
          local_var = 42
          return local_var
      """.trimIndent(),
    )
    openFileInEditor(defsFile.virtualFile)

    val useScope = runReadActionBlocking {
      val localVar = PsiTreeUtil
        .collectElementsOfType(myFixture.file, StarlarkTargetExpression::class.java)
        .single { it.name == "local_var" }
      PsiSearchHelper.getInstance(project).getUseScope(localVar)
    }
    useScope.shouldBeInstanceOf<LocalSearchScope>()
  }

  @Test
  fun `should not narrow scope for function parameter`() {
    val defsFile = myFixture.addFileToProject(
      "defs.bzl",
      """
      def helper(param):
          return param
      """.trimIndent(),
    )
    openFileInEditor(defsFile.virtualFile)

    val useScope = runReadActionBlocking {
      val param = PsiTreeUtil
        .collectElementsOfType(myFixture.file, StarlarkNamedElement::class.java)
        .single { it.name == "param" }
      PsiSearchHelper.getInstance(project).getUseScope(param)
    }
    useScope.shouldBeInstanceOf<LocalSearchScope>()
  }

  private fun openFileInEditor(file: VirtualFile): Unit = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      myFixture.openFileInEditor(file)
    }
  }
}
