package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.collections.shouldContainAll
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BazelNativeRulesCompletionContributorTest : BasePlatformTestCase() {
  @Test
  fun `should complete cc native rule`() {
    myFixture.configureByText("BUILD", "")
    myFixture.type("cc")

    val ccLookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    ccLookups shouldContainAll
      listOf(
        "cc_binary",
        "cc_import",
        "cc_library",
        "cc_test",
        "cc_toolchain",
        "cc_shared_library",
        "cc_static_library",
        "cc_proto_library",
      )

    myFixture.configureByText("BUILD", "")
    myFixture.type("fd")
    val fdoLookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    fdoLookups shouldContainAll
      listOf(
        "fdo_profile",
        "fdo_prefetch_hints",
      )

    myFixture.configureByText("BUILD", "")
    myFixture.type("pro")
    val propLookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    propLookups shouldContainAll
      listOf(
        "propeller_optimize",
      )
  }

  @Test
  fun `should complete java native rule`() {
    myFixture.configureByText("BUILD", "")
    myFixture.type("java")

    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainAll
      listOf(
        "java_binary",
        "java_library",
      )
  }

  @Test
  fun `should complete objective-c native rule`() {
    myFixture.configureByText("BUILD", "")
    myFixture.type("obj")

    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainAll
      listOf(
        "objc_library",
        "objc_import",
      )
  }

  @Test
  fun `should complete protobuf native rule`() {
    myFixture.configureByText("BUILD", "")
    myFixture.type("proto")

    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainAll
      listOf(
        "cc_proto_library",
        "java_lite_proto_library",
        "java_proto_library",
        "proto_library",
        "py_proto_library",
        "proto_lang_toolchain",
        "proto_toolchain",
      )
  }

  @Test
  fun `should complete python native rule`() {
    myFixture.configureByText("BUILD", "")
    myFixture.type("py")

    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainAll
      listOf(
        "py_binary",
        "py_library",
        "py_test",
        "py_runtime",
      )
  }

  @Test
  fun `should complete shell native rule`() {
    myFixture.configureByText("BUILD", "")
    myFixture.type("sh")

    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    lookups shouldContainAll
      listOf(
        "sh_binary",
        "sh_library",
        "sh_test",
      )
  }

  @Test
  fun `should autocomplete brackets `() {
    myFixture.configureByText("BUILD", "")
    myFixture.type("java_bin")

    val lookups = myFixture.completeBasic()

    assertNull(lookups)
    val expected = "java_binary(<caret>)"

    myFixture.checkResult(expected)
  }
}
