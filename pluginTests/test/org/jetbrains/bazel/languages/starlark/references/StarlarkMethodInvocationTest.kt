package org.jetbrains.bazel.languages.starlark.references

import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.TempDirTestFixture
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.instanceOf
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.kotlin.idea.base.psi.getLineStartOffset
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkMethodInvocationTest : BasePlatformTestCase() {
  override fun createTempDirTestFixture(): TempDirTestFixture? = IdeaTestFixtureFactory.getFixtureFactory().createTempDirTestFixture()

  @Before
  fun beforeEach() {
    project.isBazelProject = true
    project.rootDir = myFixture.tempDirFixture.getFile(".")!!
  }

  // https://youtrack.jetbrains.com/issue/BAZEL-3039
  @Test
  fun `local function invocation`() {
    myFixture.verifyTargetOfReferenceAtCaret(
      """
        def <target>_compiler_target_actual(actual):
           return "@rules_kotlin_maven//:%s" % actual

        def kt_define_compiler_targets():
           for name, actual in _COMPILER_TARGETS:
               native.alias(
                   name = name,
                   actual = <caret>_compiler_target_actual(actual),
               )
      """.trimIndent(),
    )
  }
}
