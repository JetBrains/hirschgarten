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
    // given
    myFixture.configureByText("BUILD", "")
    myFixture.type("cc")

    // when
    val ccLookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
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

    // when
    myFixture.configureByText("BUILD", "")
    myFixture.type("fd")
    val fdoLookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    fdoLookups shouldContainAll
      listOf(
        "fdo_profile",
        "fdo_prefetch_hints",
      )

    // when
    myFixture.configureByText("BUILD", "")
    myFixture.type("pro")
    val propLookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    propLookups shouldContainAll
      listOf(
        "propeller_optimize",
      )
  }

  @Test
  fun `should complete java native rule`() {
    // given
    myFixture.configureByText("BUILD", "")
    myFixture.type("java")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainAll
      listOf(
        "java_binary",
        "java_library",
      )
  }

  @Test
  fun `should complete objective-c native rule`() {
    // given
    myFixture.configureByText("BUILD", "")
    myFixture.type("obj")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainAll
      listOf(
        "objc_library",
        "objc_import",
      )
  }

  @Test
  fun `should complete protobuf native rule`() {
    // given
    myFixture.configureByText("BUILD", "")
    myFixture.type("proto")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
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
    // given
    myFixture.configureByText("BUILD", "")
    myFixture.type("py")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
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
    // given
    myFixture.configureByText("BUILD", "")
    myFixture.type("sh")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainAll
      listOf(
        "sh_binary",
        "sh_library",
        "sh_test",
      )
  }

  @Test
  fun `should autocomplete brackets `() {
    // given
    myFixture.configureByText("BUILD", "")
    myFixture.type("java_bin")

    // when
    val lookups = myFixture.completeBasic()

    // then
    assertNull(lookups)
    val expected = "java_binary(<caret>)"

    myFixture.checkResult(expected)
  }
}
