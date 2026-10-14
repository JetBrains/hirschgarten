package org.jetbrains.bazel.languages.starlark.references

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.psi.PsiClass
import com.intellij.testFramework.ExtensionTestUtil
import com.intellij.testFramework.junit5.TestDisposable
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.instanceOf
import org.jetbrains.bazel.languages.starlark.bazel.StarlarkClassParametersProvider
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@BazelTestApplication
class StarlarkClassnameReferenceTest {
  private val myFixture by bazelCodeInsightFixture()

  @TestDisposable
  lateinit var disposable: Disposable

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

    val resolved = runReadActionBlocking {
      val reference = myFixture.file.findReferenceAt(myFixture.caretOffset)
      reference!!.resolve()
    }

    // then
    resolved.shouldNotBeNull()
    resolved shouldBe instanceOf<PsiClass>()
    runReadActionBlocking { (resolved as? PsiClass)?.name } shouldBe "KotlinClass"
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

    val resolved = runReadActionBlocking {
      val reference = myFixture.file.findReferenceAt(myFixture.caretOffset)
      reference!!.resolve()
    }

    // then
    resolved.shouldNotBeNull()
    resolved shouldBe instanceOf<PsiClass>()
    runReadActionBlocking { (resolved as? PsiClass)?.name } shouldBe "JavaClass"
  }

  @Test
  fun `test should resolve reference for custom registered parameter name`() {
    // given
    val provider =
      object : StarlarkClassParametersProvider {
        override fun getClassnameParameters(): List<String> = listOf("name_of_the_class")
      }
    ExtensionTestUtil.maskExtensions(StarlarkClassParametersProvider.EP_NAME, listOf(provider), disposable)

    myFixture.addFileToProject(
      "com/example/MyClass.java",
      """
      package com.example;

      public class MyClass {}
      """.trimIndent(),
    )

    val file = myFixture.configureByText(
      "BUILD.bazel",
      """
      custom_rule(
          name = "my_rule",
          name_of_the_class = "com.example.My<caret>Class"
      )
      """.trimIndent(),
    )

    // when
    val element = runReadActionBlocking { myFixture.getReferenceAtCaretPosition()?.resolve() }

    // then
    assertNotNull(element)
    assertTrue(element is PsiClass)
    assertEquals("MyClass", runReadActionBlocking { (element as PsiClass).name })
  }
}
