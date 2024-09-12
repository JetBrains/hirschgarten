package org.jetbrains.bazel.languages.starlark.findusages

import io.kotest.matchers.collections.shouldHaveSize
import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkFindUsagesTestCase
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkFunctionFindUsagesTest : StarlarkFindUsagesTestCase() {
  @Test
  fun `should find local usage`() {
    // given
    myFixture.configureByFile("FunctionDeclaration.bzl")
    val function =
      myFixture.file.children
        .filterIsInstance<StarlarkFunctionDeclaration>()
        .first()
    // when
    val usages = myFixture.findUsages(function)
    // then
    usages shouldHaveSize 1
  }
}
