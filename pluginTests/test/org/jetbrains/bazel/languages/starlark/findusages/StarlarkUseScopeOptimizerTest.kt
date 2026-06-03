package org.jetbrains.bazel.languages.starlark.findusages

import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration
import org.jetbrains.bazel.project.BazelProjectFixtures
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkUseScopeOptimizerTest : BasePlatformTestCase() {

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
    myFixture.openFileInEditor(defsFile.virtualFile)

    val helperFn = PsiTreeUtil
      .collectElementsOfType(myFixture.file, StarlarkFunctionDeclaration::class.java)
      .single { it.name == "helper" }
    val useScope = PsiSearchHelper.getInstance(project).getUseScope(helperFn)
    useScope.shouldBeInstanceOf<GlobalSearchScope>()
    useScope.contains(defsFile.virtualFile) shouldBe true
    useScope.contains(consumerFile.virtualFile) shouldBe true
    useScope.contains(unrelatedFile.virtualFile) shouldBe false
  }

  @Test
  fun `should narrow scope for top-level public variable to defining and loading files`() {
    BazelProjectFixtures.initializeBazelProject(project, myFixture.tempDirPath)
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
    myFixture.openFileInEditor(defsFile.virtualFile)

    val myConst = PsiTreeUtil.collectElementsOfType(myFixture.file, StarlarkTargetExpression::class.java)
      .single { it.name == "MY_CONST" }
    val useScope = PsiSearchHelper.getInstance(project).getUseScope(myConst)
    useScope.shouldBeInstanceOf<GlobalSearchScope>()
    useScope.contains(defsFile.virtualFile) shouldBe true
    useScope.contains(consumerFile.virtualFile) shouldBe true
    useScope.contains(unrelatedFile.virtualFile) shouldBe false
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
    myFixture.openFileInEditor(defsFile.virtualFile)

    val fn = PsiTreeUtil
      .collectElementsOfType(myFixture.file, StarlarkFunctionDeclaration::class.java)
      .single { it.name == "_private_helper" }
    val useScope = PsiSearchHelper.getInstance(project).getUseScope(fn)
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
    myFixture.openFileInEditor(defsFile.virtualFile)

    val privateConst = PsiTreeUtil
      .collectElementsOfType(myFixture.file, StarlarkTargetExpression::class.java)
      .single { it.name == "_PRIVATE_CONST" }
    val useScope = PsiSearchHelper.getInstance(project).getUseScope(privateConst)
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
    myFixture.openFileInEditor(defsFile.virtualFile)

    val localVar = PsiTreeUtil
      .collectElementsOfType(myFixture.file, StarlarkTargetExpression::class.java)
      .single { it.name == "local_var" }
    val useScope = PsiSearchHelper.getInstance(project).getUseScope(localVar)
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
    myFixture.openFileInEditor(defsFile.virtualFile)

    val param = PsiTreeUtil
      .collectElementsOfType(myFixture.file, StarlarkNamedElement::class.java)
      .single { it.name == "param" }
    val useScope = PsiSearchHelper.getInstance(project).getUseScope(param)
    useScope.shouldBeInstanceOf<LocalSearchScope>()
  }
}
