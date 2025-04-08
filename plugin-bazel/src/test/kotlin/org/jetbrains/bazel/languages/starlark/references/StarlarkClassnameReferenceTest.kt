package org.jetbrains.bazel.languages.starlark.references

import com.intellij.psi.PsiClass
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.instanceOf
import org.jetbrains.bazel.config.isBazelProject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkClassnameReferenceTest : BasePlatformTestCase() {
  @Before
  fun beforeEach() {
    project.isBazelProject = true
  }

  @Test
  fun `should resolve kotlin class`() {
    // given
    myFixture.addFileToProject(
      "Kotlin.kt",
      """
      package org.app.test
      class KotlinClass {
        fun test() : String? = null
      }
      """.trimIndent(),
    )

    // when
    myFixture.configureByText(
      "BUILD",
      """
      load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
      kt_jvm_library(
        name = "testTarget",
        classname = "<caret>org.app.test.KotlinClass",
      )
      """.trimMargin(),
    )

    val reference = myFixture.file.findReferenceAt(myFixture.caretOffset)
    val resolved = reference!!.resolve()

    // then
    resolved.shouldNotBeNull()
    resolved shouldBe instanceOf<PsiClass>()
    (resolved as? PsiClass)?.name shouldBe "KotlinClass"
  }

  @Test
  fun `should resolve java class`() {
    // given
    myFixture.addFileToProject(
      "Java.java",
      """
      package org.app.test;
      public class JavaClass {
          public String greet() {
              return "Hello, test!";
          }
      }
      """.trimIndent(),
    )

    // when
    myFixture.configureByText(
      "BUILD",
      """
      java_binary(
        name = "testTarget",
        classname = "<caret>org.app.test.JavaClass",
      )
      """.trimMargin(),
    )

    val reference = myFixture.file.findReferenceAt(myFixture.caretOffset)
    val resolved = reference!!.resolve()

    // then
    resolved.shouldNotBeNull()
    resolved shouldBe instanceOf<PsiClass>()
    (resolved as? PsiClass)?.name shouldBe "JavaClass"
  }
}
